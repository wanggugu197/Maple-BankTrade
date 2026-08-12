package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.AbstractTradeEntryStorage;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 机器多资源交易条目存储器。
 * <p>
 * 自动交易查找使用懒构建索引（{@link ItemResource}/{@link FluidResource} → 条目），
 * 避免每次线性扫全表；索引在首次查询或注册变更后重建（此时 components 已绑定）。
 * </p>
 */
public final class MachineTradeStorage extends AbstractTradeEntryStorage<MachineTrade> {

    private final boolean allowAutoTrade;

    /** 单物品输入 auto 条目索引；惰性构建。 */
    private Map<ItemResource, Map.Entry<Identifier, MachineTrade>> itemAutoIndex = Map.of();

    /** 单流体输入 auto 条目索引；惰性构建。 */
    private Map<FluidResource, Map.Entry<Identifier, MachineTrade>> fluidAutoIndex = Map.of();

    private boolean autoIndexDirty = true;

    public MachineTradeStorage(Identifier tradeTypeId) {
        this(tradeTypeId, false);
    }

    public MachineTradeStorage(Identifier tradeTypeId, boolean allowAutoTrade) {
        super(tradeTypeId);
        this.allowAutoTrade = allowAutoTrade;
    }

    public boolean allowAutoTrade() {
        return allowAutoTrade;
    }

    @Override
    protected boolean isValidEntry(MachineTrade entry) {
        if (entry == null || !entry.isValid()) {
            return false;
        }
        return !entry.autoTrade() || allowAutoTrade;
    }

    @Override
    public MachineTrade register(MachineTrade entry) {
        MachineTrade result = super.register(entry);
        // 新 auto 条目或重复注册均标记索引失效（惰性重建）
        if (entry != null && entry.autoTrade()) {
            autoIndexDirty = true;
        }
        return result;
    }

    /** 返回当前上下文下可见的条目（保持注册顺序）。 */
    public List<Map.Entry<Identifier, MachineTrade>> listVisible(MachineTradeContext context) {
        if (context == null) {
            return List.of();
        }
        List<Map.Entry<Identifier, MachineTrade>> visible = new ArrayList<>();
        for (Map.Entry<Identifier, MachineTrade> entry : entries().entrySet()) {
            MachineTrade trade = entry.getValue();
            if (trade.visibilityHook().isVisible(context, trade)) {
                visible.add(entry);
            }
        }
        return visible;
    }

    /**
     * 按物品资源查找自动交易条目（O(1) 索引 + visibility）。
     */
    @Nullable
    public Map.Entry<Identifier, MachineTrade> findAutoTradeByItem(
                                                                   ItemResource resource,
                                                                   @Nullable MachineTradeContext context) {
        if (!allowAutoTrade || resource == null || resource.isEmpty()) {
            return null;
        }
        ensureAutoIndex();
        Map.Entry<Identifier, MachineTrade> match = itemAutoIndex.get(resource);
        if (match == null) {
            return null;
        }
        if (context != null) {
            if (!match.getValue().visibilityHook().isVisible(context, match.getValue())) {
                return null;
            }
        }
        return match;
    }

    /**
     * 按流体资源查找自动交易条目（O(1) 索引 + visibility）。
     */
    @Nullable
    public Map.Entry<Identifier, MachineTrade> findAutoTradeByFluid(
                                                                    FluidResource resource,
                                                                    @Nullable MachineTradeContext context) {
        if (!allowAutoTrade || resource == null || resource.isEmpty()) {
            return null;
        }
        ensureAutoIndex();
        Map.Entry<Identifier, MachineTrade> match = fluidAutoIndex.get(resource);
        if (match == null) {
            return null;
        }
        if (context != null) {
            if (!match.getValue().visibilityHook().isVisible(context, match.getValue())) {
                return null;
            }
        }
        return match;
    }

    /**
     * 惰性构建 auto 索引。须在 Item/Fluid components 绑定后调用（游戏运行期查询时）。
     * <p>
     * <b>禁止</b>同一 storage 内两条 auto 条目使用相同物品/流体输入；冲突时打 error 并
     * {@link IllegalStateException} 崩溃，避免静默吞配方。
     * </p>
     */
    private void ensureAutoIndex() {
        if (!autoIndexDirty) {
            return;
        }
        Map<ItemResource, Map.Entry<Identifier, MachineTrade>> items = new HashMap<>();
        Map<FluidResource, Map.Entry<Identifier, MachineTrade>> fluids = new HashMap<>();

        if (allowAutoTrade) {
            for (Map.Entry<Identifier, MachineTrade> entry : entries().entrySet()) {
                MachineTrade trade = entry.getValue();
                if (!trade.autoTrade()) {
                    continue;
                }
                if (trade.itemInputs().size() == 1 && trade.fluidInputs().isEmpty()) {
                    ItemIO io = trade.itemInputs().getFirst();
                    if (!io.isResolvable()) {
                        continue;
                    }
                    ItemResource resource = ItemResource.of(io.toStack());
                    Map.Entry<Identifier, MachineTrade> prev = items.putIfAbsent(resource, entry);
                    if (prev != null) {
                        String msg = "Duplicate autoTrade item input in " + tradeTypeId() + ": " + prev.getKey() + " and " + entry.getKey() + " both match " + resource;
                        MapleBankTrade.LOGGER.error(msg);
                        throw new IllegalStateException(msg);
                    }
                } else if (trade.fluidInputs().size() == 1 && trade.itemInputs().isEmpty()) {
                    FluidIO io = trade.fluidInputs().getFirst();
                    if (!io.isResolvable()) {
                        continue;
                    }
                    FluidResource resource = FluidResource.of(io.toStack());
                    Map.Entry<Identifier, MachineTrade> prev = fluids.putIfAbsent(resource, entry);
                    if (prev != null) {
                        String msg = "Duplicate autoTrade fluid input in " + tradeTypeId() + ": " + prev.getKey() + " and " + entry.getKey() + " both match " + resource;
                        MapleBankTrade.LOGGER.error(msg);
                        throw new IllegalStateException(msg);
                    }
                }
            }
        }

        itemAutoIndex = Map.copyOf(items);
        fluidAutoIndex = Map.copyOf(fluids);
        autoIndexDirty = false;
    }

    @Override
    protected MachineTrade createEmptyEntry() {
        return new MachineTrade();
    }

    @Override
    protected void onClear() {
        autoIndexDirty = true;
        itemAutoIndex = Map.of();
        fluidAutoIndex = Map.of();
    }
}

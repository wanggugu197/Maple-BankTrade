package com.maple.maple_banktrade.api.machineTrade.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.machineTrade.ui.MachineTradeUiHost;
import com.maple.maple_banktrade.api.machineTrade.ui.TradingStationUi;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.*;
import com.mapleutillib.api.baseBlock.DirectionBlockEntity;
import com.mapleutillib.api.resource.ObservableFluidResourceHandler;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import com.mapleutillib.api.resource.filter.NoInsertFilter;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * 贸易站方块实体基类。
 * <p>
 * <b>LDLib2 Sync（声明式同步/持久化）</b>
 * <ul>
 * <li>{@link Persisted} — 自动 NBT 持久化</li>
 * <li>{@link DescSynced} — 服务端变更后同步到客户端</li>
 * <li>原地修改的 final 字段（handler / Set）须在变更后调用
 * {@link #markDirty(String)} 通知 {@link FieldManagedStorage}</li>
 * <li>C2S 动作由 UI 的 server event / {@code setOnServerClick} 触发，不在本类挂
 * {@code @RPCMethod}（与 {@link TradingStationUi} 一致）</li>
 * </ul>
 * 子类传入 {@link TradingStationStorageSpec} 与 {@code allowAutoTrade}，并实现
 * {@link #fallbackTradeTypeIds()} / {@link #fallbackTradeTypeId()}。
 * </p>
 */
public abstract class BaseTradingStationBlockEntity extends DirectionBlockEntity
                                                    implements ISyncPersistRPCBlockEntity, MachineTradeUiHost {

    /** 自动交易周期：10 秒。 */
    public static final int AUTO_TRADE_INTERVAL_TICKS = 20 * 10;

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    // ── 声明式字段：@Persisted + @DescSynced ──

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler itemInput;

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableItemResourceHandler itemOutput;

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableFluidResourceHandler fluidInput;

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final ObservableFluidResourceHandler fluidOutput;

    @Persisted
    @DescSynced
    @DropSaved
    @Getter
    private final SimpleEnergyHandler energy;

    /** 绑定银行卡 UUID（插入顺序 = 货币扣款顺序）。原地增删后 markDirty。 */
    @Persisted
    @DescSynced
    @DropSaved
    private final Set<UUID> cardUuids = new LinkedHashSet<>();

    /**
     * 玩家配置：是否开启自动交易。
     * <p>
     * 仅当 {@link #allowAutoTrade} 为 true 时生效；默认开启。UI Switch 读写本字段。
     * </p>
     */
    @Persisted
    @DescSynced
    private boolean autoTradeEnabled = true;

    // ── 运行时（不同步） ──

    @Getter
    private final ResourceHandler<ItemResource> itemCapability;

    @Getter
    private final ResourceHandler<FluidResource> fluidCapability;

    @Getter
    private final TradingStationStorageSpec storageSpec;

    /**
     * 机器是否支持自动交易（构造参数，不可配置）。
     * 实际运行还需 {@link #autoTradeEnabled} 与 storage/entry 的 autoTrade。
     */
    @Getter
    private final boolean allowAutoTrade;

    private int autoTradeTickCounter;
    /** 防止交易改库存 → onChanged → 再 auto 的重入。 */
    private boolean tradeBusy;
    /**
     * 输入变更待跑自动交易（P0 debounce）。
     * <p>
     * onChanged 只置位；由 {@link #serverTick} 在下一 tick 冲刷，避免管道连塞每槽全量扫描。
     * </p>
     */
    private boolean autoTradePending;

    protected BaseTradingStationBlockEntity(
                                            BlockEntityType<?> type,
                                            BlockPos pos,
                                            BlockState state,
                                            TradingStationStorageSpec spec,
                                            boolean allowAutoTrade) {
        super(type, pos, state);
        this.storageSpec = Objects.requireNonNull(spec, "spec");
        this.allowAutoTrade = allowAutoTrade;
        // 不支持自动交易的站强制配置为关（避免脏数据）
        if (!allowAutoTrade) {
            this.autoTradeEnabled = false;
        }

        this.itemInput = new ObservableItemResourceHandler(spec.itemInputSlots());
        this.itemOutput = new ObservableItemResourceHandler(spec.itemOutputSlots());
        this.itemInput.setDefaultStackSizeMultiplier(spec.itemStackSizeMultiplier());
        this.itemOutput.setDefaultStackSizeMultiplier(spec.itemStackSizeMultiplier());

        this.fluidInput = new ObservableFluidResourceHandler(spec.fluidInputTanks(), spec.fluidCapacityMb());
        this.fluidOutput = new ObservableFluidResourceHandler(spec.fluidOutputTanks(), spec.fluidCapacityMb());

        this.energy = new SimpleEnergyHandler(spec.energyCapacity()) {

            @Override
            protected void onEnergyChanged(int previousAmount) {
                markDirty("energy");
            }
        };

        itemOutput.setIOFilter(new NoInsertFilter<>());
        fluidOutput.setIOFilter(new NoInsertFilter<>());

        // 原地改槽位 → markDirty 字段名；仅输入侧触发自动交易
        wireItem(itemInput, "itemInput", true);
        wireItem(itemOutput, "itemOutput", false);
        wireFluid(fluidInput, "fluidInput", true);
        wireFluid(fluidOutput, "fluidOutput", false);

        // 管道：insert→输入，extract→输出
        this.itemCapability = new CombinedResourceHandler<>(itemOutput, new InsertOnly<>(itemInput));
        this.fluidCapability = new CombinedResourceHandler<>(fluidOutput, new InsertOnly<>(fluidInput));
    }

    private void wireItem(ObservableItemResourceHandler handler, String field, boolean autoTradeOnChange) {
        handler.setContext(this);
        handler.setOnChanged((_, _) -> {
            markDirty(field);
            if (autoTradeOnChange) {
                scheduleAutoTrade();
            }
        });
    }

    private void wireFluid(ObservableFluidResourceHandler handler, String field, boolean autoTradeOnChange) {
        handler.setContext(this);
        handler.setOnChanged((_, _) -> {
            markDirty(field);
            if (autoTradeOnChange) {
                scheduleAutoTrade();
            }
        });
    }

    // ── 自动交易 ──

    /**
     * 方块 ticker 入口（仅 auto 站会挂 ticker）。
     * <p>
     * 每 tick：若有 pending 则冲刷一次；每 {@link #AUTO_TRADE_INTERVAL_TICKS} 再兜底扫一次
     * （卡余额变化等不经 onChanged 的情况）。
     * </p>
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BaseTradingStationBlockEntity be) {
        be.tickAutoTrade();
    }

    private void tickAutoTrade() {
        if (!isAutoTradeActive()) {
            return;
        }

        boolean ranPending = false;
        if (autoTradePending) {
            autoTradePending = false;
            runAutoTradeIfReady();
            ranPending = true;
        }

        if (++autoTradeTickCounter >= AUTO_TRADE_INTERVAL_TICKS) {
            autoTradeTickCounter = 0;
            // 本 tick 已因 pending 跑过则跳过周期扫，避免同 tick 双倍
            if (!ranPending) {
                runAutoTradeIfReady();
            }
        }
    }

    /**
     * 输入 onChanged：仅置 pending，不立刻全量扫描。
     * <p>
     * {@code tradeBusy} 时忽略（当前 run 会扫完全部资源）；busy 结束后若仍有输入靠周期 tick 兜底。
     * </p>
     */
    private void scheduleAutoTrade() {
        if (!isAutoTradeActive() || !isServerSide() || tradeBusy) {
            return;
        }
        autoTradePending = true;
    }

    private void runAutoTradeIfReady() {
        if (!isAutoTradeActive() || tradeBusy) {
            return;
        }
        tradeBusy = true;
        try {
            autoRunTrades();
        } finally {
            tradeBusy = false;
        }
    }

    /** 机器支持且玩家配置开启时，才真正跑自动交易。 */
    public boolean isAutoTradeActive() {
        return allowAutoTrade && autoTradeEnabled;
    }

    /** 执行会改库存的交易逻辑时占位，避免 onChanged 重入。 */
    private <T> T withTradeBusy(Supplier<T> action) {
        tradeBusy = true;
        try {
            return action.get();
        } finally {
            tradeBusy = false;
        }
    }

    private boolean isServerSide() {
        Level level = getLevel();
        return level != null && !level.isClientSide();
    }

    // ── 子类默认配方类型 ──

    protected abstract List<Identifier> fallbackTradeTypeIds();

    protected abstract Identifier fallbackTradeTypeId();

    // ── 能力 ──

    public ResourceHandler<ItemResource> getItemCapability(@Nullable Direction side) {
        return itemCapability;
    }

    public ResourceHandler<FluidResource> getFluidCapability(@Nullable Direction side) {
        return fluidCapability;
    }

    public EnergyHandler getEnergyCapability(@Nullable Direction side) {
        return energy;
    }

    public static void registerCapabilities(
                                            RegisterCapabilitiesEvent event,
                                            BlockEntityType<? extends BaseTradingStationBlockEntity> type) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, type, BaseTradingStationBlockEntity::getItemCapability);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type, BaseTradingStationBlockEntity::getFluidCapability);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, BaseTradingStationBlockEntity::getEnergyCapability);
    }

    // ── 交易类型 ──

    @Override
    public List<Identifier> tradeTypeIds() {
        if (getBlockState().getBlock() instanceof MachineTradeType host) {
            List<Identifier> configured = host.machineTradeTypes();
            if (configured != null && !configured.isEmpty()) {
                return List.copyOf(configured);
            }
        }
        return fallbackTradeTypeIds();
    }

    public Identifier tradeTypeId() {
        List<Identifier> ids = tradeTypeIds();
        return ids.isEmpty() ? fallbackTradeTypeId() : ids.getFirst();
    }

    // ── 卡 UUID（原地改 Set → markDirty） ──

    /** 只读视图；修改请用 add/remove API。 */
    @Override
    public Set<UUID> getBoundCardUuids() {
        return Collections.unmodifiableSet(cardUuids);
    }

    /** Lombok 风格兼容：与 {@link #getBoundCardUuids()} 相同。 */
    public Set<UUID> getCardUuids() {
        return getBoundCardUuids();
    }

    private void notifyCardUuidsChanged() {
        markDirty("cardUuids");
    }

    public boolean removeCardUuid(@Nullable UUID cardUuid) {
        if (cardUuid == null || !cardUuids.remove(cardUuid)) {
            return false;
        }
        notifyCardUuidsChanged();
        return true;
    }

    /** @return 实际新增数量 */
    public int addCardUuids(@Nullable Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return 0;
        }
        int added = 0;
        for (UUID uuid : uuids) {
            if (uuid != null && cardUuids.add(uuid)) {
                added++;
            }
        }
        if (added > 0) {
            notifyCardUuidsChanged();
        }
        return added;
    }

    private Set<BankCard> resolveCards(@Nullable Iterable<UUID> uuids) {
        if (!(getLevel() instanceof ServerLevel serverLevel) || uuids == null) {
            return Set.of();
        }
        BankCardsWorldData data = MBTBankStates.getBankCards(serverLevel.getServer());
        LinkedHashSet<BankCard> cards = new LinkedHashSet<>();
        for (UUID uuid : uuids) {
            if (uuid == null) {
                continue;
            }
            BankCard card = data.getCard(uuid);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    // ── 交易上下文 / 执行 ──

    @Override
    public MachineTradeContext createTradeContext(Identifier tradeTypeId) {
        Level level = getLevel();
        if (level == null) return null;
        MinecraftServer server = level.getServer();
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        if (server == null || storage == null) return null;
        return new MachineTradeContext(
                this, null, level, server,
                itemInput, itemOutput, fluidInput, fluidOutput, energy,
                resolveCards(cardUuids),
                storage);
    }

    /**
     * 按 {@link #tradeTypeIds()} 遍历自动交易；进入下一类型前若输入已空则停止。
     *
     * @return 至少成功一次的条目数累计
     */
    public int autoRunTrades() {
        if (!isAutoTradeActive()) {
            return 0;
        }
        int success = 0;
        for (Identifier typeId : tradeTypeIds()) {
            if (isAutoTradeInputEmpty()) {
                break;
            }
            success += autoRunTrades(typeId);
        }
        return success;
    }

    public int autoRunTrades(Identifier tradeTypeId) {
        if (!isAutoTradeActive() || tradeTypeId == null) {
            return 0;
        }
        MachineTradeContext context = createTradeContext(tradeTypeId);
        return context == null ? 0 : MachineTradeHandler.autoRun(context);
    }

    public boolean isAutoTradeInputEmpty() {
        return isResourceHandlerEmpty(itemInput) && isResourceHandlerEmpty(fluidInput);
    }

    private static boolean isResourceHandlerEmpty(ResourceHandler<?> handler) {
        if (handler == null) {
            return true;
        }
        for (int i = 0; i < handler.size(); i++) {
            Resource resource = handler.getResource(i);
            if (!resource.isEmpty() && handler.getAmountAsInt(i) > 0) {
                return false;
            }
        }
        return true;
    }

    public TradeExecuteResult<MachineTradeDetail> runTrade(
                                                           Identifier tradeTypeId,
                                                           Identifier tradeId,
                                                           int desiredCount) {
        Objects.requireNonNull(tradeTypeId, "tradeTypeId");
        Objects.requireNonNull(tradeId, "tradeId");
        if (desiredCount <= 0) {
            return TradeExecuteResult.failure(null);
        }
        MachineTradeContext context = createTradeContext(tradeTypeId);
        if (context == null) {
            return TradeExecuteResult.failure(null);
        }
        // 占位：避免成交改输入槽时立刻连环 auto
        return withTradeBusy(() -> MachineTradeHandler.run(context, tradeId, desiredCount));
    }

    // ── UI Host ──
    @Override
    public void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount) {
        runTrade(tradeTypeId, tradeId, desiredCount);
    }

    @Override
    public boolean unbindCardFromUi(UUID cardUuid) {
        return isServerSide() && removeCardUuid(cardUuid);
    }

    @Override
    public boolean supportsAutoTrade() {
        return allowAutoTrade;
    }

    @Override
    public boolean isAutoTradeEnabled() {
        return autoTradeEnabled;
    }

    @Override
    public void setAutoTradeEnabled(boolean enabled) {
        if (!allowAutoTrade || !isServerSide()) {
            return;
        }
        if (autoTradeEnabled == enabled) {
            return;
        }
        autoTradeEnabled = enabled;
        markDirty("autoTradeEnabled");
        if (enabled) {
            // 打开后尽快扫一次输入
            scheduleAutoTrade();
        } else {
            autoTradePending = false;
        }
    }

    /** 默认库存页：物/流 I/O + 能量 + 绑定卡。子类可覆盖。 */
    protected UIElement createInventoryTab() {
        return TradingStationUi.fullIoInventoryTab(
                itemInput, itemOutput, fluidInput, fluidOutput, energy, this);
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        ModularUI ui = TradingStationUi.create(holder, this, createInventoryTab());
        refreshOutputsForUiOpen();
        return ui;
    }

    // ── 能力侧 InsertOnly ──

    /** 允许 insert，拒绝 extract（不改 UI 直连 handler 的行为）。 */
    private static final class InsertOnly<T extends Resource> extends DelegatingResourceHandler<T> {

        InsertOnly(ResourceHandler<T> delegate) {
            super(delegate);
        }

        @Override
        public int extract(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
            return 0;
        }
    }

    private void refreshOutputsForUiOpen() {
        if (!isServerSide()) return;
        markDirty("itemInput");
        markDirty("itemOutput");
        markDirty("fluidInput");
        markDirty("fluidOutput");
    }
}

package com.maple.maple_banktrade.common.block;

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
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.RPCMethod;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.machine.ui.TradingStationUi;
import com.maple.maple_banktrade.api.machine.ui.TradingStationUiHost;
import com.maple.maple_banktrade.api.trade.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.result.TradeExecuteResult;
import com.maple.maple_banktrade.trade.machine.MachineTrade;
import com.maple.maple_banktrade.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.trade.machine.MachineTradeDetail;
import com.maple.maple_banktrade.trade.machine.MachineTradeHandler;
import com.maple.maple_banktrade.trade.machine.MachineTradePlan;
import com.maple.maple_banktrade.trade.machine.MachineTradeStorage;
import com.mapleutillib.api.baseBlock.DirectionBlockEntity;
import com.mapleutillib.api.resource.ObservableFluidResourceHandler;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import com.mapleutillib.api.resource.filter.NoInsertFilter;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * 贸易站方块实体基类：物品/流体/能量/卡 UUID 存储与同步、能力、交易与默认 UI。
 * <p>
 * 子类构造时传入 {@link TradingStationStorageSpec}，并实现
 * {@link #fallbackTradeTypeIds()} / {@link #fallbackTradeTypeId()}。
 * </p>
 */
public abstract class BaseTradingStationBlockEntity extends DirectionBlockEntity
                                                    implements ISyncPersistRPCBlockEntity, TradingStationUiHost {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @Getter
    private final ObservableItemResourceHandler itemInput;

    @Persisted
    @DescSynced
    @Getter
    private final ObservableItemResourceHandler itemOutput;

    @Persisted
    @DescSynced
    @Getter
    private final ObservableFluidResourceHandler fluidInput;

    @Persisted
    @DescSynced
    @Getter
    private final ObservableFluidResourceHandler fluidOutput;

    @Persisted
    @DescSynced
    @Getter
    private final SimpleEnergyHandler energy;

    /** 绑定银行卡 UUID（插入顺序 = 货币扣款顺序）。 */
    @Persisted
    @DescSynced
    @Getter
    private final Set<UUID> cardUuids = new LinkedHashSet<>();

    /** 对外物品能力：输出可抽禁入 + 输入可入禁抽。 */
    @Getter
    private final ResourceHandler<ItemResource> itemCapability;

    @Getter
    private final ResourceHandler<FluidResource> fluidCapability;

    @Getter
    private final TradingStationStorageSpec storageSpec;

    protected BaseTradingStationBlockEntity(
                                            BlockEntityType<?> type,
                                            BlockPos pos,
                                            BlockState state,
                                            TradingStationStorageSpec spec) {
        super(type, pos, state);
        this.storageSpec = Objects.requireNonNull(spec, "spec");

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
        bindItem(itemInput, "itemInput");
        bindItem(itemOutput, "itemOutput");
        bindFluid(fluidInput, "fluidInput");
        bindFluid(fluidOutput, "fluidOutput");

        // Combined(output, insertOnly(input))：管道 insert→输入，extract→输出
        this.itemCapability = new CombinedResourceHandler<>(itemOutput, new InsertOnly<>(itemInput));
        this.fluidCapability = new CombinedResourceHandler<>(fluidOutput, new InsertOnly<>(fluidInput));
    }

    private void bindItem(ObservableItemResourceHandler handler, String field) {
        handler.setContext(this);
        handler.setOnChanged((slot, prev) -> markDirty(field));
    }

    private void bindFluid(ObservableFluidResourceHandler handler, String field) {
        handler.setContext(this);
        handler.setOnChanged((slot, prev) -> markDirty(field));
    }

    // ── 子类：默认配方类型 ──

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
        if (getBlockState().getBlock() instanceof MachineTradeTypeBlock host) {
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

    @Nullable
    public MachineTradeStorage tradeStorage() {
        return tradeStorage(tradeTypeId());
    }

    @Nullable
    public MachineTradeStorage tradeStorage(Identifier tradeTypeId) {
        if (tradeTypeId == null) return null;
        return TradeRegistry.requireStorage(tradeTypeId, MachineTradeStorage.class);
    }

    // ── 卡 UUID ──

    public void onCardUuidsChanged() {
        markDirty("cardUuids");
    }

    public boolean addCardUuid(@Nullable UUID cardUuid) {
        if (cardUuid == null || !cardUuids.add(cardUuid)) return false;
        onCardUuidsChanged();
        return true;
    }

    public boolean removeCardUuid(@Nullable UUID cardUuid) {
        if (cardUuid == null || !cardUuids.remove(cardUuid)) return false;
        onCardUuidsChanged();
        return true;
    }

    public boolean hasCardUuid(@Nullable UUID cardUuid) {
        return cardUuid != null && cardUuids.contains(cardUuid);
    }

    public void clearCardUuids() {
        if (cardUuids.isEmpty()) return;
        cardUuids.clear();
        onCardUuidsChanged();
    }

    public boolean setCardUuids(@Nullable Collection<UUID> uuids) {
        LinkedHashSet<UUID> next = new LinkedHashSet<>();
        if (uuids != null) {
            for (UUID uuid : uuids) {
                if (uuid != null) next.add(uuid);
            }
        }
        if (cardUuids.equals(next)) return false;
        cardUuids.clear();
        cardUuids.addAll(next);
        onCardUuidsChanged();
        return true;
    }

    /** @return 实际新增数量 */
    public int addCardUuids(@Nullable Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return 0;
        int added = 0;
        for (UUID uuid : uuids) {
            if (uuid != null && cardUuids.add(uuid)) {
                added++;
            }
        }
        if (added > 0) onCardUuidsChanged();
        return added;
    }

    public Set<BankCard> resolveBoundCards() {
        return resolveCards(cardUuids);
    }

    private Set<BankCard> resolveCards(@Nullable Iterable<UUID> uuids) {
        if (!(getLevel() instanceof ServerLevel serverLevel) || uuids == null) {
            return Set.of();
        }
        BankCardsWorldData data = MBTBankStates.getBankCards(serverLevel);
        LinkedHashSet<BankCard> cards = new LinkedHashSet<>();
        for (UUID uuid : uuids) {
            if (uuid == null) continue;
            BankCard card = data.getCard(uuid);
            if (card != null) cards.add(card);
        }
        return cards;
    }

    // ── 交易 ──

    @Nullable
    public MachineTradeContext createTradeContext() {
        return createTradeContext(tradeTypeId(), resolveBoundCards());
    }

    @Nullable
    public MachineTradeContext createTradeContext(Identifier tradeTypeId) {
        return createTradeContext(tradeTypeId, resolveBoundCards());
    }

    @Nullable
    public MachineTradeContext createTradeContext(
                                                  Identifier tradeTypeId,
                                                  @Nullable Set<BankCard> bankCards) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return null;
        MinecraftServer server = level.getServer();
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        if (server == null || storage == null) return null;
        return new MachineTradeContext(
                this, level, server,
                itemInput, itemOutput, fluidInput, fluidOutput, energy,
                bankCards != null ? bankCards : resolveBoundCards(),
                storage);
    }

    @Nullable
    public MachineTradeContext createTradeContextFromCardIds(
                                                             Identifier tradeTypeId,
                                                             @Nullable Iterable<UUID> uuids) {
        return createTradeContext(tradeTypeId, resolveCards(uuids));
    }

    public TradeExecuteResult<MachineTradeDetail> runTrade(Identifier tradeId, int desiredCount) {
        return runTrade(tradeTypeId(), tradeId, desiredCount, null);
    }

    public TradeExecuteResult<MachineTradeDetail> runTrade(
                                                           Identifier tradeTypeId,
                                                           Identifier tradeId,
                                                           int desiredCount) {
        return runTrade(tradeTypeId, tradeId, desiredCount, null);
    }

    public TradeExecuteResult<MachineTradeDetail> runTrade(
                                                           Identifier tradeTypeId,
                                                           Identifier tradeId,
                                                           int desiredCount,
                                                           @Nullable Set<BankCard> bankCards) {
        Objects.requireNonNull(tradeTypeId, "tradeTypeId");
        Objects.requireNonNull(tradeId, "tradeId");
        if (desiredCount <= 0) return TradeExecuteResult.failure(null);
        MachineTradeContext context = createTradeContext(tradeTypeId, bankCards);
        return context == null ? TradeExecuteResult.failure(null) : MachineTradeHandler.run(context, tradeId, desiredCount);
    }

    public TradeCheckResult<MachineTradePlan> checkTrade(Identifier tradeId, int desiredCount) {
        return checkTrade(tradeTypeId(), tradeId, desiredCount, null);
    }

    public TradeCheckResult<MachineTradePlan> checkTrade(
                                                         Identifier tradeTypeId,
                                                         Identifier tradeId,
                                                         int desiredCount,
                                                         @Nullable Set<BankCard> bankCards) {
        Objects.requireNonNull(tradeTypeId, "tradeTypeId");
        Objects.requireNonNull(tradeId, "tradeId");
        MachineTradeContext context = createTradeContext(tradeTypeId, bankCards);
        return context == null ? TradeCheckResult.of(MachineTradePlan.denied(desiredCount)) : MachineTradeHandler.check(context, tradeId, desiredCount);
    }

    public List<Map.Entry<Identifier, MachineTrade>> listVisibleTrades() {
        return listVisibleTrades(tradeTypeId(), null);
    }

    public List<Map.Entry<Identifier, MachineTrade>> listVisibleTrades(Identifier tradeTypeId) {
        return listVisibleTrades(tradeTypeId, null);
    }

    public List<Map.Entry<Identifier, MachineTrade>> listVisibleTrades(
                                                                       Identifier tradeTypeId,
                                                                       @Nullable Set<BankCard> bankCards) {
        MachineTradeContext context = createTradeContext(tradeTypeId, bankCards);
        return context == null ? List.of() : MachineTradeHandler.listVisible(context);
    }

    public List<Map.Entry<Identifier, MachineTrade>> listRegisteredTrades() {
        return listRegisteredTrades(tradeTypeId());
    }

    public List<Map.Entry<Identifier, MachineTrade>> listRegisteredTrades(Identifier tradeTypeId) {
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        return storage == null ? List.of() : List.copyOf(storage.entries().entrySet());
    }

    @RPCMethod
    public void rpcRunTrade(RPCSender sender, String tradeTypeIdString, String tradeIdString, int desiredCount) {
        if (sender.isServer()) return;
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        try {
            runTrade(
                    Identifier.parse(tradeTypeIdString),
                    Identifier.parse(tradeIdString),
                    Math.max(1, desiredCount));
        } catch (Exception ignored) {
            // invalid identifier
        }
    }

    // ── UI ──

    @Override
    public List<Map.Entry<Identifier, MachineTrade>> listTradesForUi(Identifier tradeTypeId) {
        return TradingStationUi.preferVisibleOrRegistered(
                () -> listVisibleTrades(tradeTypeId),
                () -> listRegisteredTrades(tradeTypeId));
    }

    @Override
    public void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount) {
        runTrade(tradeTypeId, tradeId, desiredCount);
    }

    /** 默认库存页：物品+流体+能量+绑定卡。子类可覆盖。 */
    protected com.lowdragmc.lowdraglib2.gui.ui.UIElement createInventoryTab() {
        return TradingStationUi.fullIoInventoryTab(
                itemInput, itemOutput, fluidInput, fluidOutput, energy, this::getCardUuids);
    }

    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        return TradingStationUi.create(holder, this, createInventoryTab());
    }

    /** 能力侧：允许 insert，拒绝 extract（不改真实库存 UI 行为）。 */
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
}

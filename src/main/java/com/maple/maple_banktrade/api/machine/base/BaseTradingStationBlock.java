package com.maple.maple_banktrade.api.machine.base;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.mapleutillib.api.baseBlock.BaseRotatedBlock;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 贸易站方块基类：交易类型列表、UI 打开、自动交易 ticker 门控。
 * <p>
 * 子类提供 {@link #createStationEntity} / {@link #stationEntityType}，以及 MapCodec。
 * {@link #runsAutoTrade} 表示本方块是否挂载自动交易 ticker；ticker 内再校验 BE
 * {@link BaseTradingStationBlockEntity#isAllowAutoTrade()}。
 * </p>
 */
public abstract class BaseTradingStationBlock extends BaseRotatedBlock
                                              implements BlockUIMenuType.BlockUI, MachineTradeTypeBlock {

    /** 本站绑定的机器交易类型（UI 标签页顺序）。 */
    public final List<Identifier> trade_type;

    /**
     * 是否运行自动交易调度（挂 server ticker）。
     * <p>
     * 通常与对应 BE 的 {@code allowAutoTrade} 构造参数一致。
     * </p>
     */
    private final boolean runsAutoTrade;

    protected BaseTradingStationBlock(
                                      Properties properties,
                                      List<Identifier> trade_type,
                                      boolean runsAutoTrade) {
        super(properties);
        this.trade_type = trade_type;
        this.runsAutoTrade = runsAutoTrade;
    }

    /** 本方块是否挂载自动交易 ticker。 */
    public boolean runsAutoTrade() {
        return runsAutoTrade;
    }

    @Override
    public List<Identifier> machineTradeTypes() {
        return trade_type == null ? List.of() : trade_type;
    }

    /** 创建本站 BlockEntity。 */
    protected abstract BaseTradingStationBlockEntity createStationEntity(BlockPos pos, BlockState state);

    /** 本站注册的 {@link BlockEntityType}（供 ticker 类型匹配）。 */
    protected abstract BlockEntityType<? extends BaseTradingStationBlockEntity> stationEntityType();

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return createStationEntity(pos, state);
    }

    /**
     * 仅服务端且 {@link #runsAutoTrade} 为 true 时返回 ticker；
     * tick 回调中再判断 BE 是否 {@link BaseTradingStationBlockEntity#isAllowAutoTrade()}。
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                                                                  @NonNull Level level,
                                                                  @NonNull BlockState state,
                                                                  @NonNull BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || !runsAutoTrade) {
            return null;
        }
        return createTickerHelper(
                blockEntityType,
                stationEntityType(),
                (lvl, pos, st, be) -> {
                    if (be instanceof BaseTradingStationBlockEntity station && station.isAllowAutoTrade()) {
                        BaseTradingStationBlockEntity.serverTick(lvl, pos, st, station);
                    }
                });
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(
                                                        @NonNull BlockState state,
                                                        Level level,
                                                        @NonNull BlockPos pos,
                                                        @NonNull Player player,
                                                        @NonNull BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        Objects.requireNonNull(holder, "holder");
        if (holder.player.level().getBlockEntity(holder.pos) instanceof BaseTradingStationBlockEntity be) {
            return be.createUI(holder);
        }
        return null;
    }
}

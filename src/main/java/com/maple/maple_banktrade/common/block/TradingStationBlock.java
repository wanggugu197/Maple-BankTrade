package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.mapleutillib.api.baseBlock.BaseRotatedBlock;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 全功能交易站方块：绑定 {@link #trade_type} 机器价目，右键打开 BE UI。
 */
public class TradingStationBlock extends BaseRotatedBlock implements BlockUIMenuType.BlockUI, MachineTradeTypeBlock {

    public final List<Identifier> trade_type;

    public TradingStationBlock(Properties properties, List<Identifier> trade_type) {
        super(properties);
        this.trade_type = trade_type;
    }

    @Override
    public List<Identifier> machineTradeTypes() {
        return trade_type == null ? List.of() : trade_type;
    }

    public static MapCodec<TradingStationBlock> codec(List<Identifier> trade_type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec())
                .apply(instance, properties -> new TradingStationBlock(properties, trade_type)));
    }

    @Override
    protected @NonNull MapCodec<? extends TradingStationBlock> codec() {
        return codec(trade_type);
    }

    @Override
    public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState state) {
        return new TradingStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockUIMenuType.openUI(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(BlockUIMenuType.BlockUIHolder holder) {
        if (holder.player.level().getBlockEntity(holder.pos) instanceof BaseTradingStationBlockEntity be) {
            return be.createUI(holder);
        }
        return null;
    }
}

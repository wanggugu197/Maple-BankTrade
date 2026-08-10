package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;

import java.util.List;

public class TradingStationBlockEntity extends BaseTradingStationBlockEntity {

    private final List<Identifier> fallbackIds = List.of();
    private final Identifier fallbackId = MapleBankTrade.id("dummy");

    public TradingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        TradingStationBlock block = (TradingStationBlock) state.getBlock();

        super(type, pos, state, block.getStorageSpec(), block.isRunsAutoTrade());
    }

    @Override
    protected List<Identifier> fallbackTradeTypeIds() {
        return fallbackIds;
    }

    @Override
    protected Identifier fallbackTradeTypeId() {
        return fallbackId;
    }
}

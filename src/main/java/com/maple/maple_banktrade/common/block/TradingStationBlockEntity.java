package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;

import java.util.List;

public class TradingStationBlockEntity extends BaseTradingStationBlockEntity {

    private final List<ResourceLocation> fallbackIds = List.of();
    private final ResourceLocation fallbackId = MapleBankTrade.id("dummy");

    public TradingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state,
                ((TradingStationBlock) state.getBlock()).getStorageSpec(),
                ((TradingStationBlock) state.getBlock()).isRunsAutoTrade());
    }

    @Override
    protected List<ResourceLocation> fallbackTradeTypeIds() {
        return fallbackIds;
    }

    @Override
    protected ResourceLocation fallbackTradeTypeId() {
        return fallbackId;
    }
}

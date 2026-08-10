package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machineTrade.station.TradingStationStorageSpec;

import java.util.List;

public class TradingStationBlockEntity extends BaseTradingStationBlockEntity {

    private final List<Identifier> fallbackIds = List.of();
    private final Identifier fallbackId = MapleBankTrade.id("dummy");

    public TradingStationBlockEntity(BlockPos pos, BlockState state,
                                     BlockEntityType<?> type,
                                     TradingStationStorageSpec storageSpec,
                                     boolean allowAutoTrade) {
        super(type, pos, state, storageSpec, allowAutoTrade);
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

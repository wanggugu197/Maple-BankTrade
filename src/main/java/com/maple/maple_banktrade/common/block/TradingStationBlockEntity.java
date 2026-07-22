package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.api.machine.base.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machine.base.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.MBTRegistration;
import com.maple.maple_banktrade.common.trade.MachineTradeRegistration;

import java.util.List;

/**
 * 全功能贸易站：大容量规格 + 全部内置机器配方类型。
 */
public class TradingStationBlockEntity extends BaseTradingStationBlockEntity {

    public static final TradingStationStorageSpec STORAGE_SPEC = TradingStationStorageSpec.fullStation();

    public TradingStationBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, false);
    }

    public TradingStationBlockEntity(BlockPos pos, BlockState state, boolean allowAutoTrade) {
        super(MBTRegistration.TRADING_STATION_ENTITY.get(), pos, state, STORAGE_SPEC, allowAutoTrade);
    }

    @Override
    protected List<Identifier> fallbackTradeTypeIds() {
        return MachineTradeRegistration.TRADING_STATION_TYPES;
    }

    @Override
    protected Identifier fallbackTradeTypeId() {
        return MachineTradeRegistration.MACHINE_BENCH.id();
    }
}

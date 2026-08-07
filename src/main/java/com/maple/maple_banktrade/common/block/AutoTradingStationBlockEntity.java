package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.api.machine.base.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machine.base.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.MBTRegistration;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;

import java.util.List;

/**
 * 自动贸易站：开启 {@code allowAutoTrade}，绑定矿石+岩浆与怪物掉落物自动出售类型。
 */
public class AutoTradingStationBlockEntity extends BaseTradingStationBlockEntity {

    public static final TradingStationStorageSpec STORAGE_SPEC = TradingStationStorageSpec.autoStation();

    public AutoTradingStationBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, true);
    }

    public AutoTradingStationBlockEntity(BlockPos pos, BlockState state, boolean allowAutoTrade) {
        super(MBTRegistration.AUTO_TRADING_STATION_ENTITY.get(), pos, state, STORAGE_SPEC, allowAutoTrade);
    }

    @Override
    protected List<Identifier> fallbackTradeTypeIds() {
        return TradeTypeRegistration.AUTO_TRADING_STATION_TYPES;
    }

    @Override
    protected Identifier fallbackTradeTypeId() {
        return TradeTypeRegistration.AUTO_SELL_ORES_LAVA.id();
    }
}

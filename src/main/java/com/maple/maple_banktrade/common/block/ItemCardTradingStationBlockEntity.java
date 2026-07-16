package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.common.MBTRegistration;
import com.maple.maple_banktrade.common.trade.MachineTradeRegistration;

import java.util.List;

/**
 * 物品卡贸易站：较小库存规格，默认 {@code machine_item_desk} 配方。
 */
public class ItemCardTradingStationBlockEntity extends BaseTradingStationBlockEntity {

    public static final TradingStationStorageSpec STORAGE_SPEC = TradingStationStorageSpec.itemCardStation();

    public ItemCardTradingStationBlockEntity(BlockPos pos, BlockState state) {
        super(MBTRegistration.ITEM_CARD_TRADING_STATION_ENTITY.get(), pos, state, STORAGE_SPEC);
    }

    @Override
    protected List<Identifier> fallbackTradeTypeIds() {
        return MachineTradeRegistration.ITEM_CARD_TRADING_STATION_TYPES;
    }

    @Override
    protected Identifier fallbackTradeTypeId() {
        return MachineTradeRegistration.MACHINE_ITEM_DESK.id();
    }
}

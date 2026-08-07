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
 * 物品卡贸易站：较小库存规格，默认 {@code machine_item_desk} 配方。
 * <p>
 * 不开启自动交易；自动出售请使用 {@link AutoTradingStationBlockEntity}。
 * </p>
 */
public class ItemCardTradingStationBlockEntity extends BaseTradingStationBlockEntity {

    public static final TradingStationStorageSpec STORAGE_SPEC = TradingStationStorageSpec.itemCardStation();

    public ItemCardTradingStationBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, false);
    }

    public ItemCardTradingStationBlockEntity(BlockPos pos, BlockState state, boolean allowAutoTrade) {
        super(MBTRegistration.ITEM_CARD_TRADING_STATION_ENTITY.get(), pos, state, STORAGE_SPEC, allowAutoTrade);
    }

    @Override
    protected List<Identifier> fallbackTradeTypeIds() {
        return TradeTypeRegistration.ITEM_CARD_TRADING_STATION_TYPES;
    }

    @Override
    protected Identifier fallbackTradeTypeId() {
        return TradeTypeRegistration.MACHINE_ITEM_DESK.id();
    }
}

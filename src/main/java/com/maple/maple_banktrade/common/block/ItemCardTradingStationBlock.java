package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.api.machine.base.BaseTradingStationBlock;
import com.maple.maple_banktrade.api.machine.base.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.common.MBTRegistration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 物品卡贸易站方块：较小库存规格，默认 {@code machine_item_desk}；不开启自动交易。
 */
public class ItemCardTradingStationBlock extends BaseTradingStationBlock {

    public ItemCardTradingStationBlock(Properties properties, List<Identifier> trade_type) {
        super(properties, trade_type, false);
    }

    public static MapCodec<ItemCardTradingStationBlock> codec(List<Identifier> trade_type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec())
                .apply(instance, properties -> new ItemCardTradingStationBlock(properties, trade_type)));
    }

    @Override
    protected @NonNull MapCodec<? extends ItemCardTradingStationBlock> codec() {
        return codec(trade_type);
    }

    @Override
    protected BaseTradingStationBlockEntity createStationEntity(BlockPos pos, BlockState state) {
        return new ItemCardTradingStationBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends BaseTradingStationBlockEntity> stationEntityType() {
        return MBTRegistration.ITEM_CARD_TRADING_STATION_ENTITY.get();
    }
}

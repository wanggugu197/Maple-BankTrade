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
 * 全功能交易站方块：绑定多机器价目，不开启自动交易 ticker。
 */
public class TradingStationBlock extends BaseTradingStationBlock {

    public TradingStationBlock(Properties properties, List<Identifier> trade_type) {
        super(properties, trade_type, false);
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
    protected BaseTradingStationBlockEntity createStationEntity(BlockPos pos, BlockState state) {
        return new TradingStationBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends BaseTradingStationBlockEntity> stationEntityType() {
        return MBTRegistration.TRADING_STATION_ENTITY.get();
    }
}

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
 * 自动贸易站方块：开启自动交易 ticker，绑定矿石+岩浆与怪物掉落物出售类型。
 */
public class AutoTradingStationBlock extends BaseTradingStationBlock {

    public AutoTradingStationBlock(Properties properties, List<Identifier> trade_type) {
        super(properties, trade_type, true);
    }

    public static MapCodec<AutoTradingStationBlock> codec(List<Identifier> trade_type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec())
                .apply(instance, properties -> new AutoTradingStationBlock(properties, trade_type)));
    }

    @Override
    protected @NonNull MapCodec<? extends AutoTradingStationBlock> codec() {
        return codec(trade_type);
    }

    @Override
    protected BaseTradingStationBlockEntity createStationEntity(BlockPos pos, BlockState state) {
        return new AutoTradingStationBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends BaseTradingStationBlockEntity> stationEntityType() {
        return MBTRegistration.AUTO_TRADING_STATION_ENTITY.get();
    }
}

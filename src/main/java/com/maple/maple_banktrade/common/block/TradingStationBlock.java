package com.maple.maple_banktrade.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlock;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machineTrade.station.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.MBTRegistration;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class TradingStationBlock extends BaseTradingStationBlock {

    @Getter
    private final TradingStationStorageSpec storageSpec;

    public TradingStationBlock(Properties properties,
                               List<ResourceLocation> tradeType,
                               boolean allowAutoTrade,
                               TradingStationStorageSpec storageSpec) {
        super(properties, tradeType, allowAutoTrade);
        this.storageSpec = storageSpec;
    }

    /**
     * 1.21.1 的 {@link net.minecraft.world.level.block.BaseEntityBlock#getRenderShape} 默认返回
     * {@link RenderShape#INVISIBLE}（带方块实体的方块不渲染模型），必须覆盖为
     * {@link RenderShape#MODEL} 才能显示交易站方块模型。
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 静态工厂方法，用于 codec 捕获 tradeType
    public static MapCodec<TradingStationBlock> codec(TradingStationBlock block) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(propertiesCodec())
                .apply(instance, properties -> new TradingStationBlock(
                        properties,
                        block.machineTradeTypes(),
                        block.isRunsAutoTrade(),
                        block.storageSpec)));
    }

    @Override
    protected @NonNull MapCodec<? extends TradingStationBlock> codec() {
        return codec(this);
    }

    @Override
    protected BaseTradingStationBlockEntity createStationEntity(BlockPos pos, BlockState state) {
        return new TradingStationBlockEntity(stationEntityType(), pos, state);
    }

    @Override
    protected BlockEntityType<? extends BaseTradingStationBlockEntity> stationEntityType() {
        return MBTRegistration.COMMON_TRADING_STATION_ENTITY.get();
    }
}

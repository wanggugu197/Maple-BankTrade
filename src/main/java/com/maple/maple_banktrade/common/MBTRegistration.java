package com.maple.maple_banktrade.common;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.util.entry.BlockEntityTypeEntry;
import com.gto.registrylib.util.entry.BlockEntry;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machineTrade.station.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.block.TradingStationBlock;
import com.maple.maple_banktrade.common.block.TradingStationBlockEntity;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.mapleutillib.utils.RLUtils;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTRegistrationHelper.createTradingStation;
import static com.maple.maple_banktrade.common.MBTRegistrationHelper.createTransactionTalisman;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;

public class MBTRegistration {

    public static void init() {}

    // ======================== 交易站方块注册（使用辅助方法简化） ========================

    public static final BlockEntry<TradingStationBlock> TRADING_STATION = createTradingStation(
            REGISTRY,
            TAB_BANK.getKey(), Blocks.OAK_PLANKS,
            TradeTypeRegistration.TRADING_STATION_TYPES, false,
            TradingStationStorageSpec.builder()
                    .itemSlots(24, 24)
                    .fluidTanks(6, 6, 64_000)
                    .energy(Integer.MAX_VALUE)
                    .build(),
            RLUtils.mc("block/oak_planks"));

    public static final BlockEntry<TradingStationBlock> ITEM_CARD_TRADING_STATION = createTradingStation(
            REGISTRY,
            TAB_BANK.getKey(), Blocks.WHITE_WOOL,
            TradeTypeRegistration.ITEM_CARD_TRADING_STATION_TYPES, false,
            TradingStationStorageSpec.builder()
                    .itemSlots(18, 18)
                    .fluidTanks(2, 2, 16_000)
                    .energy(100_000)
                    .build(),
            RLUtils.mc("block/white_wool"));

    public static final BlockEntry<TradingStationBlock> AUTO_TRADING_STATION = createTradingStation(
            REGISTRY,
            TAB_BANK.getKey(), Blocks.GOLD_BLOCK,
            TradeTypeRegistration.AUTO_TRADING_STATION_TYPES, true,
            TradingStationStorageSpec.builder()
                    .itemSlots(27, 9)
                    .fluidTanks(4, 2, 64_000)
                    .energy(50_000)
                    .build(),
            RLUtils.mc("block/gold_block"),
            MapleBankTrade.id("block/wallet"));

    // ======================== 方块实体 & 能力 ========================

    public static final BlockEntityTypeEntry<TradingStationBlockEntity> COMMON_TRADING_STATION_ENTITY = REGISTRY.blockEntity(REGISTRY, "common_trading_station_entity", TradingStationBlockEntity::new)
            .validBlocks(TRADING_STATION, ITEM_CARD_TRADING_STATION, AUTO_TRADING_STATION)
            .register();

    public static void registerTradingStationCapabilities(RegisterCapabilitiesEvent event) {
        BaseTradingStationBlockEntity.registerCapabilities(event, COMMON_TRADING_STATION_ENTITY.get());
    }

    // ======================== 交易符令 ========================

    public static final ItemEntry<ComponentItem> ITEM_DESK = createTransactionTalisman(
            REGISTRY,
            TAB_BANK.getKey(),
            TradeTypeRegistration.MACHINE_ITEM_DESK.id(),
            RLUtils.mc("item/diamond"));
}

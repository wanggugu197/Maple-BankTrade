package com.maple.maple_banktrade.common;

import com.gto.registrylib.util.entry.BlockEntityTypeEntry;
import com.gto.registrylib.util.entry.BlockEntry;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.common.block.ItemCardTradingStationBlock;
import com.maple.maple_banktrade.common.block.ItemCardTradingStationBlockEntity;
import com.maple.maple_banktrade.common.block.TradingStationBlock;
import com.maple.maple_banktrade.common.block.TradingStationBlockEntity;
import com.mapleutillib.utils.RLUtils;
import com.mapleutillib.utils.generator.ModBlockModelGeneratorHelper;
import com.mapleutillib.utils.generator.ModItemModelGeneratorHelper;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;
import static com.maple.maple_banktrade.common.trade.MachineTradeRegistration.ITEM_CARD_TRADING_STATION_TYPES;
import static com.maple.maple_banktrade.common.trade.MachineTradeRegistration.TRADING_STATION_TYPES;

/**
 * 模组内置方块/物品等内容注册入口（非 API）。
 */
public class MBTRegistration {

    /**
     * 触发本类加载，完成交易站等内置内容的 DeferredRegister 登记。
     * <p>
     * 实际注册在静态字段初始化中完成；本方法仅作显式入口。
     * </p>
     */
    public static void init() {}

    /**
     * 交易站：multipart 叠加自定义模型 + 橡木木板；物品用 composite 联合渲染。
     * <p>
     * 几何模型仍使用 {@code models/block/trading_station.json}，此处只生成
     * blockstate / items 映射。
     */
    public static final BlockEntry<TradingStationBlock> TRADING_STATION = REGISTRY
            .block("trading_station", p -> new TradingStationBlock(p, TRADING_STATION_TYPES))
            .langCn("交易站")
            .blockstate(() -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block,
                    MapleBankTrade.id("block/trading_station"),
                    RLUtils.mc("block/oak_planks")))
            .item(builder -> builder
                    .addTab(TAB_BANK.getKey())
                    .model(() -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item,
                            RLUtils.mc("block/oak_planks"),
                            MapleBankTrade.id("block/trading_station"))))
            .register();

    public static final BlockEntityTypeEntry<TradingStationBlockEntity> TRADING_STATION_ENTITY = REGISTRY
            .blockEntity(REGISTRY, "trading_station_entity", (_, p, s) -> new TradingStationBlockEntity(p, s))
            .validBlock(TRADING_STATION)
            .register();

    /**
     * 物品卡贸易站：较小 {@link com.maple.maple_banktrade.common.block.TradingStationStorageSpec}，
     * 默认 item_desk 配方；仍具备物品/流体/能量/卡存储。
     */
    public static final BlockEntry<ItemCardTradingStationBlock> ITEM_CARD_TRADING_STATION = REGISTRY
            .block("item_card_trading_station", p -> new ItemCardTradingStationBlock(p, ITEM_CARD_TRADING_STATION_TYPES))
            .langCn("物品卡贸易站")
            .lang("Item-Card Trading Station")
            .blockstate(() -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block,
                    MapleBankTrade.id("block/trading_station"),
                    RLUtils.mc("block/crafting_table_top")))
            .item(builder -> builder
                    .addTab(TAB_BANK.getKey())
                    .model(() -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item,
                            RLUtils.mc("block/crafting_table_top"),
                            MapleBankTrade.id("block/trading_station"))))
            .register();

    public static final BlockEntityTypeEntry<ItemCardTradingStationBlockEntity> ITEM_CARD_TRADING_STATION_ENTITY = REGISTRY
            .blockEntity(REGISTRY, "item_card_trading_station_entity", (_, p, s) -> new ItemCardTradingStationBlockEntity(p, s))
            .validBlock(ITEM_CARD_TRADING_STATION)
            .register();
}

package com.maple.maple_banktrade.common;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.tooltip.SubNode;
import com.gto.registrylib.util.entry.BlockEntityTypeEntry;
import com.gto.registrylib.util.entry.BlockEntry;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.data.TradableType;
import com.maple.maple_banktrade.api.machineTrade.itemAttachment.MachineTradeItemAttachment;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;
import com.maple.maple_banktrade.api.machineTrade.station.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.block.TradingStationBlock;
import com.maple.maple_banktrade.common.block.TradingStationBlockEntity;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.mapleutillib.api.composite.UIComponentItem;
import com.mapleutillib.api.registry.ModRegistryCore;
import com.mapleutillib.utils.RLUtils;
import com.mapleutillib.utils.generator.ModBlockModelGeneratorHelper;
import com.mapleutillib.utils.generator.ModItemModelGeneratorHelper;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;
import static com.maple.maple_banktrade.common.MBTTags.*;

/**
 * 模组内置方块/物品等内容注册入口（非 API）。
 */
public class MBTRegistration {

    public static void init() {}

    /**
     * 交易站：multipart 叠加自定义模型 + 橡木木板；物品用 composite 联合渲染。
     * <p>
     * 几何模型仍使用 {@code models/block/trading_station.json}，此处只生成
     * blockstate / items 映射。
     */
    public static final BlockEntry<TradingStationBlock> TRADING_STATION = REGISTRY
            .block("trading_station", p -> new TradingStationBlock(
                    p,
                    TradeTypeRegistration.TRADING_STATION_TYPES,
                    false,
                    TradingStationStorageSpec.builder()
                            .itemSlots(24, 24)
                            .fluidTanks(6, 6, 64_000)
                            .energy(Integer.MAX_VALUE)
                            .build()))
            .langCn("交易站")
            .initialProperties(Blocks.OAK_PLANKS)
            .blockstate(() -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block,
                    MapleBankTrade.id("block/trading_station"),
                    RLUtils.mc("block/oak_planks")))
            .addTag(TRADING_STATION_BLOCK)
            .item(builder -> builder
                    .addTab(TAB_BANK.getKey())
                    .addTag(TRADING_STATION_ITEM)
                    .model(() -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item,
                            RLUtils.mc("block/oak_planks"),
                            MapleBankTrade.id("block/trading_station"))))
            .register();

    /**
     * 物品卡贸易站：较小 {@link TradingStationStorageSpec}，
     * 默认 item_desk 配方；仍具备物品/流体/能量/卡存储。
     */
    public static final BlockEntry<TradingStationBlock> ITEM_CARD_TRADING_STATION = REGISTRY
            .block("item_card_trading_station", p -> new TradingStationBlock(
                    p,
                    TradeTypeRegistration.ITEM_CARD_TRADING_STATION_TYPES,
                    false,
                    TradingStationStorageSpec.builder()
                            .itemSlots(18, 18)
                            .fluidTanks(2, 2, 16_000)
                            .energy(100_000)
                            .build()))
            .langCn("物品卡贸易站")
            .lang("Item-Card Trading Station")
            .initialProperties(Blocks.WHITE_WOOL)
            .blockstate(() -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block,
                    MapleBankTrade.id("block/trading_station"),
                    RLUtils.mc("block/white_wool")))
            .addTag(TRADING_STATION_BLOCK)
            .item(builder -> builder
                    .addTab(TAB_BANK.getKey())
                    .addTag(TRADING_STATION_ITEM)
                    .model(() -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item,
                            RLUtils.mc("block/white_wool"),
                            MapleBankTrade.id("block/trading_station"))))
            .register();

    /**
     * 自动贸易站：开启自动交易，绑定矿石+岩浆与怪物掉落物自动出售类型。
     */
    public static final BlockEntry<TradingStationBlock> AUTO_TRADING_STATION = REGISTRY
            .block("auto_trading_station", p -> new TradingStationBlock(
                    p,
                    TradeTypeRegistration.AUTO_TRADING_STATION_TYPES,
                    true,
                    TradingStationStorageSpec.builder()
                            .itemSlots(27, 9)
                            .fluidTanks(4, 2, 64_000)
                            .energy(50_000)
                            .build()))
            .langCn("自动贸易站")
            .lang("Auto Trading Station")
            .initialProperties(Blocks.GOLD_BLOCK)
            .blockstate(() -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block,
                    MapleBankTrade.id("block/trading_station"),
                    RLUtils.mc("block/gold_block")))
            .addTag(TRADING_STATION_BLOCK)
            .item(builder -> builder
                    .addTab(TAB_BANK.getKey())
                    .addTag(TRADING_STATION_ITEM)
                    .model(() -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item,
                            RLUtils.mc("block/gold_block"),
                            MapleBankTrade.id("block/trading_station"))))
            .register();

    public static final BlockEntityTypeEntry<TradingStationBlockEntity> COMMON_TRADING_STATION_ENTITY = REGISTRY
            .blockEntity(REGISTRY, "common_trading_station_entity", TradingStationBlockEntity::new)
            .validBlocks(TRADING_STATION, ITEM_CARD_TRADING_STATION, AUTO_TRADING_STATION)
            .register();

    public static void registerTradingStationCapabilities(RegisterCapabilitiesEvent event) {
        BaseTradingStationBlockEntity.registerCapabilities(event, COMMON_TRADING_STATION_ENTITY.get());
    }

    public static final ItemEntry<ComponentItem> ITEM_DESK = createTransactionTalisman(
            REGISTRY, TAB_BANK.getKey(),
            TradeTypeRegistration.MACHINE_ITEM_DESK.id(), RLUtils.mc("item/diamond"));

    public static ItemEntry<ComponentItem> createTransactionTalisman(ModRegistryCore registryCore, ResourceKey<CreativeModeTab> tab,
                                                                     Identifier type, Identifier overlay) {
        return registryCore
                .componentItem("transaction_talisman_" + type.toString().replace(":", "_"), UIComponentItem::new)
                .lang("Transaction Talisman")
                .langCn("交易符令")
                .attach(new MachineTradeItemAttachment(type))
                .addTag(TRADING_TALISMAN_ITEM)
                .addTab(tab)
                .addTooltip((collector, _) -> {
                    collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.transaction_talisman", Component.translatable(TradableType.getTradableTypeTranslationKey(type)))));
                    TradableType.requireById(type).description().forEach(c -> collector.node(new SubNode.Basic(c)));
                })
                .model(() -> (item, prov) -> ModItemModelGeneratorHelper.createMultiLayerItem(prov, item,
                        MapleBankTrade.id("item/transaction_talisman"),
                        MapleBankTrade.id("item/talisman"),
                        overlay))
                .register();
    }
}

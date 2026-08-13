package com.maple.maple_banktrade.common;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import com.google.gson.JsonObject;
import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.datagen.generator.RegistryLibBlockModelGenerator;
import com.gto.registrylib.datagen.generator.RegistryLibItemModelGenerator;
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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;
import static com.maple.maple_banktrade.common.MBTTags.*;

public class MBTRegistration {

    public static void init() {}

    // ======================== 交易站方块注册（使用辅助方法简化） ========================

    public static final BlockEntry<TradingStationBlock> TRADING_STATION = createTradingStation(
            REGISTRY,
            "Trading Station", "交易站",
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
            "Item-Card Trading Station", "物品卡贸易站",
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
            "Auto Trading Station", "自动贸易站",
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

    // ======================== 辅助方法（支持 Lambda 自定义生成） ========================

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       String nameEn,
                                                                       String nameZh,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<Identifier> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Identifier model) {
        Identifier base = MapleBankTrade.id("block/trading_station");
        return createTradingStation(
                registryCore, nameEn, nameZh,
                tab, initProperties, tradeType, allowAutoTrade, storageSpec,
                () -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block, base, model),
                () -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item, base, model));
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       String nameEn,
                                                                       String nameZh,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<Identifier> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Identifier model,
                                                                       Identifier icon) {
        Identifier base = MapleBankTrade.id("block/trading_station");
        Identifier modelId = RLUtils.get(registryCore.getModid(),
                "block/trading_station/" + StringUtils.substringAfterLast(icon.getPath(), "/"));
        return createTradingStation(
                registryCore, nameEn, nameZh,
                tab, initProperties, tradeType, allowAutoTrade, storageSpec,
                () -> (block, prov) -> {
                    prov.modelOutput.accept(modelId, () -> {
                        JsonObject root = new JsonObject();
                        root.addProperty("parent", base.toString());
                        JsonObject textures = new JsonObject();
                        textures.addProperty("icon", icon.toString());
                        root.add("textures", textures);
                        return root;
                    });
                    ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block, modelId, model);
                },
                () -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item, modelId, model));
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       String nameEn,
                                                                       String nameZh,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<Identifier> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Supplier<BiConsumer<TradingStationBlock, RegistryLibBlockModelGenerator>> blockStateGenerator,
                                                                       Supplier<BiConsumer<BlockItem, RegistryLibItemModelGenerator>> itemModelGenerator) {
        return registryCore
                .block(nameEn.toLowerCase().replaceAll("[^a-z0-9]+", "_"), p -> new TradingStationBlock(p, tradeType, allowAutoTrade, storageSpec))
                .langCn(nameZh)
                .lang(nameEn)
                .initialProperties(initProperties)
                .blockstate(blockStateGenerator)
                .addTag(TRADING_STATION_BLOCK)
                .item(builder -> builder
                        .addTab(tab)
                        .addTag(TRADING_STATION_ITEM)
                        .model(itemModelGenerator))
                .register();
    }

    public static ItemEntry<ComponentItem> createTransactionTalisman(
                                                                     ModRegistryCore registryCore,
                                                                     ResourceKey<CreativeModeTab> tab,
                                                                     Identifier type,
                                                                     Identifier overlay) {
        return registryCore
                .componentItem("transaction_talisman_" + type.getPath(), UIComponentItem::new)
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

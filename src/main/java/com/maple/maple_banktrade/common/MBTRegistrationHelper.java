package com.maple.maple_banktrade.common;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;

import com.google.gson.JsonObject;
import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.datagen.generator.RegistryLibBlockModelGenerator;
import com.gto.registrylib.datagen.generator.RegistryLibItemModelGenerator;
import com.gto.registrylib.tooltip.SubNode;
import com.gto.registrylib.util.entry.BlockEntry;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.data.TradableType;
import com.maple.maple_banktrade.api.machineTrade.itemAttachment.MachineTradeItemAttachment;
import com.maple.maple_banktrade.api.machineTrade.station.TradingStationStorageSpec;
import com.maple.maple_banktrade.common.block.TradingStationBlock;
import com.mapleutillib.api.composite.UIComponentItem;
import com.mapleutillib.api.registry.ModRegistryCore;
import com.mapleutillib.utils.RLUtils;
import com.mapleutillib.utils.generator.ModBlockModelGeneratorHelper;
import com.mapleutillib.utils.generator.ModItemModelGeneratorHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.maple.maple_banktrade.common.MBTTags.*;

public class MBTRegistrationHelper {

    // ======================== 辅助方法 ========================

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<Identifier> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Identifier model) {
        Identifier base = MapleBankTrade.id("block/trading_station");
        return createTradingStation(
                registryCore,
                tab, initProperties, tradeType, allowAutoTrade, storageSpec,
                () -> (block, prov) -> ModBlockModelGeneratorHelper.createHorizontalMultiPartBlock(prov, block, base, model),
                () -> (item, prov) -> ModItemModelGeneratorHelper.mapItemToComposite(prov, item, base, model));
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
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
                registryCore,
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
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<Identifier> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Supplier<BiConsumer<TradingStationBlock, RegistryLibBlockModelGenerator>> blockStateGenerator,
                                                                       Supplier<BiConsumer<BlockItem, RegistryLibItemModelGenerator>> itemModelGenerator) {
        return registryCore
                .block("trading_station_" + tradeType.stream().map(Identifier::getPath).collect(Collectors.joining("_")), p -> new TradingStationBlock(p, tradeType, allowAutoTrade, storageSpec))
                .langCn("贸易站")
                .lang("Trading Station")
                .initialProperties(initProperties)
                .blockstate(blockStateGenerator)
                .addTag(TRADING_STATION_BLOCK)
                .item(builder -> builder
                        .addTab(tab)
                        .addTag(TRADING_STATION_ITEM)
                        .model(itemModelGenerator)
                        .addTooltip((collector, _) -> {
                            for (Identifier type : tradeType) {
                                collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.transaction_talisman", Component.translatable(TradableType.getTradableTypeTranslationKey(type)))));
                                TradableType.requireById(type).description().forEach(c -> collector.node(new SubNode.Basic(c)));
                            }
                        }))
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

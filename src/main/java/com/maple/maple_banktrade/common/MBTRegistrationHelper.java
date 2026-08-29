package com.maple.maple_banktrade.common;

import net.minecraft.core.Direction;
import net.minecraft.data.models.blockstates.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
import com.mapleutillib.utils.generator.ModItemModelGeneratorHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.maple.maple_banktrade.common.MBTTags.*;

public class MBTRegistrationHelper {

    // ======================== 辅助方法 ========================

    /**
     * 获取 registrylib 方块模型生成器的 blockstate 输出消费者。
     * <p>
     * registrylib 8.0.16 将 {@code RegistryLibBlockModelGenerator.blockStateOutput} 声明为私有，
     * 但联合（multipart）blockstate 生成需要 {@link Consumer}{@code <BlockStateGenerator>}。
     * 此处通过反射取回该字段，以便数据生成代码输出 multipart 联合模型格式。
     * </p>
     */
    private static Consumer<BlockStateGenerator> blockStateOutput(RegistryLibBlockModelGenerator prov) {
        try {
            Field field = RegistryLibBlockModelGenerator.class.getDeclaredField("blockStateOutput");
            field.setAccessible(true);
            return (Consumer<BlockStateGenerator>) field.get(prov);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to access RegistryLibBlockModelGenerator.blockStateOutput", e);
        }
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<ResourceLocation> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       ResourceLocation base) {
        return createTradingStation(
                registryCore,
                tab, initProperties, tradeType, allowAutoTrade, storageSpec,
                base, MapleBankTrade.id("block/trading_station/icon"));
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<ResourceLocation> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       ResourceLocation base,
                                                                       ResourceLocation icon) {
        ResourceLocation model = MapleBankTrade.id("block/trading_station");
        ResourceLocation modelId = RLUtils.get(registryCore.getModid(),
                "block/trading_station_" + tradeType.stream().map(ResourceLocation::getPath).collect(Collectors.joining("_")));
        return createTradingStation(
                registryCore,
                tab, initProperties, tradeType, allowAutoTrade, storageSpec,
                () -> (block, prov) -> {
                    prov.getBuilder()
                            .parent(model)
                            .transformTexture(map -> map.put("base", base.toString()))
                            .transformTexture(map -> map.put("icon", icon.toString()))
                            .build(modelId);
                    MultiPartGenerator generator = MultiPartGenerator.multiPart(block);
                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        Variant variant = Variant.variant()
                                .with(VariantProperties.MODEL, modelId)
                                .with(VariantProperties.Y_ROT, switch (facing) {
                                    case EAST -> VariantProperties.Rotation.R90;
                                    case SOUTH -> VariantProperties.Rotation.R180;
                                    case WEST -> VariantProperties.Rotation.R270;
                                    default -> VariantProperties.Rotation.R0;
                                });
                        generator = generator.with(Condition.condition().term(BlockStateProperties.HORIZONTAL_FACING, facing), variant);
                    }
                    blockStateOutput(prov).accept(generator);
                });
    }

    public static BlockEntry<TradingStationBlock> createTradingStation(
                                                                       ModRegistryCore registryCore,
                                                                       ResourceKey<CreativeModeTab> tab,
                                                                       Block initProperties,
                                                                       List<ResourceLocation> tradeType,
                                                                       boolean allowAutoTrade,
                                                                       TradingStationStorageSpec storageSpec,
                                                                       Supplier<BiConsumer<TradingStationBlock, RegistryLibBlockModelGenerator>> blockStateGenerator) {
        return registryCore
                .block("trading_station_" + tradeType.stream().map(ResourceLocation::getPath).collect(Collectors.joining("_")), p -> new TradingStationBlock(p, tradeType, allowAutoTrade, storageSpec))
                .langCn("贸易站")
                .lang("Trading Station")
                .initialProperties(initProperties)
                .blockstate(blockStateGenerator)
                .addTag(TRADING_STATION_BLOCK)
                .item(itemBuilder -> itemBuilder
                        .addTab(tab)
                        .addTag(TRADING_STATION_ITEM)
                        .addTooltip((collector, ignored) -> {
                            for (ResourceLocation type : tradeType) {
                                collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.transaction_talisman", Component.translatable(TradableType.getTradableTypeTranslationKey(type)))));
                                TradableType.requireById(type).description().forEach(c -> collector.node(new SubNode.Basic(c)));
                            }
                        }))
                .register();
    }

    public static ItemEntry<ComponentItem> createTransactionTalisman(
                                                                     ModRegistryCore registryCore,
                                                                     ResourceKey<CreativeModeTab> tab,
                                                                     ResourceLocation type,
                                                                     ResourceLocation overlay) {
        return registryCore
                .componentItem("transaction_talisman_" + type.getPath(), UIComponentItem::new)
                .lang("Transaction Talisman")
                .langCn("交易符令")
                .attach(new MachineTradeItemAttachment(type))
                .addTag(TRADING_TALISMAN_ITEM)
                .addTab(tab)
                .addTooltip((collector, ignored) -> {
                    collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.transaction_talisman", Component.translatable(TradableType.getTradableTypeTranslationKey(type)))));
                    TradableType.requireById(type).description().forEach(c -> collector.node(new SubNode.Basic(c)));
                })
                .model(() -> (item, prov) -> {
                    ResourceLocation modelId = createTransactionTalismanItemModel(prov, prov.name(() -> item) + "0", overlay);
                    ModItemModelGeneratorHelper.mapItemToModel(prov, item, modelId);
                })
                .register();
    }

    public static ResourceLocation createTransactionTalismanItemModel(RegistryLibItemModelGenerator prov, String id, ResourceLocation overlay) {
        ResourceLocation modelId = prov.modLoc("block/" + id);
        prov.modelOutput.accept(modelId, () -> {
            JsonObject root = new JsonObject();
            root.addProperty("parent", MapleBankTrade.id("block/transaction_talisman").toString());
            JsonObject textures = new JsonObject();
            textures.addProperty("talisman", MapleBankTrade.id("item/talisman").toString());
            textures.addProperty("icon", overlay.toString());
            root.add("textures", textures);
            return root;
        });
        return modelId;
    }
}

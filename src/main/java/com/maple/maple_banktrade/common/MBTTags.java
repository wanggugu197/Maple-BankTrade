package com.maple.maple_banktrade.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.maple.maple_banktrade.MapleBankTrade;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;

/**
 * 标签注册。
 */
public class MBTTags {

    // ==============================================
    // 标签
    // ==============================================
    public static final TagKey<Item> TRADING_STATION_ITEM = createItemTag("trading_station");
    public static final TagKey<Item> TRADING_TALISMAN_ITEM = createItemTag("trading_talisman");
    public static final TagKey<Block> TRADING_STATION_BLOCK = createBlockTag("trading_station");

    // ==============================================
    // 初始化
    // ==============================================

    /** 将本模组物品标签加入注册表。 */
    public static void init() {
        REGISTRY.itemTags().add(TRADING_STATION_ITEM);
        REGISTRY.blockTags().add(TRADING_STATION_BLOCK);
    }

    /** 创建本模组命名空间下的标签。 */
    private static TagKey<Item> createItemTag(String path) {
        return TagKey.create(Registries.ITEM, MapleBankTrade.id(path));
    }

    private static TagKey<Block> createBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, MapleBankTrade.id(path));
    }
}

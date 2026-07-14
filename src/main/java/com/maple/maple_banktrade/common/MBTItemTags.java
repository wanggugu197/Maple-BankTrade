package com.maple.maple_banktrade.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import com.maple.maple_banktrade.MapleBankTrade;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;

/**
 * 物品标签注册。
 */
public class MBTItemTags {

    // ==============================================
    // 标签
    // ==============================================

    public static final TagKey<Item> TRADING_DEVICES = createItemTag("trading_devices");

    // ==============================================
    // 初始化
    // ==============================================

    /** 将本模组物品标签加入注册表。 */
    public static void init() {
        REGISTRY.itemTags().add(TRADING_DEVICES);
    }

    /** 创建本模组命名空间下的物品标签。 */
    private static TagKey<Item> createItemTag(String path) {
        return TagKey.create(Registries.ITEM, MapleBankTrade.id(path));
    }
}

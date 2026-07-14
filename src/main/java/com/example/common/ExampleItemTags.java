package com.example.common;

import com.example.ExampleMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import static com.example.ExampleMod.REGISTRY;

/**
 * 物品标签注册
 */
public class ExampleItemTags {

    public static void init() {
        REGISTRY.itemTags().add(EXAMPLE);
    }

    public static final TagKey<Item> EXAMPLE = createItemTag("example");

    private static TagKey<Item> createItemTag(String path) {
        return TagKey.create(Registries.ITEM, ExampleMod.id(path));
    }
}

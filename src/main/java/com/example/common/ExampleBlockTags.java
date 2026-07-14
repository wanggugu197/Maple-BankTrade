package com.example.common;

import com.example.ExampleMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import static com.example.ExampleMod.REGISTRY;

/**
 * 方块标签注册
 */
public class ExampleBlockTags {

    public static void init() {
        REGISTRY.blockTags().add(EXAMPLE);
    }

    public static final TagKey<Block> EXAMPLE = createBlockTag("example");

    private static TagKey<Block> createBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, ExampleMod.id(path));
    }
}

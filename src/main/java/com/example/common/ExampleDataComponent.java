package com.example.common;

import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;

import com.gto.registrylib.util.entry.DataComponentTypeEntry;

import static com.example.ExampleMod.REGISTRY;

/**
 * 数据组件注册
 */
public class ExampleDataComponent {

    public static void init() {}

    public static final DataComponentTypeEntry<Integer> EXAMPLE = REGISTRY.dataComponentTypeEntry(
            "example",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DataComponentTypeEntry<Vec3i> COORDINATE = REGISTRY.dataComponentTypeEntry(
            "coordinate",
            builder -> builder.persistent(Vec3i.CODEC).networkSynchronized(Vec3i.STREAM_CODEC));
}

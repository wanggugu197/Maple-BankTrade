package com.maple.maple_banktrade.api.bank.item;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.gto.registrylib.util.entry.DataComponentTypeEntry;

import java.util.Set;
import java.util.UUID;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;

/**
 * 数据组件注册
 */
public class BankDataComponent {

    public static void init() {}

    public static final DataComponentTypeEntry<Set<UUID>> CARD_PERMISSIONS = REGISTRY.dataComponentTypeEntry(
            "card_permissions",
            builder -> builder
                    .persistent(UUIDUtil.CODEC_LINKED_SET)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Sets::newLinkedHashSet, Lists::newArrayList)));

    public static final DataComponentTypeEntry<ResourceLocation> CARD_NAME_INDEX = REGISTRY.dataComponentTypeEntry(
            "card_name_index",
            builder -> builder
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));
}

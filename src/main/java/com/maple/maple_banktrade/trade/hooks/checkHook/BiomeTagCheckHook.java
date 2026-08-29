package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 群系标签钩子：当所在位置所属群系包含指定标签时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class BiomeTagCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private ResourceLocation tagId;
    @Persisted
    private boolean flip;

    public BiomeTagCheckHook(ResourceLocation tagId) {
        this(tagId, false);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip;
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
        boolean condition = context.level().getBiome(pos).is(tag);
        return flip != condition;
    }
}

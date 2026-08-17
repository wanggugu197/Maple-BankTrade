package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 群系标签钩子：当所在位置所属群系包含指定标签时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class BiomeTagVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier tagId;
    @Persisted
    private boolean flip;

    public BiomeTagVisibleHook(Identifier tagId) {
        this(tagId, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip ? FLAG_VISIBLE : 0;
        TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
        boolean condition = context.level().getBiome(pos).is(tag);
        return (flip != condition) ? FLAG_VISIBLE : 0;
    }
}

package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 群系钩子：当所在位置所属群系与 {@link #biomeId} 匹配时返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class BiomeVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier biomeId;
    @Persisted
    private boolean flip;

    public BiomeVisibleHook(Identifier biomeId) {
        this(biomeId, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip;
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, biomeId);
        boolean condition = context.level().getBiome(pos).is(key);
        return flip != condition;
    }
}

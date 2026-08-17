package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 具体结构钩子：当所在位置位于指定的具体结构实例内部时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class StructureVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier structureId;
    @Persisted
    private boolean flip;

    public StructureVisibleHook(Identifier structureId) {
        this(structureId, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip ? FLAG_VISIBLE : 0;
        var registry = context.level().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, structureId);
        Holder<Structure> holder = registry.get(key).orElse(null);
        if (holder == null) return flip ? FLAG_VISIBLE : 0;
        Structure structure = holder.value();
        boolean condition = context.level().structureManager()
                .getStructureWithPieceAt(pos, structure).isValid();
        return (flip != condition) ? FLAG_VISIBLE : 0;
    }
}

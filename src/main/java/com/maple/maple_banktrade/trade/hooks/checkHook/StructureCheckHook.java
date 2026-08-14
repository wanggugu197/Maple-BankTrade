package com.maple.maple_banktrade.trade.hooks.checkHook;

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
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 具体结构钩子：当所在位置位于指定的具体结构实例内部时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class StructureCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private Identifier structureId;
    @Persisted
    private boolean flip;

    public StructureCheckHook(Identifier structureId) {
        this(structureId, false);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip;
        var registry = context.level().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, structureId);
        Holder<Structure> holder = registry.get(key).orElse(null);
        if (holder == null) return flip;
        Structure structure = holder.value();
        boolean condition = context.level().structureManager()
                .getStructureWithPieceAt(pos, structure).isValid();
        return flip != condition;
    }
}

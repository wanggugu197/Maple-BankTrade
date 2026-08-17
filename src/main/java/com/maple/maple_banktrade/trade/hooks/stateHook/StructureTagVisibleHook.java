package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 结构标签钩子：当所在位置位于任意属于指定标签的结构内部时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class StructureTagVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier tagId;
    @Persisted
    private boolean flip;

    public StructureTagVisibleHook(Identifier tagId) {
        this(tagId, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return flip ? FLAG_VISIBLE : 0;
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, tagId);
        boolean condition = context.level().structureManager()
                .getStructureWithPieceAt(pos, tag).isValid();
        return (flip != condition) ? FLAG_VISIBLE : 0;
    }
}

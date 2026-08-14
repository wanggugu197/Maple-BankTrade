package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.core.BlockPos;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 高度钩子：当所在位置 Y 坐标在 {@link #minY} 与 {@link #maxY} 之间（含）时返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class HeightVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private int minY;
    @Persisted
    private int maxY;

    /** 便捷构造：高度 ≥ minY（不设上限） */
    public HeightVisibleHook(int minY) {
        this(minY, Integer.MAX_VALUE);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return false;
        return pos.getY() >= minY && pos.getY() <= maxY;
    }
}

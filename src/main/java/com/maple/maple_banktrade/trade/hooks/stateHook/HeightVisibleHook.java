package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.core.BlockPos;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 高度钩子：当所在位置 Y 坐标在 {@link #minY} 与 {@link #maxY} 之间（含）时可见。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class HeightVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private int minY;
    @Persisted
    private int maxY;

    /** 便捷构造：高度 ≥ minY（不设上限） */
    public HeightVisibleHook(int minY) {
        this(minY, Integer.MAX_VALUE);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return 0;
        boolean inside = pos.getY() >= minY && pos.getY() <= maxY;
        return inside ? FLAG_VISIBLE : 0;
    }
}

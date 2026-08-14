package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.core.BlockPos;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 高度钩子：当所在位置 Y 坐标在 {@link #minY} 与 {@link #maxY} 之间（含）时返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class HeightCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private int minY;
    @Persisted
    private int maxY;

    /** 便捷构造：高度 ≥ minY（不设上限） */
    public HeightCheckHook(int minY) {
        this(minY, Integer.MAX_VALUE);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return false;
        return pos.getY() >= minY && pos.getY() <= maxY;
    }
}

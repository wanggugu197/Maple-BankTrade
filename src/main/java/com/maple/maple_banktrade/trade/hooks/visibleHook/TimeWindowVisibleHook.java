package com.maple.maple_banktrade.trade.hooks.visibleHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 时间区间钩子：当前维度的维度默认时钟时间在 {@link #startTick} {@link #endTick} 之间时返回 true
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TimeWindowVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private long startTick;
    @Persisted
    private long endTick;

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        long dayTime = context.level().getDefaultClockTime() % 24000L;
        if (startTick <= endTick) {
            return dayTime >= startTick && dayTime <= endTick;
        } else {
            return dayTime >= startTick || dayTime <= endTick;
        }
    }
}

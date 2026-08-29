package com.maple.maple_banktrade.trade.hooks.stateHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 时间区间钩子：当前维度的默认时钟时间在 {@link #startTick} {@link #endTick} 之间时可见。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TimeWindowVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private long startTick;
    @Persisted
    private long endTick;

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        long dayTime = context.level().getDayTime() % 24000L;
        boolean inside;
        if (startTick <= endTick) {
            inside = dayTime >= startTick && dayTime <= endTick;
        } else {
            inside = dayTime >= startTick || dayTime <= endTick;
        }
        return inside ? FLAG_VISIBLE : 0;
    }
}

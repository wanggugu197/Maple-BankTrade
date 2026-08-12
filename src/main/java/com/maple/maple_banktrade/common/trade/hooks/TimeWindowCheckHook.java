package com.maple.maple_banktrade.common.trade.hooks;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TimeWindowCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private long startTick;
    @Persisted
    private long endTick;

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        long dayTime = context.level().getDefaultClockTime() % 24000L;
        return dayTime >= startTick && dayTime <= endTick;
    }
}

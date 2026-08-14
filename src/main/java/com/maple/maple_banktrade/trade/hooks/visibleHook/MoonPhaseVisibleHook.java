package com.maple.maple_banktrade.trade.hooks.visibleHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 月相钩子：当所在维度当前月相与 {@link #phase} 匹配时返回 true。
 * <p>
 * 月相取值 0~7（0 = 满月，4 = 新月），按 {@code (世界天数 % 8)} 计算。
 * </p>
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class MoonPhaseVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private int phase;
    @Persisted
    private boolean flip;

    public MoonPhaseVisibleHook(int phase) {
        this(phase, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        long dayTime = context.level().getDefaultClockTime();
        int current = (int) ((dayTime / 24000L) % 8);
        return flip != (current == phase);
    }
}

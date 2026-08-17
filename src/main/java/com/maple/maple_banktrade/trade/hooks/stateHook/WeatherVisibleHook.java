package com.maple.maple_banktrade.trade.hooks.stateHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 天气钩子：当所在世界当前天气与 {@link #state} 匹配时可见。
 * <p>
 * state 取值：0 = 晴天，1 = 下雨（不含雷暴），2 = 雷暴；未知值按晴天处理。
 * </p>
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class WeatherVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private short state;
    @Persisted
    private boolean flip;

    public WeatherVisibleHook(short state) {
        this(state, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        boolean condition = switch (state) {
            case 1 -> context.level().isRaining() && !context.level().isThundering();
            case 2 -> context.level().isThundering();
            default -> !context.level().isRaining(); // 0 = 晴天
        };
        return (flip != condition) ? FLAG_VISIBLE : 0;
    }
}

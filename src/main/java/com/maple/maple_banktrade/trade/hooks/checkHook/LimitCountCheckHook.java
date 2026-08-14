package com.maple.maple_banktrade.trade.hooks.checkHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;

/**
 * 限购 / 次数上限钩子：将本次交易的期望次数降级为 {@code min(请求次数, maxCount)}，
 * 随后放行（返回 true）。
 * <p>
 * 例如 {@code new LimitCountCheckHook(1)} 使交易每次执行最多 1 次（自动交易也会被限制）。
 * 依赖 {@link MachineTradeRequest#setDesiredCount(int)}：{@link MachineTradeDefinition}
 * 会在 check 钩子返回后重新读取降级后的次数。
 * </p>
 */
public final class LimitCountCheckHook extends MachineTradeHooks.CheckHook {

    /** 单次交易允许的最大次数 */
    @Persisted
    private int maxCount;

    /** 无参构造：限制为 1 次 */
    public LimitCountCheckHook() {
        this(1);
    }

    public LimitCountCheckHook(int maxCount) {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount must be positive");
        }
        this.maxCount = maxCount;
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        request.setDesiredCount(Math.min(request.desiredCount(), maxCount));
        return true;
    }
}

package com.maple.maple_banktrade.trade.hooks.successHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 组合成功回调钩子：依次执行所有子钩子的 {@link #afterSuccess} 方法。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CompositeSuccessHook extends MachineTradeHooks.SuccessHook {

    @Persisted
    private List<MachineTradeHooks.SuccessHook> hooks;

    /**
     * 便捷构造器
     */
    public CompositeSuccessHook(MachineTradeHooks.SuccessHook... hooks) {
        this.hooks = List.of(hooks);
    }

    @Override
    public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                             MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {
        hooks.forEach(hook -> hook.afterSuccess(context, request, plan, result));
    }
}

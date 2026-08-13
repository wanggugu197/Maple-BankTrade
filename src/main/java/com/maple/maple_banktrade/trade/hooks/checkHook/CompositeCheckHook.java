package com.maple.maple_banktrade.trade.hooks.checkHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 组合检查钩子：当至少 {@link #requiredCount} 个子钩子返回 true 时，整体返回 true（即检查通过）。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CompositeCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private List<MachineTradeHooks.CheckHook> hooks;

    @Persisted
    private int requiredCount;

    /**
     * 便捷构造器
     */
    public CompositeCheckHook(int requiredCount, MachineTradeHooks.CheckHook... hooks) {
        this.requiredCount = requiredCount;
        this.hooks = List.of(hooks);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        if (hooks.isEmpty()) return true;
        int satisfied = 0;
        for (MachineTradeHooks.CheckHook hook : hooks) {
            if (hook != null && hook.check(context, request, trade)) {
                satisfied++;
                if (satisfied >= requiredCount) return true;
            }
        }
        return false;
    }
}

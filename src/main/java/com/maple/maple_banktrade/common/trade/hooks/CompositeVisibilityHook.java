package com.maple.maple_banktrade.common.trade.hooks;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 组合可见性钩子：当至少 {@link #requiredCount} 个子钩子返回 true 时，整体返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CompositeVisibilityHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private List<MachineTradeHooks.VisibilityHook> hooks;
    @Persisted
    private int requiredCount;

    /**
     * 便捷构造器
     */
    public CompositeVisibilityHook(int requiredCount, MachineTradeHooks.VisibilityHook... hooks) {
        this.requiredCount = requiredCount;
        this.hooks = List.of(hooks);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        if (hooks.isEmpty()) return true;
        int satisfied = 0;
        for (MachineTradeHooks.VisibilityHook hook : hooks) {
            if (hook != null && hook.isVisible(context, trade)) {
                satisfied++;
                if (satisfied >= requiredCount) return true;
            }
        }
        return false;
    }
}

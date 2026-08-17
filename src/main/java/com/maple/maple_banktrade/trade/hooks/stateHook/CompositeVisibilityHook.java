package com.maple.maple_banktrade.trade.hooks.stateHook;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 组合状态钩子：当至少 {@link #requiredCount} 个子钩子可见时整体可见。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CompositeVisibilityHook extends MachineTradeHooks.StateHook {

    @Persisted
    private List<MachineTradeHooks.StateHook> hooks;
    @Persisted
    private int requiredCount;

    /**
     * 便捷构造器
     */
    public CompositeVisibilityHook(int requiredCount, MachineTradeHooks.StateHook... hooks) {
        this.requiredCount = requiredCount;
        this.hooks = List.of(hooks);
    }

    // CompositeVisibilityHook.java
    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        if (requiredCount <= 0 || hooks == null || hooks.isEmpty()) {
            return FLAG_VISIBLE; // 无条件则完全可见
        }

        int visibleCount = 0;
        int mergedFlags = 0; // 聚合所有子钩子的状态位（包括完成位）

        for (MachineTradeHooks.StateHook hook : hooks) {
            if (hook == null) continue;
            int childState = hook.getState(context, trade);
            // 统计可见子钩子数量
            if ((childState & FLAG_VISIBLE) != 0) {
                visibleCount++;
            }
            // 合并所有状态位（OR）
            mergedFlags |= childState;
        }

        // 最终可见性：满足要求的子钩子数量
        if (visibleCount >= requiredCount) {
            // 确保可见位为1，同时保留所有聚合的额外位（包括完成位）
            return mergedFlags | FLAG_VISIBLE;
        } else {
            // 不可见：清除可见位，但保留其他状态位（例如完成位）
            return mergedFlags & ~FLAG_VISIBLE;
        }
    }
}

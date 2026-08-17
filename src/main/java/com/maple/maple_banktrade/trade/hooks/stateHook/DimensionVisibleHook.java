package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 所在维度钩子：当所在维度为 {@link #targetDimension} 时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class DimensionVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier targetDimension;
    @Persisted
    private boolean flip;

    public DimensionVisibleHook(Identifier targetDimension) {
        this.targetDimension = targetDimension;
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        boolean condition = context.level().dimension().identifier().equals(targetDimension);
        return (flip != condition) ? FLAG_VISIBLE : 0;
    }
}

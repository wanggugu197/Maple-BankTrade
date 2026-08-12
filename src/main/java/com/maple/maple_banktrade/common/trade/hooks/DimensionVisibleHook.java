package com.maple.maple_banktrade.common.trade.hooks;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class DimensionVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier targetDimension;

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        return context.level().dimension().identifier().equals(targetDimension);
    }
}

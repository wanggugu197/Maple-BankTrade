package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CardExistenceVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    Identifier cardNameIndex;

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        return context.bankCards().stream().anyMatch(card -> cardNameIndex.equals(card.getNameIndex()));
    }
}

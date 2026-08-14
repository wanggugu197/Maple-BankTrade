package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 银行卡存在性钩子：当 {@link #cardNameIndex} 存在于上下文时返回 true
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CardExistenceCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    Identifier cardNameIndex;

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        return context.bankCards().stream().anyMatch(card -> cardNameIndex.equals(card.getNameIndex()));
    }
}

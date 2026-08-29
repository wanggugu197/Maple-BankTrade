package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 银行卡存在性钩子：当 {@link #cardNameIndex} 存在于上下文时可见。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CardExistenceVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    ResourceLocation cardNameIndex;

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        boolean condition = context.bankCards().stream().anyMatch(card -> cardNameIndex.equals(card.getNameIndex()));
        return condition ? FLAG_VISIBLE : 0;
    }
}

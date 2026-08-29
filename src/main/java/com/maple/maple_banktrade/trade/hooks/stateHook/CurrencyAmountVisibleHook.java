package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.math.BigInteger;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 货币额度钩子：当货币 {@link #currencyTypeId} 量大于 {@link #amount} 时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CurrencyAmountVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private ResourceLocation currencyTypeId;
    @Persisted
    private BigInteger amount;
    @Persisted
    private boolean flip;

    public CurrencyAmountVisibleHook(ResourceLocation currencyTypeId, BigInteger amount) {
        this.currencyTypeId = currencyTypeId;
        this.amount = amount;
        this.flip = false;
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        boolean condition = context.bankCards().stream().anyMatch(card -> {
            if (card instanceof CurrencyStorageBankCard currencyCard) {
                return flip != currencyCard.getCurrencyBalance(currencyTypeId).compareTo(amount) > 0;
            }
            return false;
        });
        return condition ? FLAG_VISIBLE : 0;
    }
}

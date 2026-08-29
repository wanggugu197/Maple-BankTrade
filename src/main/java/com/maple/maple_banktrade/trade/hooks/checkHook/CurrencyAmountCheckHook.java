package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.math.BigInteger;

/**
 * 货币额度钩子：当货币 {@link #currencyTypeId} 量大于 {@link #amount} 时返回 true
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class CurrencyAmountCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private ResourceLocation currencyTypeId;
    @Persisted
    private BigInteger amount;
    @Persisted
    private boolean flip;

    public CurrencyAmountCheckHook(ResourceLocation currencyTypeId, BigInteger amount) {
        this.currencyTypeId = currencyTypeId;
        this.amount = amount;
        this.flip = false;
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        return context.bankCards().stream().anyMatch(card -> {
            if (card instanceof CurrencyStorageBankCard currencyCard) {
                return flip != currencyCard.getCurrencyBalance(currencyTypeId).compareTo(amount) > 0;
            }
            return false;
        });
    }
}

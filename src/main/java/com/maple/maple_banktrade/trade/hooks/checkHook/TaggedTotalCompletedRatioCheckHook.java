package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 标记卡总完成比例钩子：当已完成比例（0~100） ≥ {@link #percent} 时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedTotalCompletedRatioCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private int percent;
    @Persisted
    private boolean flip;

    /**
     * 便捷构造（flip = false）
     */
    public TaggedTotalCompletedRatioCheckHook(Identifier nameIndex, int percent) {
        this(nameIndex, percent, false);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard tagged) {
            boolean condition = tagged.getCompletionRatio() >= percent;
            return flip != condition;
        }
        return flip;
    }
}

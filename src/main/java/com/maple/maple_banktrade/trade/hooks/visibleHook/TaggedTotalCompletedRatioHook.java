package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 标记卡总完成比例钩子：当已完成比例（0~100） ≥ {@link #percent} 时返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedTotalCompletedRatioHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private int percent;
    @Persisted
    private boolean flip;

    /**
     * 便捷构造（flip = false）
     */
    public TaggedTotalCompletedRatioHook(Identifier nameIndex, int percent) {
        this(nameIndex, percent, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
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

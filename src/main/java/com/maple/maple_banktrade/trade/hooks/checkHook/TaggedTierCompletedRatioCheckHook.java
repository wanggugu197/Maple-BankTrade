package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.resources.ResourceLocation;

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
 * 标记卡某等级完成比例钩子：当指定 tier 下已完成比例（0~100） ≥ {@link #percent} 时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedTierCompletedRatioCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private ResourceLocation nameIndex;
    @Persisted
    private short tier;
    @Persisted
    private int percent;
    @Persisted
    private boolean flip;

    /**
     * 便捷构造（flip = false）
     */
    public TaggedTierCompletedRatioCheckHook(ResourceLocation nameIndex, short tier, int percent) {
        this(nameIndex, tier, percent, false);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard tagged) {
            boolean condition = tagged.getCompletionRatioByTier(tier) >= percent;
            return flip != condition;
        }
        return flip;
    }
}

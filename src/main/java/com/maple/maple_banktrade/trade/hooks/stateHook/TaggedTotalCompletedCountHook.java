package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 标记卡总完成数量钩子：当已完成条目数 ≥ {@link #threshold} 时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedTotalCompletedCountHook extends MachineTradeHooks.StateHook {

    @Persisted
    private ResourceLocation nameIndex;
    @Persisted
    private int threshold;
    @Persisted
    private boolean flip;

    /**
     * 便捷构造（flip = false）
     */
    public TaggedTotalCompletedCountHook(ResourceLocation nameIndex, int threshold) {
        this(nameIndex, threshold, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard tagged) {
            boolean condition = tagged.getCompletedEntries() >= threshold;
            return (flip != condition) ? FLAG_VISIBLE : 0;
        }
        return flip ? FLAG_VISIBLE : 0;
    }
}

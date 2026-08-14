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
 * 标记完成钩子：当标记卡 {@link #nameIndex} 中的 {@link #id} 完成时返回 true
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedCompletedVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private String id;
    @Persisted
    private boolean flip;

    public TaggedCompletedVisibleHook(Identifier nameIndex, String id) {
        this.nameIndex = nameIndex;
        this.id = id;
        this.flip = false;
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            return flip != taggedCard.isComplete(id);
        }
        return flip;
    }
}

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
 * 标记完成钩子：当标记卡 {@link #nameIndex} 中的 {@link #id} 完成时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedCompletedVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private ResourceLocation nameIndex;
    @Persisted
    private String id;
    @Persisted
    private boolean flip;

    public TaggedCompletedVisibleHook(ResourceLocation nameIndex, String id) {
        this.nameIndex = nameIndex;
        this.id = id;
        this.flip = false;
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            return (flip != taggedCard.isComplete(id)) ? FLAG_VISIBLE : 0;
        }
        return flip ? FLAG_VISIBLE : 0;
    }
}

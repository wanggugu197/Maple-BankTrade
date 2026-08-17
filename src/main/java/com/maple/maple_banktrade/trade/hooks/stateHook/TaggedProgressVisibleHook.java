package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.Identifier;

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
 * 标记进度钩子：当标记卡 {@link #nameIndex} 中的 {@link #id} 进度大于 {@link #progress} 时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedProgressVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private String id;
    @Persisted
    private int progress;
    @Persisted
    private boolean flip;

    public TaggedProgressVisibleHook(Identifier nameIndex, String id, int progress) {
        this.nameIndex = nameIndex;
        this.id = id;
        this.progress = progress;
        this.flip = false;
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            return (flip != (taggedCard.getProgress(id) >= progress)) ? FLAG_VISIBLE : 0;
        }
        return flip ? FLAG_VISIBLE : 0;
    }
}

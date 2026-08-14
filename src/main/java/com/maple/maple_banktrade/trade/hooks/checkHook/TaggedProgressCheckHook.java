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
 * 标记进度钩子：当标记卡 {@link #nameIndex} 中的 {@link #id} 进度大于 {@link #progress} 时返回 true
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedProgressCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private String id;
    @Persisted
    private int progress;
    @Persisted
    private boolean flip;

    public TaggedProgressCheckHook(Identifier nameIndex, String id, int progress) {
        this.nameIndex = nameIndex;
        this.id = id;
        this.progress = progress;
        this.flip = false;
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            return flip != taggedCard.getProgress(id) >= progress;
        }
        return flip;
    }
}

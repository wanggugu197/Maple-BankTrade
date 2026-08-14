package com.maple.maple_banktrade.trade.hooks.successHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.*;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * 标记进度钩子：将标记卡 {@link #nameIndex} 中的 {@link #id} 进度增加 1
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedIncreaseSuccessHook extends MachineTradeHooks.SuccessHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private String id;

    @Override
    public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                             MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card == null) return;
        UUID uuid = card.getCardUuid();
        MBTBankStates.modifyBankCards(context.server(), data -> {
            if (data.getCard(uuid) instanceof TaggedBankCard taggedCard) {
                MachineTradeDetail detail = result.detail();
                if (detail != null)
                    taggedCard.addProgress(id, detail.tradeCount());
            }
        });
    }
}

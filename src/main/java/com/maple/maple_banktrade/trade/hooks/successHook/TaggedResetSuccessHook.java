package com.maple.maple_banktrade.trade.hooks.successHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeDetail;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradePlan;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * 标记进度重置钩子：交易成功后，将标记卡 {@link #nameIndex} 中的 {@link #id} 进度重置为 0
 * （条目若原本无进度则无操作）。可用于“循环任务”：完成某条件后清空进度重新开始。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedResetSuccessHook extends MachineTradeHooks.SuccessHook {

    @Persisted
    private ResourceLocation nameIndex;
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
                taggedCard.resetProgress(id);
            }
        });
    }
}

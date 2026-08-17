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

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_COMPLETED;

/**
 * 仅检查完成状态，不控制可见性。
 * 当卡片存在且对应任务完成时设置 FLAG_COMPLETED。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class TaggedCompletedStateHook extends MachineTradeHooks.StateHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private String id;
    @Persisted
    private boolean flip; // 反转逻辑（可选）

    public TaggedCompletedStateHook(Identifier nameIndex, String id) {
        this(nameIndex, id, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard tagged) {
            boolean completed = tagged.isComplete(id);
            return (flip != completed) ? FLAG_COMPLETED : 0;
        }
        return flip ? FLAG_COMPLETED : 0;
    }
}

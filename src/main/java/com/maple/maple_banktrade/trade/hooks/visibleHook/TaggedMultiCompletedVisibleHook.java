package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 标记卡多条目完成钩子：当标记卡 {@link #nameIndex} 中的 {@link #ids} 全部完成时返回 true。
 * {@link #flip} 用于反转逻辑
 */
public final class TaggedMultiCompletedVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier nameIndex;
    @Persisted
    private Set<String> ids;
    @Persisted
    private boolean flip;

    public TaggedMultiCompletedVisibleHook(Identifier nameIndex, Set<String> ids) {
        this(nameIndex, ids, false);
    }

    public TaggedMultiCompletedVisibleHook(Identifier nameIndex, Set<String> ids, boolean flip) {
        this.nameIndex = Objects.requireNonNull(nameIndex, "nameIndex");
        // 必须保持可变集合：LDLib2 CollectionAccessor 反序列化时会 clear + add
        this.ids = ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
        this.flip = flip;
    }

    public TaggedMultiCompletedVisibleHook(Identifier nameIndex, String... ids) {
        this(nameIndex, ids == null ? Set.of() : Set.of(ids));
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            boolean condition = ids.stream().allMatch(taggedCard::isComplete);
            return flip != condition;
        }
        return flip;
    }
}

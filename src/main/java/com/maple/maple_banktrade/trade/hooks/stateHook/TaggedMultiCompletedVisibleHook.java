package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 标记卡多条目完成钩子：当标记卡 {@link #nameIndex} 中的 {@link #ids} 全部完成时可见。
 * {@link #flip} 用于反转逻辑
 */
public final class TaggedMultiCompletedVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private ResourceLocation nameIndex;
    @Persisted
    private Set<String> ids;
    @Persisted
    private boolean flip;

    public TaggedMultiCompletedVisibleHook(ResourceLocation nameIndex, Set<String> ids) {
        this(nameIndex, ids, false);
    }

    public TaggedMultiCompletedVisibleHook(ResourceLocation nameIndex, Set<String> ids, boolean flip) {
        this.nameIndex = Objects.requireNonNull(nameIndex, "nameIndex");
        // 必须保持可变集合：LDLib2 CollectionAccessor 反序列化时会 clear + add
        this.ids = ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
        this.flip = flip;
    }

    public TaggedMultiCompletedVisibleHook(ResourceLocation nameIndex, String... ids) {
        this(nameIndex, ids == null ? Set.of() : Set.of(ids));
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BankCard card = context.bankCards().stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);
        if (card instanceof TaggedBankCard taggedCard) {
            boolean condition = ids.stream().allMatch(taggedCard::isComplete);
            return (flip != condition) ? FLAG_VISIBLE : 0;
        }
        return flip ? FLAG_VISIBLE : 0;
    }
}

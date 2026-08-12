package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

public class TaggedBankCard extends BankCard {

    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("tagged");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<TaggedBankCard> CODEC = PersistedParser.createCodec(TaggedBankCard::new);
    public static final MapCodec<TaggedBankCard> MAP_CODEC = PersistedParser.createMapCodec(TaggedBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private Set<Identifier> tags;

    // ==============================================
    // 构造
    // ==============================================

    public TaggedBankCard() {
        this.tags = new HashSet<>();
    }

    public TaggedBankCard(BankCardIdentity identity) {
        this(identity, Set.of());
    }

    protected TaggedBankCard(BankCardIdentity identity, Set<Identifier> tags) {
        this(identity, CARD_TYPE_ID, tags);
    }

    protected TaggedBankCard(BankCardIdentity identity, Identifier cardTypeId, Set<Identifier> tags) {
        super(identity, cardTypeId);
        this.tags = new HashSet<>(tags);
    }

    // ==============================================
    // 修改方法
    // ==============================================

    public boolean addTag(Identifier tag) {
        return tag != null && tags.add(tag);
    }

    public boolean removeTag(Identifier tag) {
        return tag != null && tags.remove(tag);
    }
}

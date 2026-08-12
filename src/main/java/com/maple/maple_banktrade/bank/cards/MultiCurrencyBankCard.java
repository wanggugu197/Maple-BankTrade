package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public class MultiCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("multi_currency");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<MultiCurrencyBankCard> CODEC = PersistedParser.createCodec(MultiCurrencyBankCard::new);
    public static final MapCodec<MultiCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(MultiCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private Map<Identifier, Long> balances;

    // ==============================================
    // 构造
    // ==============================================

    public MultiCurrencyBankCard() {
        this.balances = new LinkedHashMap<>();
    }

    public MultiCurrencyBankCard(BankCardIdentity identity, CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(0L, currencyTypes));
    }

    public MultiCurrencyBankCard(BankCardIdentity identity, Collection<CurrencyType> currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, 0L));
    }

    public MultiCurrencyBankCard(BankCardIdentity identity, Map<Identifier, Long> balances) {
        this(identity, CARD_TYPE_ID, balances);
    }

    protected MultiCurrencyBankCard(BankCardIdentity identity, Identifier cardTypeId,
                                    Map<Identifier, Long> balances) {
        super(identity, cardTypeId);
        this.balances = new LinkedHashMap<>();
        balances.forEach((type, amount) -> {
            Identifier currencyId = normalizeCurrencyId(type);
            if (currencyId != null) {
                this.balances.put(currencyId, normalizeLongAmount(amount));
            }
        });
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Collections.unmodifiableSet(balances.keySet());
    }

    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        return currencyId == null ? 0L : balances.getOrDefault(currencyId, 0L);
    }

    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (amount <= 0 || currencyId == null || !balances.containsKey(currencyId)) return false;
        long current = balances.getOrDefault(currencyId, 0L);
        if (Long.MAX_VALUE - current < amount) return false;
        balances.put(currencyId, current + amount);
        return true;
    }

    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (amount <= 0 || currencyId == null || !balances.containsKey(currencyId)) return false;
        long current = balances.getOrDefault(currencyId, 0L);
        if (current < amount) return false;
        balances.put(currencyId, current - amount);
        return true;
    }

    // ==============================================
    // 工具
    // ==============================================

    protected static Identifier normalizeCurrencyId(Identifier typeId) {
        CurrencyType type = CurrencyType.requireById(typeId);
        return type == null ? null : type.id();
    }

    protected static long normalizeLongAmount(long amount) {
        return Math.max(0L, amount);
    }
}

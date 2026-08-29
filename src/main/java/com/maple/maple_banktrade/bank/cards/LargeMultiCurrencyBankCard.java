package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.ResourceLocation;

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

import java.math.BigInteger;
import java.util.*;

public class LargeMultiCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    public static final ResourceLocation CARD_TYPE_ID = MapleBankTrade.id("large_multi_currency");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<LargeMultiCurrencyBankCard> CODEC = PersistedParser.createCodec(LargeMultiCurrencyBankCard::new);
    public static final MapCodec<LargeMultiCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(LargeMultiCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private Map<ResourceLocation, BigInteger> balances;

    // ==============================================
    // 构造
    // ==============================================

    public LargeMultiCurrencyBankCard() {
        this.balances = new LinkedHashMap<>();
    }

    public LargeMultiCurrencyBankCard(BankCardIdentity identity, CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(BigInteger.ZERO, currencyTypes));
    }

    public LargeMultiCurrencyBankCard(BankCardIdentity identity, Collection<CurrencyType> currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, BigInteger.ZERO));
    }

    public LargeMultiCurrencyBankCard(BankCardIdentity identity, Map<ResourceLocation, BigInteger> balances) {
        this(identity, CARD_TYPE_ID, balances);
    }

    protected LargeMultiCurrencyBankCard(BankCardIdentity identity, ResourceLocation cardTypeId,
                                         Map<ResourceLocation, BigInteger> balances) {
        super(identity, cardTypeId);
        this.balances = new LinkedHashMap<>();
        balances.forEach((type, amount) -> {
            ResourceLocation currencyId = normalizeCurrencyId(type);
            if (currencyId != null) {
                this.balances.put(currencyId, normalizeBigAmount(amount));
            }
        });
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    @Override
    public Set<ResourceLocation> getSupportedCurrencyIds() {
        return Collections.unmodifiableSet(balances.keySet());
    }

    @Override
    public BigInteger getCurrencyBalance(ResourceLocation currencyTypeId) {
        ResourceLocation currencyId = normalizeCurrencyId(currencyTypeId);
        return currencyId == null ? BigInteger.ZERO : balances.getOrDefault(currencyId, BigInteger.ZERO);
    }

    @Override
    public long getCurrencyBalanceAsLong(ResourceLocation currencyTypeId) {
        return CurrencyStorageBankCard.saturatingLongValue(getCurrencyBalance(currencyTypeId));
    }

    @Override
    public boolean increaseCurrency(ResourceLocation currencyTypeId, BigInteger amount) {
        if (amount.signum() <= 0) return false;
        ResourceLocation currencyId = normalizeCurrencyId(currencyTypeId);
        if (currencyId == null || !balances.containsKey(currencyId)) return false;
        balances.computeIfPresent(currencyId, (ignored, cur) -> cur.add(amount));
        return true;
    }

    @Override
    public boolean increaseCurrency(ResourceLocation currencyTypeId, long amount) {
        return increaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    @Override
    public boolean decreaseCurrency(ResourceLocation currencyTypeId, BigInteger amount) {
        if (amount.signum() <= 0) return false;
        ResourceLocation currencyId = normalizeCurrencyId(currencyTypeId);
        if (currencyId == null || !balances.containsKey(currencyId)) return false;
        BigInteger current = balances.get(currencyId);
        if (current.compareTo(amount) < 0) return false;
        balances.put(currencyId, current.subtract(amount));
        return true;
    }

    @Override
    public boolean decreaseCurrency(ResourceLocation currencyTypeId, long amount) {
        return decreaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    // ==============================================
    // 工具
    // ==============================================

    protected static ResourceLocation normalizeCurrencyId(ResourceLocation typeId) {
        CurrencyType type = CurrencyType.requireById(typeId);
        return type == null ? null : type.id();
    }

    protected static BigInteger normalizeBigAmount(BigInteger amount) {
        return amount == null || amount.signum() < 0 ? BigInteger.ZERO : amount;
    }
}

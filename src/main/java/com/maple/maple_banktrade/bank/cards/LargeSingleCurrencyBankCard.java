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
import java.util.Objects;
import java.util.Set;

public class LargeSingleCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    public static final ResourceLocation CARD_TYPE_ID = MapleBankTrade.id("large_single_currency");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<LargeSingleCurrencyBankCard> CODEC = PersistedParser.createCodec(LargeSingleCurrencyBankCard::new);
    public static final MapCodec<LargeSingleCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(LargeSingleCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private ResourceLocation currencyTypeId;

    @Persisted
    @Getter
    @Setter
    private BigInteger balance;

    public CurrencyType getCurrencyType() {
        return CurrencyType.requireById(currencyTypeId);
    }

    // ==============================================
    // 构造
    // ==============================================

    public LargeSingleCurrencyBankCard() {}

    public LargeSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType) {
        this(identity, currencyType, BigInteger.ZERO);
    }

    public LargeSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, BigInteger balance) {
        this(identity, CARD_TYPE_ID, currencyType, balance);
    }

    protected LargeSingleCurrencyBankCard(BankCardIdentity identity, ResourceLocation cardTypeId,
                                          CurrencyType currencyType, BigInteger balance) {
        super(identity, cardTypeId);
        this.currencyTypeId = Objects.requireNonNull(currencyType, "currencyType").id();
        this.balance = normalizeBigAmount(balance);
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    @Override
    public Set<ResourceLocation> getSupportedCurrencyIds() {
        return Set.of(currencyTypeId);
    }

    @Override
    public BigInteger getCurrencyBalance(ResourceLocation currencyTypeId) {
        return supportsCurrency(currencyTypeId) ? balance : BigInteger.ZERO;
    }

    @Override
    public long getCurrencyBalanceAsLong(ResourceLocation currencyTypeId) {
        return CurrencyStorageBankCard.saturatingLongValue(getCurrencyBalance(currencyTypeId));
    }

    @Override
    public boolean increaseCurrency(ResourceLocation currencyTypeId, BigInteger amount) {
        if (amount.signum() <= 0 || !supportsCurrency(currencyTypeId)) return false;
        balance = balance.add(amount);
        return true;
    }

    @Override
    public boolean increaseCurrency(ResourceLocation currencyTypeId, long amount) {
        return increaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    @Override
    public boolean decreaseCurrency(ResourceLocation currencyTypeId, BigInteger amount) {
        if (amount.signum() <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (balance.compareTo(amount) < 0) return false;
        balance = balance.subtract(amount);
        return true;
    }

    @Override
    public boolean decreaseCurrency(ResourceLocation currencyTypeId, long amount) {
        return decreaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    // ==============================================
    // 工具
    // ==============================================

    protected static BigInteger normalizeBigAmount(BigInteger amount) {
        return amount == null || amount.signum() < 0 ? BigInteger.ZERO : amount;
    }
}

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

import java.util.Objects;
import java.util.Set;

/**
 * 单货币银行卡，保存一种货币和 long 余额。
 */
public class SingleCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("single_currency");

    // ==============================================
    // Codec（由 PersistedParser 生成）
    // ==============================================

    public static final Codec<SingleCurrencyBankCard> CODEC = PersistedParser.createCodec(SingleCurrencyBankCard::new);
    public static final MapCodec<SingleCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(SingleCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private Identifier currencyTypeId;

    @Persisted
    @Getter
    @Setter
    private long balance;

    public CurrencyType getCurrencyType() {
        return CurrencyType.requireById(currencyTypeId);
    }

    // ==============================================
    // 构造
    // ==============================================

    /** 无参构造（反序列化） */
    public SingleCurrencyBankCard() {}

    public SingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType) {
        this(identity, currencyType, 0L);
    }

    public SingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, long balance) {
        this(identity, CARD_TYPE_ID, currencyType, balance);
    }

    protected SingleCurrencyBankCard(BankCardIdentity identity, Identifier cardTypeId,
                                     CurrencyType currencyType, long balance) {
        super(identity, cardTypeId);
        this.currencyTypeId = Objects.requireNonNull(currencyType, "currencyType").id();
        this.balance = normalizeLongAmount(balance);
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Set.of(currencyTypeId);
    }

    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        return supportsCurrency(currencyTypeId) ? balance : 0L;
    }

    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        if (amount <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (Long.MAX_VALUE - balance < amount) return false;
        balance += amount;
        return true;
    }

    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        if (amount <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    // ==============================================
    // 工具
    // ==============================================

    protected static long normalizeLongAmount(long amount) {
        return Math.max(0L, amount);
    }
}

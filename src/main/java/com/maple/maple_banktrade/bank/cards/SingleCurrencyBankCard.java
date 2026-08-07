package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.Objects;
import java.util.Set;

/**
 * 单货币银行卡，保存一种货币和 long 余额。
 */
public class SingleCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 单货币银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("single_currency");

    // ==============================================
    // Codec
    // ==============================================

    /** 单货币银行卡序列化编解码器。 */
    public static final MapCodec<SingleCurrencyBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            CurrencyType.CODEC.fieldOf("currency").forGetter(SingleCurrencyBankCard::getCurrencyType),
            Codec.LONG.fieldOf("balance").forGetter(SingleCurrencyBankCard::getBalance))
            .apply(instance, SingleCurrencyBankCard::new));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Identifier currencyTypeId;
    @Getter
    private long balance;

    /** 获取本卡唯一支持的货币类型。 */
    public CurrencyType getCurrencyType() {
        return CurrencyType.requireById(currencyTypeId);
    }

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行、名称索引和货币的单货币卡。 */
    public SingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType) {
        this(identity, currencyType, 0L);
    }

    public SingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, long balance) {
        this(identity, CARD_TYPE_ID, currencyType, balance);
    }

    /** 从存档字段恢复单货币卡。 */
    protected SingleCurrencyBankCard(BankCardIdentity identity, Identifier cardTypeId, CurrencyType currencyType, long balance) {
        super(identity, cardTypeId);
        this.currencyTypeId = Objects.requireNonNull(currencyType, "currencyType").id();
        this.balance = normalizeLongAmount(balance);
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    /** 返回本卡唯一支持的货币。 */
    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Set.of(currencyTypeId);
    }

    /** 查询 long 余额；货币不匹配返回 0。 */
    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        return supportsCurrency(currencyTypeId) ? balance : 0L;
    }

    /** 增加 long 余额。 */
    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        if (amount <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (Long.MAX_VALUE - balance < amount) return false;
        balance += amount;
        return true;
    }

    /** 减少 long 余额。 */
    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        if (amount <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    // ==============================================
    // 工具方法
    // ==============================================

    /** 将 long 金额规范化为非负值。 */
    protected static long normalizeLongAmount(long amount) {
        return Math.max(0L, amount);
    }
}

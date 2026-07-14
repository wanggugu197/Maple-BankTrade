package com.maple.maple_banktrade.bank.cards;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

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
    public static final Codec<SingleCurrencyBankCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("card_uuid").forGetter(SingleCurrencyBankCard::getCardUuid),
            Identifier.CODEC.fieldOf("bank_type").forGetter(SingleCurrencyBankCard::getBankTypeId),
            Identifier.CODEC.fieldOf("card_type").forGetter(SingleCurrencyBankCard::getCardTypeId),
            Identifier.CODEC.fieldOf("name_index").forGetter(SingleCurrencyBankCard::getNameIndex),
            CurrencyType.ID_CODEC.fieldOf("currency").forGetter(SingleCurrencyBankCard::getCurrencyTypeId),
            Codec.LONG.fieldOf("balance").forGetter(SingleCurrencyBankCard::getBalance))
            .apply(instance, (cardUuid, bankTypeId, ignoredCardTypeId, nameIndex, currencyTypeId, balance) -> new SingleCurrencyBankCard(cardUuid, bankTypeId, nameIndex, currencyTypeId, balance)));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Identifier currencyTypeId;
    @Getter
    private long balance;

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行、名称索引和货币的单货币卡。 */
    public SingleCurrencyBankCard(UUID cardUuid, BankType bankType, Identifier nameIndex, CurrencyType currencyType) {
        this(cardUuid, bankType.id(), nameIndex, currencyType.id(), 0L);
    }

    /** 从存档字段恢复单货币卡。 */
    protected SingleCurrencyBankCard(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex, Identifier currencyTypeId, long balance) {
        this(cardUuid, bankTypeId, CARD_TYPE_ID, nameIndex, currencyTypeId, balance);
    }

    /** 从存档字段恢复单货币卡，并允许子类指定自己的银行卡类型。 */
    protected SingleCurrencyBankCard(UUID cardUuid, Identifier bankTypeId, Identifier cardTypeId, Identifier nameIndex, Identifier currencyTypeId, long balance) {
        super(cardUuid, bankTypeId, cardTypeId, nameIndex);
        this.currencyTypeId = normalizeCurrencyId(currencyTypeId);
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

    /** 规范化货币 ID，未知货币视为非法。 */
    protected static Identifier normalizeCurrencyId(Identifier typeId) {
        CurrencyType type = CurrencyType.requireById(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown currency type: " + typeId);
        }
        return type.id();
    }

    /** 将 long 金额规范化为非负值。 */
    protected static long normalizeLongAmount(long amount) {
        return Math.max(0L, amount);
    }
}

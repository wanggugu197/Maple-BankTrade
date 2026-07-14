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

import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 单货币大额银行卡，保存一种货币和 BigInteger 余额。
 */
public class LargeSingleCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 单货币大额银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("large_single_currency");

    // ==============================================
    // Codec
    // ==============================================

    /** BigInteger 使用字符串形式保存，避免数值范围受 long 限制。 */
    private static final Codec<BigInteger> BIG_INTEGER_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return com.mojang.serialization.DataResult.success(new BigInteger(value));
        } catch (NumberFormatException e) {
            return com.mojang.serialization.DataResult.error(() -> "Invalid BigInteger: " + value);
        }
    }, BigInteger::toString);

    /** 单货币大额银行卡序列化编解码器。 */
    public static final Codec<LargeSingleCurrencyBankCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("card_uuid").forGetter(LargeSingleCurrencyBankCard::getCardUuid),
            Identifier.CODEC.fieldOf("bank_type").forGetter(LargeSingleCurrencyBankCard::getBankTypeId),
            Identifier.CODEC.fieldOf("card_type").forGetter(LargeSingleCurrencyBankCard::getCardTypeId),
            Identifier.CODEC.fieldOf("name_index").forGetter(LargeSingleCurrencyBankCard::getNameIndex),
            CurrencyType.ID_CODEC.fieldOf("currency").forGetter(LargeSingleCurrencyBankCard::getCurrencyTypeId),
            BIG_INTEGER_CODEC.fieldOf("balance").forGetter(LargeSingleCurrencyBankCard::getBalance))
            .apply(instance, (cardUuid, bankTypeId, ignoredCardTypeId, nameIndex, currencyTypeId, balance) -> new LargeSingleCurrencyBankCard(cardUuid, bankTypeId, nameIndex, currencyTypeId, balance)));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Identifier currencyTypeId;
    @Getter
    private BigInteger balance;

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行、名称索引和货币的大额单货币卡。 */
    public LargeSingleCurrencyBankCard(UUID cardUuid, BankType bankType, Identifier nameIndex, CurrencyType currencyType) {
        this(cardUuid, bankType.id(), nameIndex, currencyType.id(), BigInteger.ZERO);
    }

    /** 从存档字段恢复大额单货币卡。 */
    protected LargeSingleCurrencyBankCard(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex, Identifier currencyTypeId, BigInteger balance) {
        super(cardUuid, bankTypeId, CARD_TYPE_ID, nameIndex);
        this.currencyTypeId = normalizeCurrencyId(currencyTypeId);
        this.balance = normalizeBigAmount(balance);
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    /** 返回本卡唯一支持的货币。 */
    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Set.of(currencyTypeId);
    }

    /** 查询 BigInteger 余额。 */
    @Override
    public BigInteger getCurrencyBalance(Identifier currencyTypeId) {
        return supportsCurrency(currencyTypeId) ? balance : BigInteger.ZERO;
    }

    /** 查询 long 饱和余额。 */
    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        return CurrencyStorageBankCard.saturatingLongValue(getCurrencyBalance(currencyTypeId));
    }

    /** 增加 BigInteger 余额。 */
    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0 || !supportsCurrency(currencyTypeId)) return false;

        balance = balance.add(amount);
        return true;
    }

    /** 增加 long 余额。 */
    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        return increaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    /** 减少 BigInteger 余额。 */
    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0 || !supportsCurrency(currencyTypeId)) return false;
        if (balance.compareTo(amount) < 0) return false;

        balance = balance.subtract(amount);
        return true;
    }

    /** 减少 long 余额。 */
    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        return decreaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
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

    /** 将 BigInteger 金额规范化为非负值。 */
    protected static BigInteger normalizeBigAmount(BigInteger amount) {
        return amount == null || amount.signum() < 0 ? BigInteger.ZERO : amount;
    }
}

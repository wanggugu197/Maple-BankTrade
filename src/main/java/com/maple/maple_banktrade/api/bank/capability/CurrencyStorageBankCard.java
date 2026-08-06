package com.maple.maple_banktrade.api.bank.capability;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mapleutillib.utils.FormattingUtil;

import java.math.BigInteger;
import java.util.*;

/**
 * 支持货币余额读写的银行卡统一接口。
 */
public interface CurrencyStorageBankCard {

    /** 获取当前卡支持的货币 ID。 */
    Set<Identifier> getSupportedCurrencyIds();

    /** 判断当前卡是否支持指定货币。 */
    default boolean supportsCurrency(Identifier currencyTypeId) {
        CurrencyType type = CurrencyType.requireById(currencyTypeId);
        return type != null && getSupportedCurrencyIds().contains(type.id());
    }

    /** 查询指定货币余额；不支持时返回 0。 */
    default BigInteger getCurrencyBalance(Identifier currencyTypeId) {
        return BigInteger.valueOf(getCurrencyBalanceAsLong(currencyTypeId));
    }

    /** 查询指定货币余额并格式化为字符串。 */
    default String getCurrencyBalanceAsString(Identifier currencyTypeId) {
        return formatCurrencyAmount(getCurrencyBalance(currencyTypeId));
    }

    /** 查询 long 余额；超范围实现应返回饱和值。 */
    long getCurrencyBalanceAsLong(Identifier currencyTypeId);

    /** 增加余额；默认仅支持 long 范围内金额。 */
    default boolean increaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        return canRepresentAsLong(amount) && increaseCurrency(currencyTypeId, amount.longValue());
    }

    /** 增加指定货币余额。 */
    boolean increaseCurrency(Identifier currencyTypeId, long amount);

    /** 减少余额；默认仅支持 long 范围内金额。 */
    default boolean decreaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        return canRepresentAsLong(amount) && decreaseCurrency(currencyTypeId, amount.longValue());
    }

    /** 减少指定货币余额。 */
    boolean decreaseCurrency(Identifier currencyTypeId, long amount);

    /** 将金额转为 long，超出时饱和到边界。 */
    static long saturatingLongValue(BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) return Long.MAX_VALUE;
        if (amount.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) return Long.MIN_VALUE;
        return amount.longValue();
    }

    /** 判断金额是否可无损转为 long。 */
    static boolean canRepresentAsLong(BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        return amount.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0 && amount.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0;
    }

    /** 将货币金额格式化为普通数字或科学计数法。 */
    static String formatCurrencyAmount(BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        return canRepresentAsLong(amount) ? FormattingUtil.DECIMAL_FORMAT_0F.format(amount) : FormattingUtil.DECIMAL_FORMAT_SIC_2F.format(amount);
    }

    /** 创建固定货币键的初始余额表，跳过空货币。 */
    static <V> Map<Identifier, V> createInitialBalances(Collection<CurrencyType> currencyTypes, V initialValue) {
        Map<Identifier, V> result = new HashMap<>();
        if (currencyTypes != null) {
            for (CurrencyType type : currencyTypes) {
                if (type != null) result.put(type.id(), initialValue);
            }
        }
        return result;
    }

    /** 创建固定货币键的初始余额表，跳过空货币。 */
    @SafeVarargs
    static <V> Map<Identifier, V> createInitialBalances(V initialValue, CurrencyType... currencyTypes) {
        return createInitialBalances(currencyTypes == null ? List.of() : Arrays.asList(currencyTypes), initialValue);
    }
}

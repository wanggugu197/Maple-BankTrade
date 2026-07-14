package com.maple.maple_banktrade.trade.currency_item;

import com.maple.maple_banktrade.api.trade.context.TradePlan;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 货币-物品交易计划（check 组装，execute 提交）。
 */
public record CurrencyItemTradePlan(
                                    boolean executable,
                                    CurrencyItemTrade trade,
                                    BigInteger currencyAmount,
                                    int itemAmount,
                                    int tradeCount,
                                    boolean buy)
        implements TradePlan {

    /** 创建不可执行计划。 */
    public static CurrencyItemTradePlan denied() {
        return new CurrencyItemTradePlan(false, null, BigInteger.ZERO, 0, 0, false);
    }

    /** 创建可执行计划。 */
    public static CurrencyItemTradePlan of(CurrencyItemTrade trade, BigInteger currencyAmount, int itemAmount, int tradeCount, boolean buy) {
        Objects.requireNonNull(trade, "trade");
        Objects.requireNonNull(currencyAmount, "currencyAmount");
        return new CurrencyItemTradePlan(true, trade, currencyAmount, itemAmount, tradeCount, buy);
    }
}

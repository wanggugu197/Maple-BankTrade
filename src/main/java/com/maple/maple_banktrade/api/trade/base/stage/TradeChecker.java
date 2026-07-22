package com.maple.maple_banktrade.api.trade.base.stage;

import com.maple.maple_banktrade.api.trade.base.context.TradePlan;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeCheckResult;

/**
 * 交易检查阶段函数。
 *
 * @param <I> 输入类型
 * @param <P> 计划类型
 */
@FunctionalInterface
public interface TradeChecker<I extends TradeCheckInput<?, ?>, P extends TradePlan> {

    /** 预演交易并返回检查结果。 */
    TradeCheckResult<P> check(I input);
}

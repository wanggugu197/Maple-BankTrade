package com.maple.maple_banktrade.api.trade.base.stage;

import com.maple.maple_banktrade.api.trade.base.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;

/**
 * 交易执行阶段函数。
 *
 * @param <I> 输入类型
 * @param <R> 执行结果类型
 */
@FunctionalInterface
public interface TradeExecutor<I extends TradeExecuteInput<?, ?, ?>, R extends TradeExecuteResult<?>> {

    /** 按计划提交主交易。 */
    R execute(I input);
}

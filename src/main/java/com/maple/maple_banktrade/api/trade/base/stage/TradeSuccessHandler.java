package com.maple.maple_banktrade.api.trade.base.stage;

import com.maple.maple_banktrade.api.trade.base.input.TradeSuccessInput;

/**
 * 交易成功后处理函数（仅副作用，不否决主交易）。
 *
 * @param <I> 输入类型
 */
@FunctionalInterface
public interface TradeSuccessHandler<I extends TradeSuccessInput<?, ?, ?, ?>> {

    /** 主交易成功后执行额外处理。 */
    void afterSuccess(I input);
}

package com.maple.maple_banktrade.api.trade.base.input;

import com.maple.maple_banktrade.api.trade.base.context.TradeContext;
import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;

import java.util.Objects;

/**
 * 交易检查阶段输入。
 *
 * @param <C> 上下文类型
 * @param <R> 请求类型
 */
public interface TradeCheckInput<C extends TradeContext, R extends TradeRequest> {

    /** 获取交易上下文。 */
    C context();

    /** 获取交易请求。 */
    R request();

    /** 创建检查阶段输入。 */
    static <C extends TradeContext, R extends TradeRequest> Basic<C, R> of(C context, R request) {
        return new Basic<>(context, request);
    }

    /**
     * 通用检查阶段输入实现。
     */
    record Basic<C extends TradeContext, R extends TradeRequest>(C context, R request) implements TradeCheckInput<C, R> {

        public Basic {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(request, "request");
        }
    }
}

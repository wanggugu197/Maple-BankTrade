package com.maple.maple_banktrade.api.trade.base.input;

import com.maple.maple_banktrade.api.trade.base.context.TradeContext;
import com.maple.maple_banktrade.api.trade.base.context.TradePlan;
import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;

import java.util.Objects;

/**
 * 交易执行阶段输入。
 *
 * @param <C> 上下文类型
 * @param <R> 请求类型
 * @param <P> 计划类型
 */
public interface TradeExecuteInput<C extends TradeContext, R extends TradeRequest, P extends TradePlan> extends TradeCheckInput<C, R> {

    /** 获取检查阶段产出的计划。 */
    P plan();

    /** 创建执行阶段输入。 */
    static <C extends TradeContext, R extends TradeRequest, P extends TradePlan> Basic<C, R, P> of(C context,
                                                                                                   R request,
                                                                                                   P plan) {
        return new Basic<>(context, request, plan);
    }

    /** 由检查输入与计划组装执行输入。 */
    static <C extends TradeContext, R extends TradeRequest, P extends TradePlan> Basic<C, R, P> from(TradeCheckInput<C, R> input,
                                                                                                     P plan) {
        Objects.requireNonNull(input, "input");
        return of(input.context(), input.request(), plan);
    }

    /**
     * 通用执行阶段输入实现。
     */
    record Basic<C extends TradeContext, R extends TradeRequest, P extends TradePlan>(C context, R request,
                                                                                      P plan)
            implements TradeExecuteInput<C, R, P> {

        public Basic {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(plan, "plan");
        }
    }
}

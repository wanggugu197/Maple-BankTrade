package com.maple.maple_banktrade.api.trade.base.input;

import com.maple.maple_banktrade.api.trade.base.context.TradeContext;
import com.maple.maple_banktrade.api.trade.base.context.TradePlan;
import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;

import java.util.Objects;

/**
 * 交易成功后处理阶段输入（主交易已提交）。
 *
 * @param <C> 上下文类型
 * @param <R> 请求类型
 * @param <P> 计划类型
 * @param <E> 执行结果类型
 */
public interface TradeSuccessInput<C extends TradeContext, R extends TradeRequest, P extends TradePlan, E extends TradeExecuteResult<?>> extends TradeExecuteInput<C, R, P> {

    /** 获取主交易执行结果。 */
    E executionResult();

    /** 创建成功后处理输入。 */
    static <C extends TradeContext, R extends TradeRequest, P extends TradePlan, E extends TradeExecuteResult<?>> Basic<C, R, P, E> of(
                                                                                                                                       C context, R request, P plan, E executionResult) {
        return new Basic<>(context, request, plan, executionResult);
    }

    /** 由执行输入与结果组装成功后处理输入。 */
    static <C extends TradeContext, R extends TradeRequest, P extends TradePlan, E extends TradeExecuteResult<?>> Basic<C, R, P, E> from(
                                                                                                                                         TradeExecuteInput<C, R, P> input, E executionResult) {
        Objects.requireNonNull(input, "input");
        return of(input.context(), input.request(), input.plan(), executionResult);
    }

    /**
     * 通用成功后处理阶段输入实现。
     */
    record Basic<C extends TradeContext, R extends TradeRequest, P extends TradePlan, E extends TradeExecuteResult<?>>(
                                                                                                                       C context, R request, P plan, E executionResult)
            implements TradeSuccessInput<C, R, P, E> {

        public Basic {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(plan, "plan");
            Objects.requireNonNull(executionResult, "executionResult");
        }
    }
}

package com.maple.maple_banktrade.api.trade.base.definition;

import com.maple.maple_banktrade.api.trade.base.context.TradeContext;
import com.maple.maple_banktrade.api.trade.base.context.TradePlan;
import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;

import java.util.Objects;

/**
 * 标准三段式交易运行器：check → execute → afterSuccess。
 */
public final class TradeRunner {

    /** 使用通用 Basic 输入类型运行交易定义。 */
    public static <C extends TradeContext, R extends TradeRequest, P extends TradePlan, D> TradeExecuteResult<D> run(
                                                                                                                     TradeDefinition<TradeCheckInput.Basic<C, R>, P, TradeExecuteInput.Basic<C, R, P>, TradeExecuteResult<D>, TradeSuccessInput.Basic<C, R, P, TradeExecuteResult<D>>> definition,
                                                                                                                     C context,
                                                                                                                     R request) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(request, "request");

        TradeCheckInput.Basic<C, R> checkInput = TradeCheckInput.of(context, request);
        TradeCheckResult<P> checkResult = definition.check(checkInput);
        if (checkResult.denied()) {
            return TradeExecuteResult.failure(null, checkResult.messages());
        }

        TradeExecuteInput.Basic<C, R, P> executeInput = TradeExecuteInput.from(checkInput, checkResult.plan());
        TradeExecuteResult<D> executeResult = definition.execute(executeInput);
        if (executeResult.success()) {
            definition.afterSuccess(TradeSuccessInput.from(executeInput, executeResult));
        }
        return executeResult;
    }

    private TradeRunner() {}
}

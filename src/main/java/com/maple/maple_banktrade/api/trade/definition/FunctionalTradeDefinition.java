package com.maple.maple_banktrade.api.trade.definition;

import com.maple.maple_banktrade.api.trade.context.TradePlan;
import com.maple.maple_banktrade.api.trade.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.stage.TradeChecker;
import com.maple.maple_banktrade.api.trade.stage.TradeExecutor;
import com.maple.maple_banktrade.api.trade.stage.TradeSuccessHandler;

import java.util.Objects;

/**
 * 用 check / execute / afterSuccess 三个函数组装的交易定义。
 *
 * @param <CI> 检查阶段输入
 * @param <P>  检查阶段计划
 * @param <EI> 执行阶段输入
 * @param <ER> 执行阶段结果
 * @param <SI> 成功后处理输入
 */
public final class FunctionalTradeDefinition<
        CI extends TradeCheckInput<?, ?>,
        P extends TradePlan,
        EI extends TradeExecuteInput<?, ?, P>,
        ER extends TradeExecuteResult<?>,
        SI extends TradeSuccessInput<?, ?, P, ER>>
                                            implements TradeDefinition<CI, P, EI, ER, SI> {

    // ==============================================
    // 字段
    // ==============================================

    private final TradeChecker<CI, P> checker;
    private final TradeExecutor<EI, ER> executor;
    private final TradeSuccessHandler<SI> successHandler;

    // ==============================================
    // 构造 / 工厂
    // ==============================================

    /** 用三个阶段函数创建交易定义。 */
    public FunctionalTradeDefinition(TradeChecker<CI, P> checker,
                                     TradeExecutor<EI, ER> executor,
                                     TradeSuccessHandler<SI> successHandler) {
        this.checker = Objects.requireNonNull(checker, "checker");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.successHandler = Objects.requireNonNull(successHandler, "successHandler");
    }

    /** 用三个阶段函数创建交易定义。 */
    public static <
            CI extends TradeCheckInput<?, ?>,
            P extends TradePlan,
            EI extends TradeExecuteInput<?, ?, P>,
            ER extends TradeExecuteResult<?>,
            SI extends TradeSuccessInput<?, ?, P, ER>> FunctionalTradeDefinition<CI, P, EI, ER, SI> of(TradeChecker<CI, P> checker,
                                                                                                       TradeExecutor<EI, ER> executor,
                                                                                                       TradeSuccessHandler<SI> successHandler) {
        return new FunctionalTradeDefinition<>(checker, executor, successHandler);
    }

    // ==============================================
    // 交易阶段
    // ==============================================

    /** 执行检查阶段。 */
    @Override
    public TradeCheckResult<P> check(CI input) {
        return checker.check(input);
    }

    /** 执行提交阶段。 */
    @Override
    public ER execute(EI input) {
        return executor.execute(input);
    }

    /** 执行成功后处理。 */
    @Override
    public void afterSuccess(SI input) {
        successHandler.afterSuccess(input);
    }
}

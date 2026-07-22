package com.maple.maple_banktrade.api.trade.base.definition;

import com.maple.maple_banktrade.api.trade.base.context.TradePlan;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.base.stage.TradeChecker;
import com.maple.maple_banktrade.api.trade.base.stage.TradeExecutor;
import com.maple.maple_banktrade.api.trade.base.stage.TradeSuccessHandler;

/**
 * 三段式交易定义：check → execute → afterSuccess。
 *
 * @param <CI> 检查阶段输入
 * @param <P>  检查阶段计划
 * @param <EI> 执行阶段输入
 * @param <ER> 执行阶段结果
 * @param <SI> 成功后处理输入
 */
public interface TradeDefinition<
        CI extends TradeCheckInput<?, ?>,
        P extends TradePlan,
        EI extends TradeExecuteInput<?, ?, P>,
        ER extends TradeExecuteResult<?>,
        SI extends TradeSuccessInput<?, ?, P, ER>>
                                extends TradeChecker<CI, P>, TradeExecutor<EI, ER>, TradeSuccessHandler<SI> {}

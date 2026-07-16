package com.maple.maple_banktrade.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.trade.definition.TradeRunner;
import com.maple.maple_banktrade.api.trade.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.result.TradeExecuteResult;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 机器多资源交易对外入口。
 */
@UtilityClass
public class MachineTradeHandler {

    /**
     * 按期望次数执行；check 可能降级实际次数，成功时 detail.tradeCount 为实际值。
     */
    public static TradeExecuteResult<MachineTradeDetail> run(@NonNull MachineTradeContext context,
                                                             @NonNull Identifier tradeId,
                                                             int desiredCount) {
        if (desiredCount <= 0) {
            return TradeExecuteResult.failure(null);
        }
        return TradeRunner.run(
                MachineTradeDefinition.INSTANCE,
                context,
                MachineTradeRequest.of(tradeId, desiredCount));
    }

    /**
     * 仅 check，不提交；plan.tradeCount 为降级后的可执行次数。
     */
    public static TradeCheckResult<MachineTradePlan> check(@NonNull MachineTradeContext context,
                                                           @NonNull Identifier tradeId,
                                                           int desiredCount) {
        if (desiredCount <= 0) {
            return TradeCheckResult.of(MachineTradePlan.denied(desiredCount));
        }
        return MachineTradeDefinition.INSTANCE.check(
                TradeCheckInput.of(context, MachineTradeRequest.of(tradeId, desiredCount)));
    }

    /** 当前上下文下可见的交易条目。 */
    public static List<Map.Entry<Identifier, MachineTrade>> listVisible(@NonNull MachineTradeContext context) {
        return context.storage().listVisible(context);
    }
}

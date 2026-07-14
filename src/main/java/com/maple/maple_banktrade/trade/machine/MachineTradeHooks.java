package com.maple.maple_banktrade.trade.machine;

import net.minecraft.network.chat.Component;

import com.maple.maple_banktrade.api.trade.result.TradeExecuteResult;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * 机器交易自定义钩子。
 */
@UtilityClass
public class MachineTradeHooks {

    /** 始终可见。 */
    public static final MachineTradeVisibilityCheck ALWAYS_VISIBLE = (context, trade) -> true;

    /** 无附加检查，始终通过。 */
    public static final MachineTradeCheckHook PASS = (context, request, trade) -> List.of();

    /** 无成功后处理。 */
    public static final MachineTradeSuccessHook NOOP = (context, request, plan, result) -> {};

    /**
     * 交易可见性：用于列表/UI 过滤，不进入 {@code TradeRunner}。
     */
    @FunctionalInterface
    public interface MachineTradeVisibilityCheck {

        /** 当前上下文下该条目是否可见。 */
        boolean isVisible(MachineTradeContext context, MachineTrade trade);
    }

    /**
     * check 阶段附加检查：失败则整笔 denied，不进入次数降级。
     */
    @FunctionalInterface
    public interface MachineTradeCheckHook {

        /**
         * 返回空列表表示通过；非空 messages 表示硬拒绝。
         */
        List<Component> check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade);
    }

    /**
     * 主交易成功后的副作用处理。
     */
    @FunctionalInterface
    public interface MachineTradeSuccessHook {

        /** 仅在 execute 成功后调用；{@code plan.tradeCount()} 为实际次数。 */
        void afterSuccess(MachineTradeContext context,
                          MachineTradeRequest request,
                          MachineTradePlan plan,
                          TradeExecuteResult<MachineTradeDetail> result);
    }
}

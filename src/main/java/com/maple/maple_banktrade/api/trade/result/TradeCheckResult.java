package com.maple.maple_banktrade.api.trade.result;

import net.minecraft.network.chat.Component;

import com.maple.maple_banktrade.api.trade.context.TradePlan;

import java.util.List;
import java.util.Objects;

/**
 * 检查阶段结果。
 *
 * @param <P> 计划类型
 */
public interface TradeCheckResult<P extends TradePlan> {

    /** 获取检查产出的计划。 */
    P plan();

    /** 获取检查阶段消息列表。 */
    List<Component> messages();

    /** 计划是否可执行。 */
    default boolean allowed() {
        return plan().executable();
    }

    /** 计划是否不可执行。 */
    default boolean denied() {
        return !allowed();
    }

    /** 由计划创建检查结果。 */
    static <P extends TradePlan> TradeCheckResult<P> of(P plan) {
        return of(plan, List.of());
    }

    /** 由计划与消息创建检查结果。 */
    static <P extends TradePlan> TradeCheckResult<P> of(P plan, List<Component> messages) {
        return new Basic<>(plan, messages);
    }

    /** 创建可执行的基础检查结果。 */
    static TradeCheckResult<TradePlan.Basic> pass() {
        return of(TradePlan.allowed());
    }

    /** 创建带消息的可执行基础检查结果。 */
    static TradeCheckResult<TradePlan.Basic> pass(List<Component> messages) {
        return of(TradePlan.allowed(), messages);
    }

    /** 创建不可执行的基础检查结果。 */
    static TradeCheckResult<TradePlan.Basic> fail() {
        return of(TradePlan.denied());
    }

    /** 创建带消息的不可执行基础检查结果。 */
    static TradeCheckResult<TradePlan.Basic> fail(List<Component> messages) {
        return of(TradePlan.denied(), messages);
    }

    /**
     * 通用检查结果实现。
     */
    record Basic<P extends TradePlan>(P plan, List<Component> messages) implements TradeCheckResult<P> {

        public Basic {
            Objects.requireNonNull(plan, "plan");
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }
}

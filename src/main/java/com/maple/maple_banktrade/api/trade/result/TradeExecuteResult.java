package com.maple.maple_banktrade.api.trade.result;

import net.minecraft.network.chat.Component;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 执行阶段结果。
 *
 * @param <D> 附加结果明细类型
 */
public interface TradeExecuteResult<D> {

    /** 主交易是否成功。 */
    boolean success();

    /** 获取附加明细，可能为 null。 */
    @Nullable
    D detail();

    /** 获取执行阶段消息列表。 */
    List<Component> messages();

    /** 主交易是否失败。 */
    default boolean failed() {
        return !success();
    }

    /** 创建无明细的成功结果。 */
    static TradeExecuteResult<Void> succeeded() {
        return success(null);
    }

    /** 创建带明细的成功结果。 */
    static <D> TradeExecuteResult<D> success(@Nullable D detail) {
        return success(detail, List.of());
    }

    /** 创建带明细与消息的成功结果。 */
    static <D> TradeExecuteResult<D> success(@Nullable D detail, List<Component> messages) {
        return new Basic<>(true, detail, messages);
    }

    /** 创建无明细的失败结果。 */
    static TradeExecuteResult<Void> failure() {
        return failure(null);
    }

    /** 创建带明细的失败结果。 */
    static <D> TradeExecuteResult<D> failure(@Nullable D detail) {
        return failure(detail, List.of());
    }

    /** 创建带明细与消息的失败结果。 */
    static <D> TradeExecuteResult<D> failure(@Nullable D detail, List<Component> messages) {
        return new Basic<>(false, detail, messages);
    }

    /**
     * 通用执行结果实现。
     */
    record Basic<D>(boolean success, @Nullable D detail,
                    List<Component> messages)
            implements TradeExecuteResult<D> {

        public Basic {
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }
}

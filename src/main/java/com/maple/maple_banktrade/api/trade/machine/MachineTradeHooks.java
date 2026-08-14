package com.maple.maple_banktrade.api.trade.machine;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import lombok.Getter;
import lombok.Setter;

/**
 * 机器交易钩子定义：包含可见性、额外检查、成功回调三种钩子的基类及默认实现。
 * <p>
 * 所有钩子均实现 {@link IPersistedSerializable}，支持通过 LDLib2 持久化。
 * 使用时，只需在 {@link MachineTrade} 中持有具体子类实例即可。
 * </p>
 */
public final class MachineTradeHooks {

    // ============================================
    // 1. 可见性钩子基类
    // ============================================
    @Setter
    @Getter
    public abstract static class VisibilityHook implements IPersistedSerializable {

        /** 可见性检查 */
        public abstract boolean isVisible(MachineTradeContext context, MachineTrade trade);
    }

    // ============================================
    // 2. 额外检查钩子基类
    // ============================================
    @Setter
    @Getter
    public abstract static class CheckHook implements IPersistedSerializable {

        /**
         * 执行额外检查。
         * <p>
         * 可通过 {@link MachineTradeRequest#setDesiredCount(int)} 降低本次交易的期望次数
         * （限购 / 次数降级），{@link MachineTradeDefinition} 会在本方法返回后重新读取该值，
         * 并以降级后的次数计算可执行量。
         * </p>
         *
         * @return true 表示检查通过
         */
        public abstract boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade);
    }

    // ============================================
    // 3. 成功回调钩子基类
    // ============================================
    @Setter
    @Getter
    public abstract static class SuccessHook implements IPersistedSerializable {

        /** 交易成功后的副作用处理 */
        public abstract void afterSuccess(MachineTradeContext context,
                                          MachineTradeRequest request,
                                          MachineTradePlan plan,
                                          TradeExecuteResult<MachineTradeDetail> result);
    }

    // ============================================
    // 默认实现
    // ============================================

    /** 始终可见 */
    public static final class AlwaysVisibleHook extends VisibilityHook {

        @Override
        public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
            return true;
        }
    }

    /** 无额外检查，始终通过 */
    public static final class PassCheckHook extends CheckHook {

        @Override
        public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
            return true;
        }
    }

    /** 无操作成功回调 */
    public static final class NoopSuccessHook extends SuccessHook {

        @Override
        public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                                 MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {}
    }

    private MachineTradeHooks() {}
}

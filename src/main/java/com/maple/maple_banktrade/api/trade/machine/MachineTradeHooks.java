package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * 机器交易自定义钩子接口定义及默认实现。
 * 注意：默认实现仅用于注册表，实际条目应通过 ID 引用。
 */
@UtilityClass
public class MachineTradeHooks {

    /** 始终可见。 */
    public static final MachineTradeVisibilityCheck ALWAYS_VISIBLE = (_, _) -> true;

    /** 无附加检查，始终通过。 */
    public static final MachineTradeCheckHook PASS = (_, _, _) -> List.of();

    /** 无成功后处理。 */
    public static final MachineTradeSuccessHook NOOP = (_, _, _, _) -> {};

    /**
     * 交易可见性：用于列表/UI 过滤，不进入 TradeRunner。
     */
    @FunctionalInterface
    public interface MachineTradeVisibilityCheck {

        boolean isVisible(MachineTradeContext context, MachineTrade trade);
    }

    /**
     * check 阶段附加检查：失败则整笔 denied，不进入次数降级。
     */
    @FunctionalInterface
    public interface MachineTradeCheckHook {

        List<Component> check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade);
    }

    /**
     * 主交易成功后的副作用处理。
     */
    @FunctionalInterface
    public interface MachineTradeSuccessHook {

        void afterSuccess(MachineTradeContext context,
                          MachineTradeRequest request,
                          MachineTradePlan plan,
                          TradeExecuteResult<MachineTradeDetail> result);
    }

    // ---------- 工厂接口（供注册表使用） ----------
    @FunctionalInterface
    public interface VisibilityCheckFactory {

        MachineTradeVisibilityCheck create(CompoundTag config);
    }

    @FunctionalInterface
    public interface CheckHookFactory {

        MachineTradeCheckHook create(CompoundTag config);
    }

    @FunctionalInterface
    public interface SuccessHookFactory {

        MachineTradeSuccessHook create(CompoundTag config);
    }
}

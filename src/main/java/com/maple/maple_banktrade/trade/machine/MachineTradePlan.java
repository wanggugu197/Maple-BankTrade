package com.maple.maple_banktrade.trade.machine;

import com.maple.maple_banktrade.api.trade.context.TradePlan;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ScaledIO;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * 机器交易计划：含期望次数、实际可执行次数与放大后 I/O。
 */
public record MachineTradePlan(
                               boolean executable,
                               @Nullable MachineTrade trade,
                               int desiredCount,
                               int tradeCount,
                               ScaledIO scaledIo)
        implements TradePlan {

    public MachineTradePlan {
        scaledIo = scaledIo == null ? ScaledIO.empty() : scaledIo;
        if (desiredCount < 0 || tradeCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
    }

    /** 不可执行计划。 */
    public static MachineTradePlan denied() {
        return new MachineTradePlan(false, null, 0, 0, ScaledIO.empty());
    }

    /** 不可执行计划（保留期望次数便于对照）。 */
    public static MachineTradePlan denied(int desiredCount) {
        return new MachineTradePlan(false, null, Math.max(0, desiredCount), 0, ScaledIO.empty());
    }

    /**
     * 可执行计划。
     *
     * @param tradeCount 实际次数，须 ≥ 1 且 ≤ desiredCount
     */
    public static MachineTradePlan of(MachineTrade trade, int desiredCount, int tradeCount, ScaledIO scaledIo) {
        Objects.requireNonNull(trade, "trade");
        Objects.requireNonNull(scaledIo, "scaledIo");
        if (desiredCount <= 0 || tradeCount > desiredCount) {
            throw new IllegalArgumentException("invalid counts: desired=" + desiredCount + ", actual=" + tradeCount);
        }
        return new MachineTradePlan(true, trade, desiredCount, tradeCount, scaledIo);
    }
}

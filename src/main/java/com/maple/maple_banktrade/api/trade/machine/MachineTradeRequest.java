package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;

import java.util.Objects;

/**
 * 机器交易请求：目标条目与期望次数（实际次数由 check 降级后写入 plan）。
 */
public record MachineTradeRequest(Identifier tradeId, int desiredCount) implements TradeRequest {

    public MachineTradeRequest {
        Objects.requireNonNull(tradeId, "tradeId");
        if (desiredCount <= 0) {
            throw new IllegalArgumentException("desiredCount must be positive");
        }
    }

    /** 创建请求。 */
    public static MachineTradeRequest of(Identifier tradeId, int desiredCount) {
        return new MachineTradeRequest(tradeId, desiredCount);
    }
}

package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.ResourceLocation;

import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;

import java.util.Objects;

/**
 * 机器交易请求：目标条目与期望次数（实际次数由 check 降级后写入 plan）。
 * <p>
 * 期望次数<b>可变</b>：{@link MachineTradeHooks.CheckHook} 可在 check 阶段通过
 * {@link #setDesiredCount(int)} 将其降低（限购 / 次数降级），
 * {@link MachineTradeDefinition} 会在钩子返回后重新读取该值。
 * </p>
 */
public final class MachineTradeRequest implements TradeRequest {

    private final ResourceLocation tradeId;
    private int desiredCount;

    public MachineTradeRequest(ResourceLocation tradeId, int desiredCount) {
        this.tradeId = Objects.requireNonNull(tradeId, "tradeId");
        setDesiredCount(desiredCount);
    }

    public ResourceLocation tradeId() {
        return tradeId;
    }

    public int desiredCount() {
        return desiredCount;
    }

    /**
     * 修改期望次数（供 CheckHook 限购 / 次数降级使用）。
     *
     * @param desiredCount 新的期望次数，必须为正数
     */
    public void setDesiredCount(int desiredCount) {
        if (desiredCount <= 0) {
            throw new IllegalArgumentException("desiredCount must be positive");
        }
        this.desiredCount = desiredCount;
    }

    /** 创建请求。 */
    public static MachineTradeRequest of(ResourceLocation tradeId, int desiredCount) {
        return new MachineTradeRequest(tradeId, desiredCount);
    }

    @Override
    public String toString() {
        return "MachineTradeRequest[" + tradeId + ", desired=" + desiredCount + "]";
    }
}

package com.maple.maple_banktrade.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ScaledIO;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 机器交易执行明细。
 */
public record MachineTradeDetail(
                                 Identifier tradeId,
                                 int desiredCount,
                                 int tradeCount,
                                 ScaledIO scaledIo,
                                 List<UUID> cardUuids) {

    public MachineTradeDetail {
        Objects.requireNonNull(tradeId, "tradeId");
        scaledIo = scaledIo == null ? ScaledIO.empty() : scaledIo;
        cardUuids = cardUuids == null ? List.of() : List.copyOf(cardUuids);
    }

    /** 实际次数是否相对期望做了降级。 */
    public boolean reduced() {
        return tradeCount < desiredCount;
    }
}

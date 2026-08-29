package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import lombok.Getter;

/**
 * 交易数据同步完成事件（客户端）。
 * 当客户端通过 RPC 从服务端收到指定类型的完整条目数据后触发。
 */
@Getter
public class TradeSyncEvent extends Event {

    private final ResourceLocation tradeTypeId;

    public TradeSyncEvent(ResourceLocation tradeTypeId) {
        this.tradeTypeId = tradeTypeId;
    }
}

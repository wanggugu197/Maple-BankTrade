package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.Identifier;

/**
 * 某交易类型实例化后的存储器容器。
 */
public interface TradeStorage {

    /** 对应的交易类型 ID。 */
    Identifier tradeTypeId();
}

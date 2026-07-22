package com.maple.maple_banktrade.api.trade.base.registry;

/**
 * 可校验的交易条目标记，供存储器注册时校验。
 */
public interface TradeInfo {

    /** 条目数据是否有效。 */
    boolean isValid();
}

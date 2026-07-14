package com.maple.maple_banktrade.api.trade.context;

/**
 * 交易请求（触发来源、控制参数等）。
 */
public interface TradeRequest {

    /** 创建空请求。 */
    static Basic empty() {
        return new Basic();
    }

    /**
     * 最简单的交易请求实现。
     */
    record Basic() implements TradeRequest {}
}

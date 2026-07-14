package com.maple.maple_banktrade.api.trade.context;

/**
 * 检查阶段产出的交易计划，供执行阶段使用。
 */
public interface TradePlan {

    /** 当前计划是否可执行。 */
    boolean executable();

    /** 创建指定可执行性的基础计划。 */
    static Basic of(boolean executable) {
        return new Basic(executable);
    }

    /** 创建可执行计划。 */
    static Basic allowed() {
        return new Basic(true);
    }

    /** 创建不可执行计划。 */
    static Basic denied() {
        return new Basic(false);
    }

    /**
     * 最简单的交易计划实现。
     */
    record Basic(boolean executable) implements TradePlan {}
}

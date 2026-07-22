package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;

/**
 * 按交易 ID 索引条目的存储器。
 *
 * @param <E> 交易条目类型
 */
public interface TradeEntryStorage<E> extends TradeStorage {

    /** 注册条目；ID 已存在时返回已有条目且不覆盖。 */
    E register(Identifier tradeId, E entry);

    /** 按 ID 查找条目。 */
    Optional<E> find(Identifier tradeId);

    /** 按 ID 查找条目，不存在时返回 null。 */
    E require(Identifier tradeId);

    /** 返回全部条目的只读视图。 */
    Map<Identifier, E> entries();

    /** 返回已注册条目数量。 */
    default int size() {
        return entries().size();
    }

    /** 判断是否没有任何条目。 */
    default boolean isEmpty() {
        return size() == 0;
    }
}

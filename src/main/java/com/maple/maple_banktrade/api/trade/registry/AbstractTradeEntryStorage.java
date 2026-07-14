package com.maple.maple_banktrade.api.trade.registry;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.*;

/**
 * 基于 LinkedHashMap 的交易条目存储器基类。
 *
 * @param <E> 交易条目类型
 */
public abstract class AbstractTradeEntryStorage<E> implements TradeEntryStorage<E> {

    // ==============================================
    // 字段
    // ==============================================

    private final Identifier tradeTypeId;
    private final Map<Identifier, E> entries = new LinkedHashMap<>();

    // ==============================================
    // 构造
    // ==============================================

    /** 绑定交易类型 ID 创建空存储器。 */
    protected AbstractTradeEntryStorage(Identifier tradeTypeId) {
        this.tradeTypeId = Objects.requireNonNull(tradeTypeId, "tradeTypeId");
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 返回对应交易类型 ID。 */
    @Override
    public Identifier tradeTypeId() {
        return tradeTypeId;
    }

    /** 按 ID 查找条目。 */
    @Override
    public Optional<E> find(Identifier tradeId) {
        if (tradeId == null) return Optional.empty();
        return Optional.ofNullable(entries.get(tradeId));
    }

    /** 按 ID 查找条目，不存在时返回 null。 */
    @Override
    public E require(Identifier tradeId) {
        return tradeId == null ? null : entries.get(tradeId);
    }

    /** 返回全部条目的只读视图。 */
    @Override
    public Map<Identifier, E> entries() {
        return Collections.unmodifiableMap(entries);
    }

    // ==============================================
    // 注册
    // ==============================================

    /** 校验条目是否可注册。 */
    protected abstract boolean isValidEntry(E entry);

    /** 注册条目；ID 已存在时返回已有条目且不覆盖。 */
    @Override
    public E register(Identifier tradeId, E entry) {
        Objects.requireNonNull(tradeId, "tradeId");
        Objects.requireNonNull(entry, "entry");
        if (!isValidEntry(entry)) {
            throw new IllegalArgumentException("Invalid trade entry: " + tradeTypeId + "/" + tradeId);
        }

        E existing = entries.get(tradeId);
        if (existing != null) {
            MapleBankTrade.LOGGER.error("Trade entry already exists: {}/{}", tradeTypeId, tradeId);
            return existing;
        }

        entries.put(tradeId, entry);
        return entry;
    }
}

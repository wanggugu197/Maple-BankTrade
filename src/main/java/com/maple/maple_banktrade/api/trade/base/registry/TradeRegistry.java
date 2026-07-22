package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.*;

/**
 * 交易类型与存储器全局注册表。
 */
public final class TradeRegistry {

    // ==============================================
    // 字段
    // ==============================================

    private static final Map<Identifier, TradeType<?>> TYPES = new LinkedHashMap<>();
    private static final Map<Identifier, TradeStorage> STORAGES = new LinkedHashMap<>();

    // ==============================================
    // 注册
    // ==============================================

    /** 注册交易类型并创建存储器；已存在时返回已有实例。 */
    public static <S extends TradeStorage> S registerType(TradeType<S> type) {
        Objects.requireNonNull(type, "type");
        Identifier id = Objects.requireNonNull(type.id(), "type.id");

        TradeType<?> existingType = TYPES.get(id);
        if (existingType != null) {
            MapleBankTrade.LOGGER.error("Trade type already registered: {}", id);
            return castStorage(STORAGES.get(id), type);
        }

        S storage = Objects.requireNonNull(type.createStorage(), "type.createStorage");
        if (!id.equals(storage.tradeTypeId())) {
            throw new IllegalStateException("Storage tradeTypeId mismatch: expected " + id + ", got " + storage.tradeTypeId());
        }

        TYPES.put(id, type);
        STORAGES.put(id, storage);
        return storage;
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 按类型 ID 查找交易类型。 */
    public static Optional<TradeType<?>> findType(Identifier tradeTypeId) {
        if (tradeTypeId == null) return Optional.empty();
        return Optional.ofNullable(TYPES.get(tradeTypeId));
    }

    /** 按类型 ID 查找存储器。 */
    public static Optional<TradeStorage> findStorage(Identifier tradeTypeId) {
        if (tradeTypeId == null) return Optional.empty();
        return Optional.ofNullable(STORAGES.get(tradeTypeId));
    }

    /** 按类型 ID 与期望类查找存储器。 */
    public static <S extends TradeStorage> Optional<S> findStorage(Identifier tradeTypeId, Class<S> storageClass) {
        Objects.requireNonNull(storageClass, "storageClass");
        return findStorage(tradeTypeId)
                .filter(storageClass::isInstance)
                .map(storageClass::cast);
    }

    /** 按类型 ID 与期望类获取存储器，不存在时返回 null。 */
    public static <S extends TradeStorage> S requireStorage(Identifier tradeTypeId, Class<S> storageClass) {
        return findStorage(tradeTypeId, storageClass).orElse(null);
    }

    /** 返回全部已注册交易类型的只读视图。 */
    public static Map<Identifier, TradeType<?>> types() {
        return Collections.unmodifiableMap(TYPES);
    }

    /** 返回全部已实例化存储器的只读视图。 */
    public static Map<Identifier, TradeStorage> storages() {
        return Collections.unmodifiableMap(STORAGES);
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 将已有存储器安全转型为期望类型。 */
    @SuppressWarnings("unchecked")
    private static <S extends TradeStorage> S castStorage(TradeStorage storage, TradeType<S> type) {
        if (storage == null) return null;
        S created = type.createStorage();
        if (created != null && created.getClass().isInstance(storage)) {
            return (S) storage;
        }
        return null;
    }

    private TradeRegistry() {}
}

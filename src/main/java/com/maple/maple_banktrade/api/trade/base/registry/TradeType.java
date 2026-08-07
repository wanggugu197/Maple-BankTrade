package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;

/**
 * 交易类型定义：ID + 空存储器工厂。
 *
 * @param <S> 该类型产出的存储器
 */
public interface TradeType<S extends TradeStorage> extends IPersistedSerializable {

    /** 交易类型实例 ID，同时作为注册表键。 */
    Identifier id();

    /** 创建该类型的空存储器。 */
    S createStorage();

    /** 判断存储器是否属于本类型实例。 */
    default boolean supports(TradeStorage storage) {
        return storage != null && id().equals(storage.tradeTypeId());
    }
}

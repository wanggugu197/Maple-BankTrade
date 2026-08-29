package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;

/**
 * 可校验的交易条目标记，供存储器注册时校验。
 */
public interface TradeInfo extends IPersistedSerializable {

    ResourceLocation id();

    /** 条目数据是否有效。 */
    boolean isValid();
}

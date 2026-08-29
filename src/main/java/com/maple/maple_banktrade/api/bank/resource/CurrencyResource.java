package com.maple.maple_banktrade.api.bank.resource;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.transfer.resource.Resource;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;

import java.util.Objects;

/**
 * 已注册银行货币的不可变资源标识（不含数量）。
 */
public final class CurrencyResource implements Resource, IPersistedSerializable {

    @Persisted
    private ResourceLocation currencyTypeId;

    private static final CurrencyResource EMPTY_INSTANCE = new CurrencyResource(null);

    /** 空货币资源。 */
    public static final CurrencyResource EMPTY = EMPTY_INSTANCE;

    /** 无参构造器（供 LDLib2 反序列化使用） */
    public CurrencyResource() {
        this.currencyTypeId = null;
    }

    /** 规范化货币 ID；未注册时视为空。 */
    public CurrencyResource(ResourceLocation currencyTypeId) {
        CurrencyType type = CurrencyType.requireById(currencyTypeId);
        this.currencyTypeId = type == null ? null : type.id();
    }

    /** 按货币 ID 创建资源。 */
    public static CurrencyResource of(ResourceLocation currencyTypeId) {
        return new CurrencyResource(currencyTypeId);
    }

    /** 解析对应的货币类型。 */
    public CurrencyType type() {
        return CurrencyType.requireById(currencyTypeId);
    }

    /** 获取货币 ID。 */
    public ResourceLocation currencyTypeId() {
        return currencyTypeId;
    }

    /** 是否为空资源。 */
    @Override
    public boolean isEmpty() {
        return currencyTypeId == null;
    }

    // ==============================================
    // IPersistedSerializable 实现
    // ==============================================

    public static final Codec<CurrencyResource> CODEC = PersistedParser.createCodec(CurrencyResource::new);

    // ==============================================
    // 重写 equals/hashCode（确保 record 语义）
    // ==============================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CurrencyResource other)) return false;
        return Objects.equals(currencyTypeId, other.currencyTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(currencyTypeId);
    }

    @Override
    public String toString() {
        return "CurrencyResource{currencyTypeId=" + currencyTypeId + "}";
    }
}

package com.maple.maple_banktrade.bank.resource;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.resource.Resource;

import com.maple.maple_banktrade.bank.data.CurrencyType;

/**
 * 已注册银行货币的不可变资源标识（不含数量）。
 */
public record CurrencyResource(Identifier currencyTypeId) implements Resource {

    /** 空货币资源。 */
    public static final CurrencyResource EMPTY = new CurrencyResource(null);

    /** 规范化货币 ID；未注册时视为空。 */
    public CurrencyResource {
        CurrencyType type = CurrencyType.requireById(currencyTypeId);
        currencyTypeId = type == null ? null : type.id();
    }

    /** 按货币 ID 创建资源。 */
    public static CurrencyResource of(Identifier currencyTypeId) {
        return new CurrencyResource(currencyTypeId);
    }

    /** 解析对应的货币类型。 */
    public CurrencyType type() {
        return CurrencyType.requireById(currencyTypeId);
    }

    /** 是否为空资源。 */
    @Override
    public boolean isEmpty() {
        return currencyTypeId == null;
    }
}

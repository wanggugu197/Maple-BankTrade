package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.base.registry.TradeType;

import java.util.Objects;

/**
 * 货币-物品交易类型：身份 + 空存储器工厂。
 */
public final class CurrencyItemTradeType implements TradeType<CurrencyItemTradeStorage> {

    // ==============================================
    // 字段
    // ==============================================

    private final Identifier id;

    // ==============================================
    // 构造 / 工厂
    // ==============================================

    /** 使用完整类型 ID 创建交易类型。 */
    public CurrencyItemTradeType(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /** 使用 trade_type/path 创建交易类型。 */
    public static CurrencyItemTradeType of(String path) {
        return new CurrencyItemTradeType(MapleBankTrade.id("trade_type/" + path));
    }

    // ==============================================
    // TradeType
    // ==============================================

    /** 返回交易类型 ID。 */
    @Override
    public Identifier id() {
        return id;
    }

    /** 创建绑定本类型 ID 的空存储器。 */
    @Override
    public CurrencyItemTradeStorage createStorage() {
        return new CurrencyItemTradeStorage(id);
    }

    /** 注册类型并返回存储器（幂等）。 */
    public CurrencyItemTradeStorage register() {
        CurrencyItemTradeStorage existing = TradeRegistry.requireStorage(id, CurrencyItemTradeStorage.class);
        return existing != null ? existing : TradeRegistry.registerType(this);
    }
}

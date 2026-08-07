package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.base.registry.TradeType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public final class CurrencyItemTradeType implements TradeType<CurrencyItemTradeStorage> {

    @Persisted
    private Identifier id;

    // 无参构造器（反序列化必需）
    public CurrencyItemTradeType() {
        this.id = MapleBankTrade.id("default_currency_item");
    }

    public CurrencyItemTradeType(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static CurrencyItemTradeType of(Identifier id) {
        return new CurrencyItemTradeType(id);
    }

    @Override
    public CurrencyItemTradeStorage createStorage() {
        return new CurrencyItemTradeStorage(id);
    }

    public CurrencyItemTradeStorage register() {
        CurrencyItemTradeStorage existing = TradeRegistry.requireStorage(id, CurrencyItemTradeStorage.class);
        return existing != null ? existing : TradeRegistry.registerType(this);
    }
}

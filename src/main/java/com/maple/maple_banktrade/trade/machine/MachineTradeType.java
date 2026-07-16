package com.maple.maple_banktrade.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.registry.TradeType;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

/**
 * 机器多资源交易类型：身份 + 空存储器工厂。
 */
@Accessors(fluent = true)
public record MachineTradeType(@NonNull Identifier id) implements TradeType<MachineTradeStorage> {

    /**
     * 使用 trade_type/path 创建交易类型。
     */
    public static MachineTradeType of(String path) {
        return new MachineTradeType(MapleBankTrade.id("trade_type/" + path));
    }

    /**
     * 创建绑定本类型 ID 的空存储器。
     */
    @Override
    public MachineTradeStorage createStorage() {
        return new MachineTradeStorage(id);
    }

    /**
     * 注册类型并返回存储器（幂等）。
     */
    public MachineTradeStorage register() {
        MachineTradeStorage existing = TradeRegistry.requireStorage(id, MachineTradeStorage.class);
        return existing != null ? existing : TradeRegistry.registerType(this);
    }
}

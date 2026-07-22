package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.base.registry.TradeType;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

/**
 * 机器多资源交易类型：身份 + 是否允许自动交易 + 空存储器工厂。
 *
 * @param allowAutoTrade 本类型是否允许注册/运行 {@link MachineTrade#autoTrade()} 条目；默认 {@code false}
 */
@Accessors(fluent = true)
public record MachineTradeType(@NonNull Identifier id, boolean allowAutoTrade)
        implements TradeType<MachineTradeStorage> {

    public MachineTradeType {
        java.util.Objects.requireNonNull(id, "id");
    }

    /** 使用 trade_type/path 创建交易类型（不允许自动交易）。 */
    public static MachineTradeType of(String path) {
        return of(path, false);
    }

    /** 使用 trade_type/path 创建交易类型。 */
    public static MachineTradeType of(String path, boolean allowAutoTrade) {
        return new MachineTradeType(MapleBankTrade.id("trade_type/" + path), allowAutoTrade);
    }

    /** 仅 ID 构造（不允许自动交易）。 */
    public MachineTradeType(@NonNull Identifier id) {
        this(id, false);
    }

    @Override
    public MachineTradeStorage createStorage() {
        return new MachineTradeStorage(id, allowAutoTrade);
    }

    /** 注册类型并返回存储器（幂等）。 */
    public MachineTradeStorage register() {
        MachineTradeStorage existing = TradeRegistry.requireStorage(id, MachineTradeStorage.class);
        return existing != null ? existing : TradeRegistry.registerType(this);
    }
}

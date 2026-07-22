package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import com.maple.maple_banktrade.api.trade.base.context.TradeContext;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * 货币-物品交易运行时上下文。
 */
public record CurrencyItemTradeContext(
                                       @Nullable Player player,
                                       MinecraftServer server,
                                       UUID cardUuid,
                                       CurrencyItemTradeStorage storage)
        implements TradeContext {

    public CurrencyItemTradeContext {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cardUuid, "cardUuid");
        Objects.requireNonNull(storage, "storage");
    }
}

package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.maple.maple_banktrade.api.trade.base.definition.TradeRunner;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;

import java.util.Objects;
import java.util.UUID;

/**
 * 货币-物品交易对外入口。
 */
public final class CurrencyItemTradeHandler {

    // ==============================================
    // 卖出
    // ==============================================

    /** 按槽位全部可交易次数卖出。 */
    public static TradeExecuteResult<CurrencyItemTradeDetail> sellAll(ItemStacksResourceHandler sourceHandler,
                                                                      int sourceSlot,
                                                                      UUID cardUuid,
                                                                      MinecraftServer server,
                                                                      CurrencyItemTradeStorage storage) {
        if (storage == null || sourceHandler == null) {
            return TradeExecuteResult.failure(null);
        }
        ItemResource resource = sourceHandler.getResource(sourceSlot);
        if (resource.isEmpty()) {
            return TradeExecuteResult.failure(null);
        }

        CurrencyItemTrade trade = storage.findSellableByItem(resource.toStack(1));
        if (trade == null || trade.itemAmountPerTrade() <= 0) {
            return TradeExecuteResult.failure(null);
        }

        int count = sourceHandler.getAmountAsInt(sourceSlot) / trade.itemAmountPerTrade();
        if (count <= 0) {
            return TradeExecuteResult.failure(null);
        }
        return sell(sourceHandler, sourceSlot, cardUuid, server, storage, count);
    }

    /** 按指定次数卖出槽位物品。 */
    public static TradeExecuteResult<CurrencyItemTradeDetail> sell(ItemStacksResourceHandler sourceHandler,
                                                                   int sourceSlot,
                                                                   UUID cardUuid,
                                                                   MinecraftServer server,
                                                                   CurrencyItemTradeStorage storage,
                                                                   int tradeCount) {
        Objects.requireNonNull(sourceHandler, "sourceHandler");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cardUuid, "cardUuid");
        if (storage == null || tradeCount <= 0) {
            return TradeExecuteResult.failure(null);
        }

        return TradeRunner.run(
                CurrencyItemTradeDefinition.INSTANCE,
                new CurrencyItemTradeContext(null, server, cardUuid, storage),
                CurrencyItemTradeRequest.sell(sourceHandler, sourceSlot, tradeCount));
    }

    // ==============================================
    // 购买
    // ==============================================

    /** 扣卡余额并将物品放入玩家背包。 */
    public static TradeExecuteResult<CurrencyItemTradeDetail> buyToPlayerInventory(Player player,
                                                                                   UUID cardUuid,
                                                                                   MinecraftServer server,
                                                                                   CurrencyItemTradeStorage storage,
                                                                                   Identifier tradeId,
                                                                                   int tradeCount) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cardUuid, "cardUuid");
        Objects.requireNonNull(tradeId, "tradeId");
        if (storage == null || tradeCount <= 0) {
            return TradeExecuteResult.failure(null);
        }

        return TradeRunner.run(
                CurrencyItemTradeDefinition.INSTANCE,
                new CurrencyItemTradeContext(player, server, cardUuid, storage),
                CurrencyItemTradeRequest.buy(tradeId, tradeCount));
    }

    private CurrencyItemTradeHandler() {}
}

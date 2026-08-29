package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.ResourceLocation;

import com.maple.maple_banktrade.api.trade.base.context.TradeRequest;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * 货币-物品交易请求：购买或卖出。
 */
public record CurrencyItemTradeRequest(
                                       Direction direction,
                                       int tradeCount,
                                       @Nullable ResourceLocation tradeId,
                                       @Nullable ObservableItemResourceHandler sourceHandler,
                                       int sourceSlot)
        implements TradeRequest {

    /**
     * 交易方向。
     */
    public enum Direction {
        BUY,
        SELL
    }

    public CurrencyItemTradeRequest {
        Objects.requireNonNull(direction, "direction");
        if (tradeCount <= 0) {
            throw new IllegalArgumentException("tradeCount must be positive");
        }
    }

    /** 创建购买请求。 */
    public static CurrencyItemTradeRequest buy(ResourceLocation tradeId, int tradeCount) {
        return new CurrencyItemTradeRequest(Direction.BUY, tradeCount, Objects.requireNonNull(tradeId, "tradeId"), null, -1);
    }

    /** 创建卖出请求。 */
    public static CurrencyItemTradeRequest sell(ObservableItemResourceHandler sourceHandler, int sourceSlot, int tradeCount) {
        return new CurrencyItemTradeRequest(
                Direction.SELL,
                tradeCount,
                null,
                Objects.requireNonNull(sourceHandler, "sourceHandler"),
                sourceSlot);
    }

    /** 是否为购买。 */
    public boolean isBuy() {
        return direction == Direction.BUY;
    }

    /** 是否为卖出。 */
    public boolean isSell() {
        return direction == Direction.SELL;
    }
}

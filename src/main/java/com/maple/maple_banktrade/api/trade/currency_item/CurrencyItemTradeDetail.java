package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 货币-物品交易执行明细。
 */
public record CurrencyItemTradeDetail(UUID cardUuid,
                                      CurrencyResource currency,
                                      BigInteger currencyAmount,
                                      ItemStack item,
                                      int itemAmount,
                                      int tradeCount,
                                      boolean bought) {

    public CurrencyItemTradeDetail {
        item = item == null ? ItemStack.EMPTY : item.copyWithCount(itemAmount);
    }

    /** 是否为卖出（非购买）。 */
    public boolean sold() {
        return !bought;
    }
}

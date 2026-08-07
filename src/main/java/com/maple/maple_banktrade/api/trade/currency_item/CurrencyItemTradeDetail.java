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
                                      int tradeCount,
                                      boolean bought) {}

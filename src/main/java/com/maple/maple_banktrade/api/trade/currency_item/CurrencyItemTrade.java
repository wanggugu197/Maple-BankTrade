package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.base.registry.TradeInfo;

import java.math.BigInteger;
import java.util.function.Supplier;

/**
 * 货币-物品交易条目：买卖同价，模式区分是否可卖。
 */
public record CurrencyItemTrade(Supplier<ItemStack> itemSupplier,
                                int itemAmountPerTrade,
                                CurrencyResource currency,
                                BigInteger pricePerTrade,
                                Mode mode)
        implements TradeInfo {

    /**
     * 交易模式：至少允许购买，无“仅卖出”。
     */
    public enum Mode {

        BUY_ONLY,
        BUY_AND_SELL;

        /** 是否允许卖出。 */
        public boolean allowsSell() {
            return this == BUY_AND_SELL;
        }
    }

    public CurrencyItemTrade {
        currency = currency == null ? CurrencyResource.EMPTY : currency;
        pricePerTrade = pricePerTrade == null ? BigInteger.ZERO : pricePerTrade;
        mode = mode == null ? Mode.BUY_AND_SELL : mode;
    }

    /** 按物品、货币与单价创建条目（每次 1 个物品）。 */
    public static CurrencyItemTrade of(Item item, Identifier currencyTypeId, long price, Mode mode) {
        return of(item, 1, currencyTypeId, BigInteger.valueOf(price), mode);
    }

    /** 按物品数量、货币与单价创建条目。 */
    public static CurrencyItemTrade of(Item item,
                                       int itemAmountPerTrade,
                                       Identifier currencyTypeId,
                                       BigInteger pricePerTrade,
                                       Mode mode) {
        return new CurrencyItemTrade(
                item == null ? null : item::getDefaultInstance,
                itemAmountPerTrade,
                CurrencyResource.of(currencyTypeId),
                pricePerTrade,
                mode);
    }

    /** 返回按每次交易数量复制的物品栈。 */
    public ItemStack item() {
        if (itemSupplier == null) return ItemStack.EMPTY;
        ItemStack stack = itemSupplier.get();
        return stack == null ? ItemStack.EMPTY : stack.copyWithCount(itemAmountPerTrade);
    }

    /** 是否允许卖出。 */
    public boolean allowsSell() {
        return mode.allowsSell();
    }

    /** 校验条目是否可用于注册与交易。 */
    @Override
    public boolean isValid() {
        return itemSupplier != null && itemAmountPerTrade > 0 && !currency.isEmpty() && pricePerTrade.signum() > 0;
    }
}

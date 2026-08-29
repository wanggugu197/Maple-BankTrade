package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.base.registry.TradeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.util.Objects;

/**
 * 货币-物品交易条目：买卖同价，模式区分是否可卖。
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public final class CurrencyItemTrade implements TradeInfo {

    // ==============================================
    // 持久化与同步字段
    // ==============================================

    @Persisted
    private ResourceLocation id;

    @Persisted
    private ItemStack item;
    @Persisted
    private int itemAmountPerTrade;
    @Persisted
    private CurrencyResource currency;
    @Persisted
    private BigInteger pricePerTrade;
    @Persisted
    private Mode mode;

    // ==============================================
    // 构造器
    // ==============================================

    /** 无参构造器 */
    public CurrencyItemTrade() {
        this.id = null;
        this.item = ItemStack.EMPTY;
        this.itemAmountPerTrade = 0;
        this.currency = CurrencyResource.EMPTY;
        this.pricePerTrade = BigInteger.ZERO;
        this.mode = Mode.BUY_ONLY;
    }

    /** 全参构造器 */
    public CurrencyItemTrade(ResourceLocation id, ItemStack item, int itemAmountPerTrade,
                             CurrencyResource currency, BigInteger pricePerTrade,
                             Mode mode) {
        this.id = id;
        this.item = item.copyWithCount(1);
        this.itemAmountPerTrade = itemAmountPerTrade;
        this.currency = Objects.requireNonNullElse(currency, CurrencyResource.EMPTY);
        this.pricePerTrade = Objects.requireNonNullElse(pricePerTrade, BigInteger.ZERO);
        this.mode = Objects.requireNonNullElse(mode, Mode.BUY_AND_SELL);
    }

    // ==============================================
    // 静态工厂
    // ==============================================

    public static CurrencyItemTrade of(ResourceLocation id, Item item, ResourceLocation currencyTypeId, long price, Mode mode) {
        return of(id, item, 1, currencyTypeId, BigInteger.valueOf(price), mode);
    }

    public static CurrencyItemTrade of(ResourceLocation id, Item item, int itemAmountPerTrade,
                                       ResourceLocation currencyTypeId, BigInteger pricePerTrade,
                                       Mode mode) {
        return new CurrencyItemTrade(
                id,
                new ItemStack(item),
                itemAmountPerTrade,
                CurrencyResource.of(currencyTypeId),
                pricePerTrade,
                mode);
    }

    // ==============================================
    // 业务方法
    // ==============================================

    public boolean allowsSell() {
        return mode.allowsSell();
    }

    @Override
    public boolean isValid() {
        return !item.isEmpty() && itemAmountPerTrade > 0 &&
                !currency.isEmpty() && pricePerTrade.signum() > 0;
    }

    // ==============================================
    // 枚举
    // ==============================================

    public enum Mode {

        BUY_ONLY,
        BUY_AND_SELL;

        public boolean allowsSell() {
            return this == BUY_AND_SELL;
        }
    }
}

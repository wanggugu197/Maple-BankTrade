package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.maple.maple_banktrade.api.trade.base.registry.AbstractTradeEntryStorage;

/**
 * 货币-物品交易条目存储器。
 */
public final class CurrencyItemTradeStorage extends AbstractTradeEntryStorage<CurrencyItemTrade> {

    /** 绑定交易类型 ID 创建空存储器。 */
    public CurrencyItemTradeStorage(Identifier tradeTypeId) {
        super(tradeTypeId);
    }

    /** 校验条目有效性。 */
    @Override
    protected boolean isValidEntry(CurrencyItemTrade entry) {
        return entry != null && entry.isValid();
    }

    /** 查找允许卖出且物品匹配的条目。 */
    public CurrencyItemTrade findSellableByItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (CurrencyItemTrade trade : entries().values()) {
            if (trade.allowsSell() && ItemResource.of(trade.item()).equals(ItemResource.of(stack))) {
                return trade;
            }
        }
        return null;
    }

    @Override
    protected CurrencyItemTrade createEmptyEntry() {
        return new CurrencyItemTrade();
    }
}

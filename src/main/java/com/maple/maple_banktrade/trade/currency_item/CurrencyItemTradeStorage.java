package com.maple.maple_banktrade.trade.currency_item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.registry.AbstractTradeEntryStorage;

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

    /** 在本类型路径下注册条目：typePath/entryPath。 */
    public CurrencyItemTrade register(String entryPath, CurrencyItemTrade trade) {
        return register(MapleBankTrade.id(tradeTypeId().getPath() + "/" + entryPath), trade);
    }

    /** 查找允许卖出且物品匹配的条目。 */
    public CurrencyItemTrade findSellableByItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (CurrencyItemTrade trade : entries().values()) {
            if (trade.allowsSell() && ItemStack.isSameItemSameComponents(stack, trade.item())) {
                return trade;
            }
        }
        return null;
    }

    /** 是否存在可卖出的条目。 */
    public boolean hasSellable() {
        for (CurrencyItemTrade trade : entries().values()) {
            if (trade.allowsSell()) return true;
        }
        return false;
    }
}

package com.maple.maple_banktrade.api.bank.capability;

import net.minecraft.resources.Identifier;

/**
 * 可在钱包 UI 中进行货币-物品交易的银行卡标记接口。
 */
public interface TradableWalletBankCard {

    /** 绑定的货币-物品交易类型 ID。 */
    Identifier getTradeTypeId();
}

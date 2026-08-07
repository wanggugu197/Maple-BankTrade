package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.capability.TradableUI;
import com.maple.maple_banktrade.bank.cards.TradableLargeMultiCurrencyBankCard;

/**
 * 可交易大额多货币银行卡的简略 / 详情 UI。
 */
public final class TradableLargeMultiCurrencyBankCardInfo {

    /**
     * 创建简略卡 UI（复用大额多货币展示）。
     */
    public static UIElement createSimplifiedUI(TradableLargeMultiCurrencyBankCard card) {
        return LargeMultiCurrencyBankCardInfo.createSimplifiedUI(card);
    }

    /**
     * 创建详情卡 UI，并附加交易面板。
     */
    public static UIElement createDetailsUI(Player player, TradableLargeMultiCurrencyBankCard card) {
        UIElement detail = LargeMultiCurrencyBankCardInfo.createDetailsUI(player, card);
        TradableUI.createTradePanel(detail, player, card);
        return detail;
    }

    private TradableLargeMultiCurrencyBankCardInfo() {}
}

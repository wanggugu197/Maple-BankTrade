package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.capability.TradableUI;
import com.maple.maple_banktrade.bank.cards.TradableSingleCurrencyBankCard;

/**
 * 可交易单货币卡的简略 / 详情 UI。
 */
public final class TradableSingleCurrencyBankCardInfo {

    /** 创建简略卡 UI（复用单货币展示）。 */
    public static UIElement createSimplifiedUI(TradableSingleCurrencyBankCard card) {
        UIElement simplified = SingleCurrencyBankCardInfo.createSimplifiedUI(card);
        simplified.addChild(TradableUI.createTradeHead(card, 0.66f));
        return simplified;
    }

    /** 创建详情卡 UI，并附加交易面板。 */
    public static UIElement createDetailsUI(Player player, TradableSingleCurrencyBankCard card) {
        UIElement detail = SingleCurrencyBankCardInfo.createDetailsUI(player, card);
        TradableUI.addTradePanel(detail, player, card);
        return detail;
    }

    private TradableSingleCurrencyBankCardInfo() {}
}

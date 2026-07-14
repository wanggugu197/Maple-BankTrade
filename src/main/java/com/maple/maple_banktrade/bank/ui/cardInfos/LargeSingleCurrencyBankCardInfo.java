package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.BankCardInfoUI;
import com.maple.maple_banktrade.bank.cards.LargeSingleCurrencyBankCard;
import com.maple.maple_banktrade.bank.ui.capability.CurrencyTypeUI;

/**
 * 大额单货币银行卡的简略 / 详情 UI。
 */
public final class LargeSingleCurrencyBankCardInfo {

    /** 创建简略卡 UI（含余额）。 */
    public static UIElement createSimplifiedUI(LargeSingleCurrencyBankCard card) {
        return BankCardInfoUI.createSimplifiedCardUI(card, ui -> {
            BankCardInfoUI.addSimplifiedBase(ui, card);
            CurrencyTypeUI.addCurrencyUI(ui, card, 0.66f);
        });
    }

    /** 创建详情卡 UI（含余额）。 */
    public static UIElement createDetailsUI(Player player, LargeSingleCurrencyBankCard card) {
        return BankCardInfoUI.createDetailsCardUI(card, ui -> {
            BankCardInfoUI.addDetailBase(player, ui, card);
            CurrencyTypeUI.addCurrencyUI(ui, card, 1);
        });
    }

    private LargeSingleCurrencyBankCardInfo() {}
}

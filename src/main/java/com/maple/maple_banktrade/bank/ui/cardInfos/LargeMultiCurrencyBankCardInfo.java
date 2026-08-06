package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.BankCardInfoUI;
import com.maple.maple_banktrade.api.bank.ui.capability.CurrencyTypeUI;
import com.maple.maple_banktrade.bank.cards.LargeMultiCurrencyBankCard;

public final class LargeMultiCurrencyBankCardInfo {

    public static UIElement createSimplifiedUI(LargeMultiCurrencyBankCard card) {
        return BankCardInfoUI.createSimplifiedCardUI(card, ui -> {
            BankCardInfoUI.addSimplifiedBase(ui, card);
            CurrencyTypeUI.addCurrencyUI(ui, card, 0.66f);
        });
    }

    public static UIElement createDetailsUI(Player player, LargeMultiCurrencyBankCard card) {
        return BankCardInfoUI.createDetailsCardUI(card, ui -> {
            BankCardInfoUI.addDetailBase(player, ui, card);
            CurrencyTypeUI.addCurrencyUI(ui, card, 1);
        });
    }

    private LargeMultiCurrencyBankCardInfo() {}
}

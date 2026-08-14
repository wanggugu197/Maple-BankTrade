package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.BankCardInfoUI;
import com.maple.maple_banktrade.api.bank.ui.capability.InfoListUI;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;

/**
 * 信息存储卡（TaggedBankCard）的简略 / 详情 UI。
 * 显示绑定的信息列表名称、总条目数、已完成条目数，以及每个条目的具体进度。
 */
public final class TaggedBankCardInfo {

    /**
     * 创建简略卡 UI：显示信息列表名称及完成进度概览。
     */
    public static UIElement createSimplifiedUI(TaggedBankCard card) {
        return BankCardInfoUI.createSimplifiedCardUI(card, ui -> {
            BankCardInfoUI.addSimplifiedBase(ui, card);
            InfoListUI.addTaggedSimplifiedUIElement(ui, card, 0.66f);
        });
    }

    /**
     * 创建详情卡 UI：列出信息列表中每个条目的名称、进度（已完成次数/所需次数）和完成状态。
     */
    public static UIElement createDetailsUI(Player player, TaggedBankCard card) {
        return BankCardInfoUI.createDetailsCardUI(card, ui -> {
            BankCardInfoUI.addDetailBase(player, ui, card);
            InfoListUI.addTaggedSimplifiedUIElement(ui, card, 1);
            InfoListUI.addInfoEntryUI(ui, card);
        });
    }

    private TaggedBankCardInfo() {}
}

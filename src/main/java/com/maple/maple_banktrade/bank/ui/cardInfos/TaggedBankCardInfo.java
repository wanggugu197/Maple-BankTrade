package com.maple.maple_banktrade.bank.ui.cardInfos;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.ui.BankCardInfoUI;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;

import java.util.stream.Collectors;

/**
 * 标记银行卡的简略 / 详情 UI。
 */
public final class TaggedBankCardInfo {

    /** 创建简略卡 UI（显示标签数量）。 */
    public static UIElement createSimplifiedUI(TaggedBankCard card) {
        return BankCardInfoUI.createSimplifiedCardUI(card, ui -> {
            BankCardInfoUI.addSimplifiedBase(ui, card);
            ui.addChild(BankCardInfoUI.addSimplifiedLine(Component.translatable("ui.bank.card.detail.tag_count", card.getTags().size())));
        });
    }

    /** 创建详情卡 UI（列出全部标签）。 */
    public static UIElement createDetailsUI(Player player, TaggedBankCard card) {
        return BankCardInfoUI.createDetailsCardUI(card, ui -> {
            BankCardInfoUI.addDetailBase(player, ui, card);
            if (card.getTags().isEmpty()) {
                ui.addChild(BankCardInfoUI.addDetailLine(Component.translatable("ui.bank.card.detail.tags.empty")));
            } else {
                String tags = card.getTags().stream()
                        .map(Identifier::toString)
                        .sorted()
                        .collect(Collectors.joining(", "));
                ui.addChild(BankCardInfoUI.addDetailLine(Component.translatable("ui.bank.card.detail.tags", tags)));
            }
        });
    }

    private TaggedBankCardInfo() {}
}

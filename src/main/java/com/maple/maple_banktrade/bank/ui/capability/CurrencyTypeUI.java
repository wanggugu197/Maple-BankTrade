package com.maple.maple_banktrade.bank.ui.capability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.bank.data.CurrencyType;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 货币余额展示 UI 构建工具。
 */
public class CurrencyTypeUI {

    // ==============================================
    // UI 显示
    // ==============================================

    /** 为支持货币存储的卡添加全部货币余额行。 */
    public static void addCurrencyUI(UIElement ui, BankCard card, float scale) {
        if (!(card instanceof CurrencyStorageBankCard currencyCard)) return;
        for (Identifier currencyId : currencyCard.getSupportedCurrencyIds()) {
            ui.addChild(createCurrencyUIElement(currencyId, currencyCard, scale));
        }
    }

    /** 创建单行货币图标、名称与绑定余额标签。 */
    public static UIElement createCurrencyUIElement(Identifier typeId, CurrencyStorageBankCard currencyCard, float scale) {
        CurrencyType currencyType = CurrencyType.findById(typeId).orElse(null);
        if (currencyType == null) return null;
        UIElement currency = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.ROW))
                .addChild(new UIElement()
                        .style(s -> s.background(currencyType.currencyTexture()))
                        .layout(l -> l.height(8 * scale).width(8 * scale)))
                .addChild(new TextElement()
                        .setText(Component.translatable(CurrencyType.getTranslationKey(currencyType.id())))
                        .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(9 * scale))
                        .layout(l -> l.heightAuto().widthAuto())
                        .style(s -> s.tooltips(currencyType.description().toArray(new Component[0]))));

        Label label = new Label();
        label.setValue(Component.literal(currencyCard.getCurrencyBalanceAsString(typeId)))
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(9 * scale))
                .layout(l -> l.heightAuto().widthAuto());
        label.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(currencyCard.getCurrencyBalanceAsString(typeId))).build());

        return new UIElement()
                .style(s -> s
                        .background(currencyType.backgroundTexture()))
                .layout(l -> l
                        .flexDirection(FlexDirection.ROW)
                        .paddingHorizontal(4).paddingVertical(2)
                        .justifyContent(AlignContent.SPACE_BETWEEN))
                .addChild(currency)
                .addChild(label);
    }
}

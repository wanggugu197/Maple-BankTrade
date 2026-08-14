package com.maple.maple_banktrade.api.bank.ui.capability;

import net.minecraft.network.chat.Component;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.bank.data.InfoList;
import com.maple.maple_banktrade.api.bank.ui.BankCardInfoUI;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import dev.vfyjxf.taffy.style.*;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 信息记录展示 UI 构建工具。
 */
public class InfoListUI {

    private static final IGuiTexture progress_bottom = SpriteTexture.of("maple_banktrade:textures/gui/tagged_card/progress_bottom.png");
    private static final IGuiTexture progress_top = SpriteTexture.of("maple_banktrade:textures/gui/tagged_card/progress_top.png");

    private static IGuiTexture getProgressTexture(int progress) {
        if (progress < 0) progress = 0;
        if (progress > 100) progress = 100;
        return GuiTextureGroup.of(
                progress_bottom,
                SpriteTexture.of("maple_banktrade:textures/gui/tagged_card/progress/sector_" + progress + ".png"),
                progress_top);
    }

    private static final Map<Short, IGuiTexture> TIER_TEXTURES = new LinkedHashMap<>();
    static {
        TIER_TEXTURES.put((short) 0, new ColorRectTexture(0xff808080));
        TIER_TEXTURES.put((short) 1, new ColorRectTexture(0xffb87333));
        TIER_TEXTURES.put((short) 2, new ColorRectTexture(0xffc0c0c0));
        TIER_TEXTURES.put((short) 3, new ColorRectTexture(0xffffd700));
        TIER_TEXTURES.put((short) 4, new ColorRectTexture(0xffb9f2ff));
        TIER_TEXTURES.put((short) 5, new ColorRectTexture(0xff50c878));
        TIER_TEXTURES.put((short) 6, new ColorRectTexture(0xff4a4a4a));
        TIER_TEXTURES.put((short) 7, new ColorRectTexture(0xffb374c6));
        TIER_TEXTURES.put((short) 8, IGuiTexture.dynamic(() -> new ColorRectTexture(Color.HSBtoRGB((System.currentTimeMillis() % 5000) / 5000f, 1.0f, 1.0f))));
    }

    public static IGuiTexture getTierTexture(short tier) {
        IGuiTexture texture = TIER_TEXTURES.get(tier);
        return texture != null ? texture : TIER_TEXTURES.get((short) 0);
    }

    private static final IGuiTexture bottom = new ColorRectTexture(0x80ababab);

    public static void addTaggedSimplifiedUIElement(UIElement ui, TaggedBankCard taggedCard, float scale) {
        InfoList infoList = taggedCard.getInfoList();
        if (infoList == null) {
            ui.addChild(BankCardInfoUI.addDetailLine(Component.translatable("ui.bank.card.detail.info_list.missing")));
            return;
        }

        var root = new UIElement()
                .layout(l -> l
                        .flexDirection(FlexDirection.ROW)
                        .paddingHorizontal(4).paddingVertical(2))
                .style(s -> s.background(infoList.backgroundTexture()));

        var proportion = new UIElement()
                .layout(l -> l.gapAll(1 * scale).paddingAll(2 * scale));

        var display = new UIElement()
                .layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(12 * scale))
                .style(s -> s.appendTooltips(infoList.description().toArray(new Component[0])));

        var progress = new UIElement()
                .layout(l -> l.height(24 * scale).width(24 * scale).justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER))
                .style(s -> s.background(getProgressTexture(taggedCard.getCompletionRatio())));
        var percentageValue = new Label();
        percentageValue.setText(String.valueOf(taggedCard.getCompletionRatio()))
                .textStyle(s -> s.fontSize(10 * scale).textShadow(false).adaptiveWidth(true).adaptiveHeight(true));
        percentageValue.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(String.valueOf(taggedCard.getCompletionRatio()))).build());
        progress.addChild(percentageValue);

        var quantityRatio = new Label();
        quantityRatio.setText(taggedCard.getCompletedEntries() + "/" + taggedCard.getTotalEntries())
                .textStyle(s -> s.fontSize(7).textShadow(false).textColor(0xFF000000).adaptiveWidth(true).adaptiveHeight(true));
        quantityRatio.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(taggedCard.getCompletedEntries() + "/" + taggedCard.getTotalEntries())).build());

        display.addChildren(progress, quantityRatio);
        proportion.addChildren(display);

        List<Short> tiers = taggedCard.getSortedTiers();
        int baseTireWidth = (tiers.getFirst() + 1) * 5;
        if (baseTireWidth < 20) baseTireWidth = 20;
        for (Short tier : tiers) {
            proportion.addChild(createTireCountUIElement(taggedCard, scale, tier, baseTireWidth));
        }

        root.addChild(proportion);

        ui.addChild(root);
    }

    public static UIElement createTireCountUIElement(TaggedBankCard taggedCard, float scale, short tier, int baseTireWidth) {
        var base = new UIElement()
                .layout(l -> l
                        .height(7 * scale).width((baseTireWidth + 30) * scale)
                        .justifyContent(AlignContent.SPACE_BETWEEN)
                        .flexDirection(FlexDirection.ROW))
                .style(s -> s.background(bottom));

        var deepen = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW))
                .addChild(new UIElement()
                        .layout(l -> l.height(7 * scale).width(baseTireWidth * scale))
                        .style(s -> s.background(getTierTexture(tier)))
                        .addChild(new TextElement()
                                .setText("⭐".repeat(tier))
                                .textStyle(s -> s.fontSize(5 * scale)
                                        .textShadow(false)
                                        .textAlignHorizontal(Horizontal.CENTER)
                                        .textAlignVertical(Vertical.CENTER))
                                .layout(l -> l.height(7 * scale))))
                .addChild(new UIElement()
                        .layout(l -> l.height(5 * scale).width(5 * scale))
                        .style(s -> s.background(getTierTexture(tier))
                                .transform2D(new Transform2D()
                                        .rotation(45)
                                        .pivot(0, 0))));

        var count = new Label();
        count.setText(taggedCard.getCompletedEntriesByTier(tier) + "/" + taggedCard.getTotalEntriesByTier(tier))
                .textStyle(s -> s.fontSize(5 * scale)
                        .textShadow(false)
                        .adaptiveWidth(true)
                        .textAlignHorizontal(Horizontal.CENTER)
                        .textAlignVertical(Vertical.CENTER));
        count.layout(l -> l.height(7 * scale));
        count.bind(DataBindingBuilder.componentS2C(
                () -> Component.literal(taggedCard.getCompletedEntriesByTier(tier) + "/" + taggedCard.getTotalEntriesByTier(tier))).build());

        base.addChildren(deepen, count);
        return base;
    }

    public static void addInfoEntryUI(UIElement ui, TaggedBankCard card) {
        InfoList infoList = card.getInfoList();
        if (infoList == null) {
            ui.addChild(BankCardInfoUI.addDetailLine(Component.translatable("ui.bank.card.detail.info_list.missing")));
            return;
        }

        var root = new UIElement()
                .layout(l -> l.paddingAll(4).gapAll(1))
                .style(s -> s.background(infoList.backgroundTexture()));

        // 遍历每个条目并显示其进度
        for (Map.Entry<String, InfoList.InfoEntry> entry : infoList.entries().entrySet()) {
            String entryId = entry.getKey();
            InfoList.InfoEntry info = entry.getValue();
            int progress = card.getProgress(entryId);
            int required = info.requiredCount();
            boolean complete = progress >= required;

            var entryUI = new UIElement()
                    .layout(l -> l.flexDirection(FlexDirection.ROW).justifyContent(AlignContent.SPACE_BETWEEN));
            entryUI.addChild(new TextElement()
                    .setText(info.display())
                    .textStyle(style -> style
                            .textWrap(TextWrap.WRAP)
                            .textAlignVertical(Vertical.CENTER)
                            .adaptiveHeight(true))
                    .style(s -> s.appendTooltips(Component.nullToEmpty("⭐".repeat(info.tire()))))
                    .layout(l -> l.flexGrow(1)));

            var label = new Label();
            label.setText(Component.empty()
                    .append(Component.literal(card.getProgress(entryId) + "/" + info.requiredCount()))
                    .append(" ")
                    .append(complete ?
                            Component.translatable("ui.bank.card.detail.complete") :
                            Component.translatable("ui.bank.card.detail.incomplete")))
                    .textStyle(style -> style
                            .textAlignVertical(Vertical.CENTER)
                            .adaptiveHeight(true)
                            .adaptiveWidth(true));
            label.bind(DataBindingBuilder.componentS2C(
                    () -> {
                        int progressS = card.getProgress(entryId);
                        int requiredS = info.requiredCount();
                        boolean completeS = progress >= required;
                        return Component.empty()
                                .append(Component.literal(progressS + "/" + requiredS))
                                .append(" ")
                                .append(completeS ?
                                        Component.translatable("ui.bank.card.detail.complete") :
                                        Component.translatable("ui.bank.card.detail.incomplete"));
                    }).build());
            entryUI.addChild(label);
            root.addChild(entryUI);
        }
        ui.addChild(root);
    }
}

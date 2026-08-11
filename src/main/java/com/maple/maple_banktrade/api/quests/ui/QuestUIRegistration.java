package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import com.maple.maple_banktrade.MapleBankTrade;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 任务 UI 注册入口 —— 绑定到 {@link PlayerUIMenuType}，提供三标签页任务书界面。
 */
public class QuestUIRegistration extends PlayerUIMenuType {

    public static final ResourceLocation QUEST_UI = MapleBankTrade.id("quest_book");

    public static void init() {
        PlayerUIMenuType.register(QUEST_UI, player -> {
            var root = new UIElement();
            root.layout(l -> l.width(320).height(200)
                    .flexDirection(FlexDirection.COLUMN).gapAll(0));
            root.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);

            var tabView = new TabView();
            tabView.tabHeaderContainer.layout(l -> l.width(320).height(14));
            tabView.tabContentContainer.layout(l -> l.width(320).height(186)
                    .paddingAll(0));
            tabView.tabContentContainer.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);

            tabView.addTab(buildMainQuestTab(player));
            tabView.addTab(buildCompletedTab(player));
            tabView.addTab(buildTreeTab());

            root.addChild(tabView);
            return p -> ModularUI.of(root, p);
        });
    }

    /** 打开任务书 UI（服务端调用）。 */
    public static void openUI(ServerPlayer player) {
        PlayerUIMenuType.openUI(player, QUEST_UI);
    }

    // ==============================================
    // 标签页构建
    // ==============================================

    private static Tab buildMainQuestTab(Player player) {
        var split = new SplitView.Horizontal();
        split.setPercentage(30f);
        split.setBorderSize(2);
        split.setMinPercentage(20f);
        split.setMaxPercentage(50f);

        var detailCtx = QuestTaskDetailPanel.create(player);
        var listCtx = QuestTaskListPanel.create(player);

        listCtx.scroller().onMessage(QuestTaskListPanel.MSG_SELECT_TASK, payload -> {
            String taskId = payload.getStringOr(QuestTaskListPanel.KEY_TASK_ID, "");
            detailCtx.selectTask(taskId);
        });

        // 左栏内联样式：background:empty, padding-all:2
        listCtx.panel().set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        listCtx.panel().layout(l -> l.paddingAll(2));

        // 右栏内联样式：background:empty, padding-all:4
        detailCtx.panel().set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        detailCtx.panel().layout(l -> l.paddingAll(4));

        split.left(listCtx.panel());
        split.right(detailCtx.panel());
        return new Tab(split, "tab_quests");
    }

    private static Tab buildCompletedTab(Player player) {
        return new Tab(QuestCompletedPanel.create(player), "tab_completed");
    }

    private static Tab buildTreeTab() {
        return new Tab(QuestTreePanel.create(), "tab_tree");
    }
}
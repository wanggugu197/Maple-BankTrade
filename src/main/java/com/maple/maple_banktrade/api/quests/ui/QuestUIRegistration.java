package com.maple.maple_banktrade.api.quests.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.maple.maple_banktrade.MapleBankTrade;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 任务 UI 注册入口 —— 绑定到 {@link PlayerUIMenuType}，提供三标签页任务书界面。
 * <p>
 * 所有布局参数集中为静态常量，避免魔法数字散落。
 */
public class QuestUIRegistration extends PlayerUIMenuType {

    public static final Identifier QUEST_UI = MapleBankTrade.id("quest_book");

    // ==============================================
    // 布局常量
    // ==============================================
    public static final int UI_WIDTH = 360;
    public static final int UI_HEIGHT = 200;
    public static final int UI_CONTENT_HEIGHT = 188;
    public static final int TAB_HEADER_HEIGHT = 14;
    public static final int CONTENT_PADDING = 4;
    public static final float SPLIT_PERCENTAGE = 30f;
    public static final float SPLIT_MIN_PERCENTAGE = 20f;
    public static final float SPLIT_MAX_PERCENTAGE = 80f;
    public static final float SPLIT_BORDER_SIZE = 2f;
    public static final float FONT_DETAIL = 7f;
    public static final float FONT_TITLE = 10f;
    public static final int COLOR_DIM = 0xFFAAAAAA;
    public static final int ROW_HEIGHT = 10;
    public static final int TITLE_ROW_HEIGHT = 14;
    public static final int ACTION_BAR_HEIGHT = 16;
    public static final int GAP_SMALL = 2;
    public static final int GAP_MEDIUM = 4;
    public static final int PADDING_TINY = 2;
    public static final int PADDING_SMALL = 2;
    public static final int TREE_MARGIN_LEFT = 6;

    // ==============================================
    // 注册
    // ==============================================

    public static void init() {
        PlayerUIMenuType.register(QUEST_UI, player -> {
            var root = new UIElement();
            root.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(0));
            root.style(s -> s.background(IGuiTexture.EMPTY));

            var tabView = new TabView();
            tabView.tabHeaderContainer.layout(l -> l.width(UI_WIDTH).height(TAB_HEADER_HEIGHT));
            tabView.tabContentContainer.layout(l -> l.width(UI_WIDTH).height(UI_HEIGHT)
                    .paddingAll(CONTENT_PADDING));

            tabView.addTab(buildMainQuestTab(), buildMainQuestContent(player));
            tabView.addTab(buildCompletedTab(), buildCompletedContent(player));
            tabView.addTab(buildTreeTab(), buildTreeContent(player));

            root.addChild(tabView);
            return p -> ModularUI.of(UI.of(root, StylesheetManager.GDP_MERGED), p);
        });
    }

    /** 打开任务书 UI（服务端调用）。 */
    public static void openUI(ServerPlayer player) {
        PlayerUIMenuType.openUI(player, QUEST_UI);
    }

    // ==============================================
    // 分栏构建工具
    // ==============================================

    /** 创建统一的分栏容器（左 30% / 右 70%）。 */
    private static SplitView.Horizontal createSplitView() {
        var split = new SplitView.Horizontal();
        split.setPercentage(SPLIT_PERCENTAGE);
        split.setBorderSize(SPLIT_BORDER_SIZE);
        split.setMinPercentage(SPLIT_MIN_PERCENTAGE);
        split.setMaxPercentage(SPLIT_MAX_PERCENTAGE);
        return split;
    }

    // ==============================================
    // 标签页构建 —— Tab 头部
    // ==============================================

    private static Tab buildMainQuestTab() {
        var tab = new Tab();
        tab.setText(Component.translatable("ui.maple_banktrade.quest.tab_quests"));
        return tab;
    }

    private static Tab buildCompletedTab() {
        var tab = new Tab();
        tab.setText(Component.translatable("ui.maple_banktrade.quest.tab_completed"));
        return tab;
    }

    private static Tab buildTreeTab() {
        var tab = new Tab();
        tab.setText(Component.translatable("ui.maple_banktrade.quest.tab_tree"));
        return tab;
    }

    // ==============================================
    // 标签页构建 —— 内容
    // ==============================================

    private static UIElement buildMainQuestContent(Player player) {
        var split = createSplitView();

        var listCtx = QuestTaskListPanel.create(player);
        var detailCtx = QuestTaskDetailPanel.create(player, true);

        listCtx.scroller().onMessage(QuestTaskListPanel.MSG_SELECT_TASK, payload -> {
            String taskId = payload.getString(QuestTaskListPanel.KEY_TASK_ID).get();
            detailCtx.selectTask(taskId);
        });

        listCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        listCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        detailCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        detailCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        split.left(listCtx.panel());
        split.right(detailCtx.panel());
        return split;
    }

    private static UIElement buildCompletedContent(Player player) {
        var split = createSplitView();

        var completedCtx = QuestCompletedPanel.create(player);
        var detailCtx = QuestTaskDetailPanel.create(player, true);

        completedCtx.scroller().onMessage(QuestTaskListPanel.MSG_SELECT_TASK, payload -> {
            String taskId = payload.getString(QuestTaskListPanel.KEY_TASK_ID).get();
            detailCtx.selectTask(taskId);
        });

        completedCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        completedCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        detailCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        detailCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        split.left(completedCtx.panel());
        split.right(detailCtx.panel());
        return split;
    }

    private static UIElement buildTreeContent(Player player) {
        var split = createSplitView();

        var treeCtx = QuestTreePanel.create();
        var detailCtx = QuestTaskDetailPanel.create(player, false);

        treeCtx.scroller().onMessage(QuestTaskListPanel.MSG_SELECT_TASK, payload -> {
            String taskId = payload.getString(QuestTaskListPanel.KEY_TASK_ID).get();
            detailCtx.selectTask(taskId);
        });

        treeCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        treeCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        detailCtx.panel().style(s -> s.background(IGuiTexture.EMPTY));
        detailCtx.panel().layout(l -> l.paddingAll(PADDING_SMALL));

        split.left(treeCtx.panel());
        split.right(detailCtx.panel());
        return split;
    }
}

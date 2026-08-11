package com.maple.maple_banktrade.api.quests.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry;
import com.maple.maple_banktrade.api.quests.calculator.StateTransitionOrchestrator;
import com.maple.maple_banktrade.api.quests.condition.EvaluationContext;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;
import com.maple.maple_banktrade.api.quests.storage.QuestDataManager;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.List;

/**
 * 右栏任务详情面板 —— 服务端仅发送状态快照，客户端基于本地蓝图构建详情。
 */
public final class QuestTaskDetailPanel {

    private QuestTaskDetailPanel() {}

    // ==============================================
    // 构建
    // ==============================================

    public static DetailContext create(Player player, boolean showActions) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(QuestUIRegistration.GAP_MEDIUM));
        panel.style(s -> s.background(IGuiTexture.EMPTY));

        ScrollerView scroller = new ScrollerView();
        scroller.layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(0));
        scroller.style(s -> s.background(IGuiTexture.EMPTY));
        scroller.scrollerStyle(s -> s
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        scroller.viewPort(port -> {
            port.style(s -> s.background(IGuiTexture.EMPTY));
            port.layout(l -> l.paddingAll(0));
        });
        scroller.viewContainer(container -> container.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(QuestUIRegistration.GAP_SMALL + 1)
                .paddingAll(QuestUIRegistration.PADDING_TINY)));

        QuestSyncData.QuestStatusSnapshot data = player instanceof ServerPlayer sp ? QuestUiHelper.buildStatusSnapshot(sp) : QuestSyncData.QuestStatusSnapshot.empty();
        DetailContext ctx = new DetailContext(panel, scroller, player);

        Button activateBtn = null, completeBtn = null, claimBtn = null;
        if (showActions) {
            UIElement actions = new UIElement();
            actions.layout(l -> l.widthPercent(100).height(QuestUIRegistration.ACTION_BAR_HEIGHT)
                    .flexDirection(FlexDirection.ROW)
                    .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER)
                    .gapAll(QuestUIRegistration.GAP_MEDIUM).paddingAll(QuestUIRegistration.PADDING_TINY));
            actions.style(s -> s.background(IGuiTexture.EMPTY));

            activateBtn = new Button()
                    .setText(Component.translatable("ui.maple_banktrade.quest.activate"));
            activateBtn.setOnServerClick(_ -> {
                if (player instanceof ServerPlayer sp) activateTask(sp, ctx);
            });
            activateBtn.setDisplay(false);

            completeBtn = new Button()
                    .setText(Component.translatable("ui.maple_banktrade.quest.complete"));
            completeBtn.setOnServerClick(_ -> {
                if (player instanceof ServerPlayer sp) completeTask(sp, ctx);
            });
            completeBtn.setDisplay(false);

            claimBtn = new Button()
                    .setText(Component.translatable("ui.maple_banktrade.quest.claim_reward"));
            claimBtn.setOnServerClick(_ -> {
                if (player instanceof ServerPlayer sp) claimReward(sp, ctx);
            });
            claimBtn.setDisplay(false);

            actions.addChildren(activateBtn, completeBtn, claimBtn);
            ctx.setButtons(activateBtn, completeBtn, claimBtn);
            panel.addChild(actions);
        }

        panel.addChild(scroller);

        BindableValue<QuestSyncData.QuestStatusSnapshot> sync = createStatusSync(player, ctx, scroller, data, activateBtn, completeBtn, claimBtn);
        panel.addChild(sync);
        return ctx;
    }

    private static BindableValue<QuestSyncData.QuestStatusSnapshot> createStatusSync(
                                                                                     Player player, DetailContext ctx, ScrollerView scroller,
                                                                                     QuestSyncData.QuestStatusSnapshot data,
                                                                                     Button activateBtn, Button completeBtn, Button claimBtn) {
        BindableValue<QuestSyncData.QuestStatusSnapshot> sync = new BindableValue<>(data);
        sync.layout(l -> l.width(0).height(0));
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.create(
                () -> QuestUiHelper.buildStatusSnapshot((ServerPlayer) player),
                v -> { /* c2s no-op */ })
                .syncType(QuestSyncData.QuestStatusSnapshot.class)
                .initialValue(data)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(clientData -> {
                    ctx.setCachedStatus(clientData);
                    rebuildDetailFromStatus(scroller, ctx);
                    if (activateBtn != null) updateButtonsFromStatus(activateBtn, completeBtn, claimBtn, ctx);
                })
                .build());
        return sync;
    }

    // ==============================================
    // 客户端：基于状态快照 + 本地蓝图重建详情
    // ==============================================

    private static void rebuildDetailFromStatus(ScrollerView scroller, DetailContext ctx) {
        scroller.clearAllScrollViewChildren();
        String taskId = ctx.selectedTaskId();
        if (taskId == null || taskId.isEmpty()) {
            scroller.addScrollViewChild(buildEmptyHint());
            return;
        }

        ITaskDefinition def = QuestDefinitionRegistry.getDefinition(taskId);
        if (def == null) {
            scroller.addScrollViewChild(buildEmptyHint());
            return;
        }

        QuestSyncData.QuestStatusSnapshot snapshot = ctx.getCachedStatus();
        QuestSyncData.TaskStatusEntry entry = snapshot != null ? snapshot.getEntry(taskId) : null;
        String statusStr = entry != null ? entry.getStatus() : "HIDDEN";
        int completions = entry != null ? entry.getCompletions() : 0;
        boolean hasUnclaimed = entry != null && entry.isHasUnclaimedReward();

        TaskStatus status;
        try {
            status = TaskStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            status = TaskStatus.HIDDEN;
        }

        // 标题
        scroller.addScrollViewChild(buildTitle(taskId));

        // 基本信息
        scroller.addScrollViewChild(buildSection("基本信息"));
        scroller.addScrollViewChild(buildRow("类型", QuestUiHelper.formatTaskType(def.getType())));
        scroller.addScrollViewChild(buildRow("行为", QuestUiHelper.formatBehavior(def.getBehavior())));
        scroller.addScrollViewChild(buildRow("状态", QuestUiHelper.formatStatus(status)));
        scroller.addScrollViewChild(buildRow("进度", completions + " / " + def.getRequiredCompletions()));

        // 任务链
        scroller.addScrollViewChild(buildSection("任务链"));
        if (def.getParentId() != null) scroller.addScrollViewChild(buildRow("父节点", def.getParentId()));
        if (def.getPrevSiblingId() != null) scroller.addScrollViewChild(buildRow("前置", def.getPrevSiblingId()));
        if (def.getNextTaskInChain() != null) scroller.addScrollViewChild(buildRow("后继", def.getNextTaskInChain()));
        if (def.isRepeatable()) {
            scroller.addScrollViewChild(buildRow("循环", def.getMaxRepeatTimes() < 0 ? "无限" : "最多 " + def.getMaxRepeatTimes() + " 次"));
        }

        // 依赖
        if (!def.getDependentNodes().isEmpty()) {
            scroller.addScrollViewChild(buildSection("依赖 (" + QuestUiHelper.formatDependencyRequirement(def.getDependencyRequirement().name()) + ")"));
            for (String depId : def.getDependentNodes()) {
                QuestSyncData.TaskStatusEntry depEntry = snapshot != null ? snapshot.getEntry(depId) : null;
                String depStatus = depEntry != null ? depEntry.getStatus() : "HIDDEN";
                String depIcon = "";
                try {
                    depIcon = QuestUiHelper.statusIcon(TaskStatus.valueOf(depStatus));
                } catch (IllegalArgumentException ignored) {}
                scroller.addScrollViewChild(buildRow(depId, depIcon + " " + depStatus));
            }
        }

        // 条件
        if (def.getUnlockCondition() != null || def.getVisibilityCondition() != null) {
            scroller.addScrollViewChild(buildSection("条件"));
            if (def.getUnlockCondition() != null) scroller.addScrollViewChild(buildRow("解锁条件", def.getUnlockCondition().toString()));
            if (def.getVisibilityCondition() != null) scroller.addScrollViewChild(buildRow("可见条件", def.getVisibilityCondition().toString()));
        }

        // 完成方式
        if (!def.getTaskTypes().isEmpty()) {
            scroller.addScrollViewChild(buildSection("完成方式"));
            List<String> names = def.getTaskTypes().stream().map(t -> t.getClass().getSimpleName()).toList();
            scroller.addScrollViewChild(buildRow("类型", String.join(", ", names)));
        }

        // 奖励
        if (!def.getRewards().isEmpty()) {
            scroller.addScrollViewChild(buildSection("奖励"));
            for (var reward : def.getRewards()) {
                scroller.addScrollViewChild(buildRow("物品", reward.toString()));
            }
        }
    }

    /** 根据状态快照更新按钮显示。 */
    private static void updateButtonsFromStatus(Button activateBtn, Button completeBtn, Button claimBtn, DetailContext ctx) {
        String taskId = ctx.selectedTaskId();
        if (taskId == null || taskId.isEmpty()) {
            activateBtn.setDisplay(false);
            completeBtn.setDisplay(false);
            claimBtn.setDisplay(false);
            return;
        }
        QuestSyncData.QuestStatusSnapshot snapshot = ctx.getCachedStatus();
        QuestSyncData.TaskStatusEntry entry = snapshot != null ? snapshot.getEntry(taskId) : null;
        String statusStr = entry != null ? entry.getStatus() : "HIDDEN";
        boolean hasUnclaimed = entry != null && entry.isHasUnclaimedReward();

        switch (statusStr) {
            case "VISIBLE_LOCKED" -> {
                activateBtn.setDisplay(true);
                completeBtn.setDisplay(false);
                claimBtn.setDisplay(false);
            }
            case "ACTIVE" -> {
                activateBtn.setDisplay(false);
                completeBtn.setDisplay(true);
                claimBtn.setDisplay(false);
            }
            case "COMPLETED" -> {
                activateBtn.setDisplay(false);
                completeBtn.setDisplay(false);
                claimBtn.setDisplay(hasUnclaimed);
            }
            default -> {
                activateBtn.setDisplay(false);
                completeBtn.setDisplay(false);
                claimBtn.setDisplay(false);
            }
        }
    }

    // ==============================================
    // 服务端操作
    // ==============================================

    private static void activateTask(ServerPlayer player, DetailContext ctx) {
        String taskId = ctx.selectedTaskId();
        if (taskId == null || taskId.isEmpty()) return;
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        EvaluationContext evalCtx = QuestDataManager.getEvaluator(player);
        ResolutionContext resCtx = new ResolutionContext(data, evalCtx, data.getAllStates());
        StateTransitionOrchestrator.processActivate(taskId, resCtx);
    }

    private static void completeTask(ServerPlayer player, DetailContext ctx) {
        String taskId = ctx.selectedTaskId();
        if (taskId == null || taskId.isEmpty()) return;
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        EvaluationContext evalCtx = QuestDataManager.getEvaluator(player);
        ResolutionContext resCtx = new ResolutionContext(data, evalCtx, data.getAllStates());
        long gameTime = player.level().getGameTime();
        StateTransitionOrchestrator.processComplete(taskId, resCtx, gameTime, player);
    }

    private static void claimReward(ServerPlayer player, DetailContext ctx) {
        String taskId = ctx.selectedTaskId();
        if (taskId == null || taskId.isEmpty()) return;
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        EvaluationContext evalCtx = QuestDataManager.getEvaluator(player);
        ResolutionContext resCtx = new ResolutionContext(data, evalCtx, data.getAllStates());
        List<ICompletionRecord> records = data.getCompletionRecords(taskId);
        for (int i = records.size() - 1; i >= 0; i--) {
            if (!records.get(i).isRewardClaimed()) {
                StateTransitionOrchestrator.claimRewards(taskId, i + 1, resCtx, player);
                break;
            }
        }
    }

    // ==============================================
    // UI 构建子组件
    // ==============================================

    private static UIElement buildTitle(String taskId) {
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).height(QuestUIRegistration.TITLE_ROW_HEIGHT)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .paddingHorizontal(QuestUIRegistration.PADDING_TINY));
        row.style(s -> s.background(IGuiTexture.EMPTY));

        TextElement title = new TextElement().setText(Component.literal(formatName(taskId)));
        title.textStyle(s -> s
                .fontSize(QuestUIRegistration.FONT_TITLE)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false)
                .adaptiveWidth(true)
                .adaptiveHeight(true));
        row.addChild(title);
        return row;
    }

    private static UIElement buildSection(String label) {
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).height(QuestUIRegistration.ROW_HEIGHT)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
        row.style(s -> s.background(IGuiTexture.EMPTY));

        TextElement text = new TextElement().setText(Component.literal("— " + label + " —"));
        text.textStyle(s -> s
                .fontSize(QuestUIRegistration.FONT_DETAIL)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false)
                .textColor(QuestUIRegistration.COLOR_DIM)
                .adaptiveWidth(true)
                .adaptiveHeight(true));
        row.addChild(text);
        return row;
    }

    private static UIElement buildRow(String label, String value) {
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).height(QuestUIRegistration.ROW_HEIGHT)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .gapAll(QuestUIRegistration.GAP_MEDIUM).paddingHorizontal(QuestUIRegistration.PADDING_TINY));
        row.style(s -> s.background(IGuiTexture.EMPTY));

        TextElement lt = new TextElement().setText(Component.literal(label + ":"));
        applyDetailTextStyle(lt);
        TextElement vt = new TextElement().setText(Component.literal(value));
        applyDetailTextStyle(vt);
        row.addChildren(lt, vt);
        return row;
    }

    private static UIElement buildEmptyHint() {
        UIElement row = new UIElement();
        row.layout(l -> l.widthPercent(100).height(QuestUIRegistration.TITLE_ROW_HEIGHT)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .paddingHorizontal(QuestUIRegistration.PADDING_TINY));
        row.style(s -> s.background(IGuiTexture.EMPTY));

        TextElement hint = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.select_hint"));
        hint.textStyle(s -> s
                .fontSize(QuestUIRegistration.FONT_TITLE)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false)
                .adaptiveWidth(true)
                .adaptiveHeight(true));
        row.addChild(hint);
        return row;
    }

    private static void applyDetailTextStyle(TextElement text) {
        text.textStyle(s -> s
                .fontSize(QuestUIRegistration.FONT_DETAIL)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(false)
                .adaptiveWidth(true)
                .adaptiveHeight(true));
    }

    private static String formatName(String taskId) {
        if (taskId == null || taskId.isEmpty()) return "???";
        String[] parts = taskId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    // ==============================================
    // 内部类型
    // ==============================================

    public static final class DetailContext {

        private final UIElement panel;
        private final ScrollerView scroller;
        private final Player player;
        private String selectedTaskId;
        private QuestSyncData.QuestStatusSnapshot cachedStatus;
        private Button activateBtn;
        private Button completeBtn;
        private Button claimBtn;

        private DetailContext(UIElement panel, ScrollerView scroller, Player player) {
            this.panel = panel;
            this.scroller = scroller;
            this.player = player;
            this.selectedTaskId = null;
        }

        public UIElement panel() {
            return panel;
        }

        public ScrollerView scroller() {
            return scroller;
        }

        public String selectedTaskId() {
            return selectedTaskId;
        }

        public Button activateBtn() {
            return activateBtn;
        }

        public Button completeBtn() {
            return completeBtn;
        }

        public Button claimBtn() {
            return claimBtn;
        }

        /** 客户端缓存的最近一次状态快照。 */
        public QuestSyncData.QuestStatusSnapshot getCachedStatus() {
            return cachedStatus;
        }

        void setCachedStatus(QuestSyncData.QuestStatusSnapshot status) {
            this.cachedStatus = status;
        }

        void setButtons(Button activate, Button complete, Button claim) {
            this.activateBtn = activate;
            this.completeBtn = complete;
            this.claimBtn = claim;
        }

        public void selectTask(String taskId) {
            this.selectedTaskId = taskId;
            // 选择后立即用缓存的状态重建详情
            if (cachedStatus != null) {
                rebuildDetailFromStatus(scroller, this);
                if (activateBtn != null) updateButtonsFromStatus(activateBtn, completeBtn, claimBtn, this);
            }
        }
    }
}

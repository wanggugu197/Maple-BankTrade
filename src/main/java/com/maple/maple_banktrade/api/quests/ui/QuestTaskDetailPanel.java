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
import com.maple.maple_banktrade.api.quests.calculator.StateTransitionOrchestrator;
import com.maple.maple_banktrade.api.quests.condition.EvaluationContext;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;
import com.maple.maple_banktrade.api.quests.storage.QuestDataManager;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.List;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.UI_CONTENT_HEIGHT;

/**
 * 右栏任务详情面板 —— 显示选中任务的完整信息。
 */
public final class QuestTaskDetailPanel {

    private QuestTaskDetailPanel() {}

    // ==============================================
    // 构建
    // ==============================================

    /**
     * @param player      玩家实例
     * @param showActions 是否显示操作按钮（激活/完成/领取）。创造模式树状视图可传 false。
     */
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

        QuestSyncData.TaskDetail data = QuestSyncData.TaskDetail.empty();
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

        BindableValue<QuestSyncData.TaskDetail> sync;
        if (showActions) {
            final Button fActivateBtn = activateBtn;
            final Button fCompleteBtn = completeBtn;
            final Button fClaimBtn = claimBtn;
            sync = createDetailSync(player, ctx, scroller, data, fActivateBtn, fCompleteBtn, fClaimBtn);
        } else {
            sync = createDetailSync(player, ctx, scroller, data, null, null, null);
        }

        panel.addChild(sync);
        ctx.setSync(sync);
        return ctx;
    }

    private static BindableValue<QuestSyncData.TaskDetail> createDetailSync(
                                                                            Player player, DetailContext ctx, ScrollerView scroller,
                                                                            QuestSyncData.TaskDetail data,
                                                                            Button activateBtn, Button completeBtn, Button claimBtn) {
        BindableValue<QuestSyncData.TaskDetail> sync = new BindableValue<>(data);
        sync.layout(l -> l.width(0).height(0));
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.create(
                () -> {
                    String id = ctx.selectedTaskId();
                    if (id == null || id.isEmpty()) {
                        data.copyFrom(QuestSyncData.TaskDetail.empty());
                    } else {
                        data.copyFrom(QuestUiHelper.buildTaskDetail((ServerPlayer) player, id));
                    }
                    return data;
                },
                v -> { /* c2s no-op */ })
                .syncType(QuestSyncData.TaskDetail.class)
                .initialValue(data)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(clientData -> {
                    rebuildDetail(scroller, clientData);
                    if (activateBtn != null) updateButtons(activateBtn, completeBtn, claimBtn, clientData);
                })
                .build());
        return sync;
    }

    // ==============================================
    // 客户端重建
    // ==============================================

    private static void rebuildDetail(ScrollerView scroller, QuestSyncData.TaskDetail d) {
        scroller.clearAllScrollViewChildren();
        if (d.getId().isEmpty()) {
            scroller.addScrollViewChild(buildEmptyHint());
            return;
        }

        scroller.addScrollViewChild(buildTitle(d.getId()));
        scroller.addScrollViewChild(buildSection("基本信息"));
        scroller.addScrollViewChild(buildRow("类型", QuestUiHelper.formatTaskType(safeEnum(d.getType(), com.maple.maple_banktrade.api.quests.enums.TaskType.class))));
        scroller.addScrollViewChild(buildRow("行为", QuestUiHelper.formatBehavior(safeEnum(d.getBehavior(), com.maple.maple_banktrade.api.quests.enums.TaskBehavior.class))));
        scroller.addScrollViewChild(buildRow("状态", QuestUiHelper.formatStatus(safeEnum(d.getStatus(), TaskStatus.class))));
        scroller.addScrollViewChild(buildRow("进度", d.getCompletions() + " / " + d.getRequiredCompletions()));

        scroller.addScrollViewChild(buildSection("任务链"));
        if (!d.getParentId().isEmpty()) scroller.addScrollViewChild(buildRow("父节点", d.getParentId()));
        if (!d.getPrevSiblingId().isEmpty()) scroller.addScrollViewChild(buildRow("前置", d.getPrevSiblingId()));
        if (!d.getNextTaskInChain().isEmpty()) scroller.addScrollViewChild(buildRow("后继", d.getNextTaskInChain()));
        if (d.isRepeatable()) {
            scroller.addScrollViewChild(buildRow("循环", d.getMaxRepeatTimes() < 0 ? "无限" : "最多 " + d.getMaxRepeatTimes() + " 次"));
        }

        if (!d.getDependents().isEmpty()) {
            scroller.addScrollViewChild(buildSection("依赖 (" + QuestUiHelper.formatDependencyRequirement(d.getDependencyRequirement()) + ")"));
            for (QuestSyncData.DependentInfo dep : d.getDependents()) {
                String depIcon = "";
                try {
                    depIcon = QuestUiHelper.statusIcon(TaskStatus.valueOf(dep.getStatus()));
                } catch (IllegalArgumentException ignored) {}
                scroller.addScrollViewChild(buildRow(dep.getId(), depIcon + " " + dep.getStatus()));
            }
        }

        if (!d.getUnlockCond().isEmpty() || !d.getVisCond().isEmpty()) {
            scroller.addScrollViewChild(buildSection("条件"));
            if (!d.getUnlockCond().isEmpty()) scroller.addScrollViewChild(buildRow("解锁条件", d.getUnlockCond()));
            if (!d.getVisCond().isEmpty()) scroller.addScrollViewChild(buildRow("可见条件", d.getVisCond()));
        }

        if (!d.getTaskTypeInfos().isEmpty()) {
            scroller.addScrollViewChild(buildSection("完成方式"));
            scroller.addScrollViewChild(buildRow("类型", String.join(", ", d.getTaskTypeInfos())));
        }

        if (!d.getRewards().isEmpty()) {
            scroller.addScrollViewChild(buildSection("奖励"));
            for (QuestSyncData.RewardInfo r : d.getRewards()) {
                scroller.addScrollViewChild(buildRow("物品", r.getItem() + " ×" + r.getCount()));
            }
        }
    }

    private static void updateButtons(Button activateBtn, Button completeBtn, Button claimBtn, QuestSyncData.TaskDetail d) {
        TaskStatus status;
        try {
            status = TaskStatus.valueOf(d.getStatus());
        } catch (IllegalArgumentException e) {
            activateBtn.setDisplay(false);
            completeBtn.setDisplay(false);
            claimBtn.setDisplay(false);
            return;
        }
        switch (status) {
            case VISIBLE_LOCKED -> {
                activateBtn.setDisplay(true);
                completeBtn.setDisplay(false);
                claimBtn.setDisplay(false);
            }
            case ACTIVE -> {
                activateBtn.setDisplay(false);
                completeBtn.setDisplay(true);
                claimBtn.setDisplay(false);
            }
            case COMPLETED -> {
                activateBtn.setDisplay(false);
                completeBtn.setDisplay(false);
                claimBtn.setDisplay(d.isHasUnclaimedReward());
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
    // UI 构建子组件（全部使用 style() / layout() / textStyle() 内联样式）
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

    /** 详情行文字通用样式。 */
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

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E safeEnum(String value, Class<E> clazz) {
        try {
            return Enum.valueOf(clazz, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==============================================
    // 内部类型
    // ==============================================

    public static final class DetailContext {

        private final UIElement panel;
        private final ScrollerView scroller;
        private final Player player;
        private String selectedTaskId;
        private BindableValue<QuestSyncData.TaskDetail> sync;
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

        void setSync(BindableValue<QuestSyncData.TaskDetail> sync) {
            this.sync = sync;
        }

        void setButtons(Button activate, Button complete, Button claim) {
            this.activateBtn = activate;
            this.completeBtn = complete;
            this.claimBtn = claim;
        }

        public void selectTask(String taskId) {
            this.selectedTaskId = taskId;
        }
    }
}

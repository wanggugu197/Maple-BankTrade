package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.enums.TaskType;

import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 左栏任务列表面板 —— 使用 {@link TreeList} 组件按类型分组显示任务。
 */
public final class QuestTaskListPanel {

    public static final String MSG_SELECT_TASK = "select_task";
    public static final String KEY_TASK_ID = "task_id";

    private QuestTaskListPanel() {}

    public static TaskListContext create(Player player) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(2));
        panel.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);

        ScrollerView scroller = new ScrollerView();
        scroller.layout(l -> l.widthPercent(100).heightPercent(100).paddingAll(0));
        scroller.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        scroller.set(PropertyRegistry.SCROLLER_VIEW_MODE, ScrollerMode.VERTICAL);
        scroller.set(PropertyRegistry.SCROLLER_VERTICAL_DISPLAY, ScrollDisplay.NEVER);
        scroller.set(PropertyRegistry.SCROLLER_HORIZONTAL_DISPLAY, ScrollDisplay.NEVER);
        scroller.set(PropertyRegistry.SCROLLER_VIEW_MARGIN, 0f);
        scroller.viewPort(port -> {
            port.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
            port.layout(l -> l.paddingAll(0));
        });
        scroller.viewContainer(container ->
                container.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(0).paddingAll(0)));

        TreeList<QuestTreeNode> tree = new TreeList<>();
        tree.layout(l -> l.widthPercent(100).heightPercent(100).gapAll(1));
        tree.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        tree.set(PropertyRegistry.FONT_SIZE, 7f);
        tree.set(PropertyRegistry.TEXT_SHADOW, false);
        tree.setFlattenRoot(true);
        tree.setStaticTree(true);
        tree.setClickToExpand(true);
        tree.setNodeUISupplier(node -> buildNodeUI(node));

        tree.setOnSelectedChanged(selected -> {
            if (!selected.isEmpty()) {
                QuestTreeNode node = selected.iterator().next();
                if (!node.isGroup()) {
                    CompoundTag payload = new CompoundTag();
                    payload.putString(KEY_TASK_ID, node.getId());
                    scroller.sendMessage(MSG_SELECT_TASK, payload);
                }
            }
        });

        scroller.addScrollViewChild(tree);

        QuestSyncData.TreeListSnapshot data = player instanceof ServerPlayer sp
                ? QuestUiHelper.buildQuestTreeList(sp)
                : QuestSyncData.TreeListSnapshot.empty();
        TaskListContext ctx = new TaskListContext(panel, scroller, tree);

        BindableValue<QuestSyncData.TreeListSnapshot> sync = new BindableValue<>(data);
        sync.layout(l -> l.width(0).height(0));
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.create(
                () -> {
                    data.copyFrom(QuestUiHelper.buildQuestTreeList((ServerPlayer) player));
                    return data;
                },
                v -> { /* c2s no-op */ })
                .syncType(QuestSyncData.TreeListSnapshot.class)
                .initialValue(data)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(clientData -> rebuildTree(tree, clientData))
                .build());

        panel.addChild(scroller);
        panel.addChild(sync);
        return ctx;
    }

    private static void rebuildTree(TreeList<QuestTreeNode> tree, QuestSyncData.TreeListSnapshot snapshot) {
        for (QuestTreeNode root : snapshot.getRoots()) {
            root.reattachParents();
        }
        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode groupNode : snapshot.getRoots()) {
            dummyRoot.addChildNode(groupNode);
        }
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);
        for (QuestTreeNode groupNode : snapshot.getRoots()) {
            tree.expandNode(groupNode);
        }
    }

    private static UIElement buildNodeUI(QuestTreeNode node) {
        if (node.isGroup()) return buildGroupNodeUI(node);
        return buildTaskNodeUI(node);
    }

    private static UIElement buildGroupNodeUI(QuestTreeNode node) {
        TaskType type;
        try { type = TaskType.valueOf(node.getType()); }
        catch (IllegalArgumentException e) { type = TaskType.SIDE; }
        return TreeList.textTemplate(n -> Component.literal(QuestUiHelper.formatTaskType(type) + "任务")).apply(node);
    }

    private static UIElement buildTaskNodeUI(QuestTreeNode node) {
        TaskStatus status;
        try { status = TaskStatus.valueOf(node.getStatus()); }
        catch (IllegalArgumentException e) { status = TaskStatus.HIDDEN; }
        String icon = QuestUiHelper.statusIcon(status);
        String unclaimed = node.isHasUnclaimedReward() ? " \uD83C\uDF81" : "";
        return TreeList.textTemplate(n -> Component.literal(icon + unclaimed + " " + formatTaskDisplayName(node.getId()))).apply(node);
    }

    private static String formatTaskDisplayName(String taskId) {
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

    public record TaskListContext(UIElement panel, ScrollerView scroller, TreeList<QuestTreeNode> tree) {}
}
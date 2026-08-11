package com.maple.maple_banktrade.api.quests.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import dev.vfyjxf.taffy.style.FlexDirection;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.TREE_MARGIN_LEFT;

/**
 * 左栏任务列表面板 —— 使用 {@link QuestTreeList} 组件按类型分组显示任务。
 */
public final class QuestTaskListPanel {

    public static final String MSG_SELECT_TASK = "select_task";
    public static final String KEY_TASK_ID = "task_id";

    private QuestTaskListPanel() {}

    public static TaskListContext create(Player player) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(2));
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
        scroller.viewContainer(container -> container.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapAll(0).paddingAll(0)));

        // 使用 QuestTreeList（继承 TreeList 暴露 protected 方法）
        QuestTreeList tree = new QuestTreeList();
        tree.layout(l -> l.widthPercent(100).heightPercent(100).marginLeft(TREE_MARGIN_LEFT));
        tree.style(s -> s.background(IGuiTexture.EMPTY));
        tree.setFlattenRoot(true);
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

        QuestSyncData.TreeListSnapshot data = player instanceof ServerPlayer sp ? QuestUiHelper.buildQuestTreeList(sp) : QuestSyncData.TreeListSnapshot.empty();
        TaskListContext ctx = new TaskListContext(panel, scroller);

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

    private static void rebuildTree(QuestTreeList tree, QuestSyncData.TreeListSnapshot snapshot) {
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
        try {
            type = TaskType.valueOf(node.getType());
        } catch (IllegalArgumentException e) {
            type = TaskType.SIDE;
        }
        TaskType finalType = type;
        return QuestTreeList.textTemplate(n -> Component.literal(QuestUiHelper.formatTaskType(finalType) + "任务")).apply(node);
    }

    private static UIElement buildTaskNodeUI(QuestTreeNode node) {
        TaskStatus status;
        try {
            status = TaskStatus.valueOf(node.getStatus());
        } catch (IllegalArgumentException e) {
            status = TaskStatus.HIDDEN;
        }
        String icon = QuestUiHelper.statusIcon(status);
        String unclaimed = node.isHasUnclaimedReward() ? " \uD83C\uDF81" : "";
        return QuestTreeList.textTemplate(n -> Component.literal(icon + unclaimed + " " + formatTaskDisplayName(node.getId()))).apply(node);
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

    public record TaskListContext(UIElement panel, ScrollerView scroller) {}
}

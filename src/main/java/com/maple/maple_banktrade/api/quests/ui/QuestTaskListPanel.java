package com.maple.maple_banktrade.api.quests.ui;

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
import com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.TREE_MARGIN_LEFT;

/**
 * 左栏任务列表面板 —— 服务端仅发送状态快照，客户端基于本地蓝图重建树。
 * <p>
 * 任务选择通过客户端直接回调，不走 sendMessage/onMessage 服务端往返。
 */
public final class QuestTaskListPanel {

    private QuestTaskListPanel() {}

    /**
     * @param onSelect 客户端回调：当用户点击任务节点时调用，传入任务 ID
     */
    public static TaskListContext create(Player player, Consumer<String> onSelect) {
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

        QuestTreeList tree = new QuestTreeList();
        tree.layout(l -> l.widthPercent(100).heightPercent(100).marginLeft(TREE_MARGIN_LEFT));
        tree.style(s -> s.background(IGuiTexture.EMPTY));
        tree.setFlattenRoot(true);
        tree.setNodeUISupplier(QuestTaskListPanel::buildNodeUI);

        tree.setOnSelectedChanged(selected -> {
            if (!selected.isEmpty()) {
                QuestTreeNode node = selected.iterator().next();
                if (!node.isGroup()) {
                    onSelect.accept(node.getId());
                }
            }
        });

        scroller.addScrollViewChild(tree);

        QuestSyncData.QuestStatusSnapshot data = player instanceof ServerPlayer sp ? QuestUiHelper.buildStatusSnapshot(sp) : QuestSyncData.QuestStatusSnapshot.empty();
        TaskListContext ctx = new TaskListContext(panel, scroller);

        BindableValue<QuestSyncData.QuestStatusSnapshot> sync = new BindableValue<>(data);
        sync.layout(l -> l.width(0).height(0));
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.create(
                () -> QuestUiHelper.buildStatusSnapshot((ServerPlayer) player),
                v -> { /* c2s no-op */ })
                .syncType(QuestSyncData.QuestStatusSnapshot.class)
                .initialValue(data)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(clientData -> rebuildTreeFromStatus(tree, clientData))
                .build());

        panel.addChild(scroller);
        panel.addChild(sync);
        return ctx;
    }

    // ==============================================
    // 客户端：基于状态快照 + 本地蓝图重建树
    // ==============================================

    /**
     * 客户端收到状态快照后，基于本地 {@link QuestDefinitionRegistry} 蓝图重建任务树。
     * 仅包含 ACTIVE 和 VISIBLE_LOCKED 状态的任务，按类型分组。
     */
    private static void rebuildTreeFromStatus(QuestTreeList tree, QuestSyncData.QuestStatusSnapshot snapshot) {
        Map<String, QuestSyncData.TaskStatusEntry> statusMap = snapshot.toMap();

        // 按类型分组：MAIN → SIDE → TEMPORARY
        Map<TaskType, List<ITaskDefinition>> grouped = new LinkedHashMap<>();
        for (TaskType type : TaskType.values()) grouped.put(type, new ArrayList<>());

        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            QuestSyncData.TaskStatusEntry entry = statusMap.get(def.getId());
            if (entry == null) continue;
            // 过滤：只保留 ACTIVE 和 VISIBLE_LOCKED
            if (!"ACTIVE".equals(entry.getStatus()) && !"VISIBLE_LOCKED".equals(entry.getStatus())) continue;
            grouped.get(def.getType()).add(def);
        }

        List<QuestTreeNode> rootNodes = new ArrayList<>();
        for (Map.Entry<TaskType, List<ITaskDefinition>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            // 排序：ACTIVE 在前，VISIBLE_LOCKED 在后
            List<ITaskDefinition> sorted = entry.getValue().stream()
                    .sorted((a, b) -> {
                        String sa = statusMap.get(a.getId()).getStatus();
                        String sb = statusMap.get(b.getId()).getStatus();
                        return Integer.compare(statusOrderStr(sa), statusOrderStr(sb));
                    })
                    .toList();

            QuestTreeNode groupNode = QuestTreeNode.group("__group__" + entry.getKey().name(),
                    entry.getKey().name());
            rootNodes.add(groupNode);

            for (ITaskDefinition def : sorted) {
                QuestTreeNode node = buildNodeFromBlueprint(def, statusMap, 1);
                groupNode.addChildNode(node);
            }
        }

        // 设置到树
        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode r : rootNodes) dummyRoot.addChildNode(r);
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);
        for (QuestTreeNode r : rootNodes) tree.expandNode(r);
    }

    /** 客户端基于蓝图 + 状态构建节点（递归同类型子节点）。 */
    private static QuestTreeNode buildNodeFromBlueprint(ITaskDefinition def,
                                                        Map<String, QuestSyncData.TaskStatusEntry> statusMap,
                                                        int depth) {
        QuestSyncData.TaskStatusEntry entry = statusMap.get(def.getId());
        String status = entry != null ? entry.getStatus() : "HIDDEN";
        int comps = entry != null ? entry.getCompletions() : 0;
        boolean unclaimed = entry != null && entry.isHasUnclaimedReward();

        QuestTreeNode node = new QuestTreeNode()
                .setId(def.getId())
                .setType(def.getType().name())
                .setBehavior(def.getBehavior().name())
                .setGroup(def.isGroup())
                .setDepth(depth)
                .setRequiredCompletions(def.getRequiredCompletions())
                .setPoolIds(new ArrayList<>(def.getPoolIds()))
                .setDependentNodes(new ArrayList<>(def.getDependentNodes()))
                .setStatus(status)
                .setCompletions(comps)
                .setHasUnclaimedReward(unclaimed);

        for (ITaskDefinition child : QuestDefinitionRegistry.getChildren(def.getId())) {
            QuestSyncData.TaskStatusEntry childEntry = statusMap.get(child.getId());
            if (childEntry == null) continue;
            if (!"ACTIVE".equals(childEntry.getStatus()) && !"VISIBLE_LOCKED".equals(childEntry.getStatus())) continue;
            // 仅添加同类型子节点，防止跨类型混入
            if (child.getType() != def.getType()) continue;
            node.addChildNode(buildNodeFromBlueprint(child, statusMap, depth + 1));
        }
        return node;
    }

    private static int statusOrderStr(String status) {
        return switch (status) {
            case "ACTIVE" -> 0;
            case "VISIBLE_LOCKED" -> 1;
            default -> 99;
        };
    }

    // ==============================================
    // 节点 UI 渲染
    // ==============================================

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
        String key = switch (type) {
            case MAIN -> "ui.maple_banktrade.quest.type.main";
            case SIDE -> "ui.maple_banktrade.quest.type.side";
            case TEMPORARY -> "ui.maple_banktrade.quest.type.temporary";
        };
        String finalKey = key;
        return QuestTreeList.textTemplate(n -> Component.translatable(finalKey)).apply(node);
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

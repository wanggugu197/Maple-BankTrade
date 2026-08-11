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
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import dev.vfyjxf.taffy.style.FlexDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.TREE_MARGIN_LEFT;

/**
 * 已完成任务标签页 —— 服务端仅发送状态快照，客户端基于本地蓝图构建已完成列表。
 * <p>
 * 任务选择通过客户端直接回调，不走 sendMessage/onMessage 服务端往返。
 */
public final class QuestCompletedPanel {

    private QuestCompletedPanel() {}

    public static CompletedContext create(Player player, Consumer<String> onSelect) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(QuestUIRegistration.GAP_SMALL));
        panel.style(s -> s.background(IGuiTexture.EMPTY));

        QuestSyncData.QuestStatusSnapshot data = player instanceof ServerPlayer sp ? QuestUiHelper.buildStatusSnapshot(sp) : QuestSyncData.QuestStatusSnapshot.empty();

        // 标题
        TextElement title = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.completed_title", countCompleted(data)))
                .textStyle(s -> s.adaptiveHeight(true).adaptiveWidth(true));

        // ScrollerView 包裹树
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

        // 树状结构
        QuestTreeList tree = new QuestTreeList();
        tree.layout(l -> l.widthPercent(100).heightPercent(100).marginLeft(TREE_MARGIN_LEFT));
        tree.style(s -> s.background(IGuiTexture.EMPTY));
        tree.setFlattenRoot(true);
        tree.setNodeUISupplier(QuestCompletedPanel::buildCompletedNodeUI);

        tree.setOnSelectedChanged(selected -> {
            if (!selected.isEmpty()) {
                QuestTreeNode node = selected.iterator().next();
                if (!node.isGroup()) {
                    onSelect.accept(node.getId());
                }
            }
        });

        scroller.addScrollViewChild(title);
        scroller.addScrollViewChild(tree);

        // 同步状态快照
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
                    rebuildCompletedFromStatus(tree, clientData);
                    title.setText(Component.translatable(
                            "ui.maple_banktrade.quest.completed_title", countCompleted(clientData)));
                })
                .build());

        panel.addChild(scroller);
        panel.addChild(sync);
        return new CompletedContext(panel, scroller);
    }

    // ==============================================
    // 客户端：基于状态快照 + 本地蓝图重建已完成列表
    // ==============================================

    private static void rebuildCompletedFromStatus(QuestTreeList tree, QuestSyncData.QuestStatusSnapshot snapshot) {
        Map<String, QuestSyncData.TaskStatusEntry> statusMap = snapshot.toMap();

        List<QuestTreeNode> nodes = new ArrayList<>();
        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            QuestSyncData.TaskStatusEntry entry = statusMap.get(def.getId());
            if (entry == null) continue;
            // 已完成：状态为 COMPLETED 或有完成次数
            if (!"COMPLETED".equals(entry.getStatus()) && entry.getCompletions() == 0) continue;

            QuestTreeNode node = new QuestTreeNode()
                    .setId(def.getId())
                    .setType(def.getType().name())
                    .setDepth(0)
                    .setRequiredCompletions(def.getRequiredCompletions())
                    .setStatus(entry.getStatus())
                    .setCompletions(entry.getCompletions())
                    .setHasUnclaimedReward(entry.isHasUnclaimedReward());
            nodes.add(node);
        }

        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode n : nodes) dummyRoot.addChildNode(n);
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);
    }

    private static int countCompleted(QuestSyncData.QuestStatusSnapshot snapshot) {
        return (int) snapshot.getEntries().stream()
                .filter(e -> "COMPLETED".equals(e.getStatus()) || e.getCompletions() > 0)
                .count();
    }

    private static UIElement buildCompletedNodeUI(QuestTreeNode node) {
        String unclaimed = node.isHasUnclaimedReward() ? " \uD83C\uDF81" : "";
        String text = "\u2705" + unclaimed + " " + formatName(node.getId()) + "  \u2014  " + node.getCompletions() + " / " + node.getRequiredCompletions();
        return QuestTreeList.textTemplate(n -> Component.literal(text)).apply(node);
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

    public record CompletedContext(UIElement panel, ScrollerView scroller) {}
}

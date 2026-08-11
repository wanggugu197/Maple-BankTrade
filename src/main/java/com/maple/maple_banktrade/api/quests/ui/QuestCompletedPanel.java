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
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.TREE_MARGIN_LEFT;

/**
 * 已完成任务标签页 —— 左侧树状结构 + 右侧详情（通过 ScrollerView + SplitView）。
 */
public final class QuestCompletedPanel {

    private QuestCompletedPanel() {}

    /**
     * @return 包含 ScrollerView 的上下文，用于外部注册 onMessage 连线。
     */
    public static CompletedContext create(Player player) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(QuestUIRegistration.GAP_SMALL));
        panel.style(s -> s.background(IGuiTexture.EMPTY));

        QuestSyncData.TreeListSnapshot data = player instanceof ServerPlayer sp ? QuestUiHelper.buildCompletedTreeList(sp) : QuestSyncData.TreeListSnapshot.empty();

        // 标题
        TextElement title = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.completed_title", data.getTotal()))
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
        tree.setNodeUISupplier(node -> buildCompletedNodeUI(node));

        // 点击已完成任务节点 → 发送消息
        tree.setOnSelectedChanged(selected -> {
            if (!selected.isEmpty()) {
                QuestTreeNode node = selected.iterator().next();
                if (!node.isGroup()) {
                    CompoundTag payload = new CompoundTag();
                    payload.putString(QuestTaskListPanel.KEY_TASK_ID, node.getId());
                    scroller.sendMessage(QuestTaskListPanel.MSG_SELECT_TASK, payload);
                }
            }
        });

        scroller.addScrollViewChild(title);
        scroller.addScrollViewChild(tree);

        // 同步
        BindableValue<QuestSyncData.TreeListSnapshot> sync = new BindableValue<>(data);
        sync.layout(l -> l.width(0).height(0));
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.create(
                () -> {
                    data.copyFrom(QuestUiHelper.buildCompletedTreeList((ServerPlayer) player));
                    return data;
                },
                v -> { /* c2s no-op */ })
                .syncType(QuestSyncData.TreeListSnapshot.class)
                .initialValue(data)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(clientData -> {
                    rebuildCompletedTree(tree, clientData);
                    title.setText(Component.translatable(
                            "ui.maple_banktrade.quest.completed_title", clientData.getTotal()));
                })
                .build());

        panel.addChild(scroller);
        panel.addChild(sync);
        return new CompletedContext(panel, scroller);
    }

    private static void rebuildCompletedTree(QuestTreeList tree, QuestSyncData.TreeListSnapshot snapshot) {
        for (QuestTreeNode root : snapshot.getRoots()) {
            root.reattachParents();
        }
        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode node : snapshot.getRoots()) {
            dummyRoot.addChildNode(node);
        }
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);
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

    // ==============================================
    // 内部类型
    // ==============================================

    public record CompletedContext(UIElement panel, ScrollerView scroller) {}
}

package com.maple.maple_banktrade.api.quests.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;

import static com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration.TREE_MARGIN_LEFT;

/**
 * 创造模式树状结构标签页 —— 左侧完整任务树 + 右侧详情（ScrollerView + SplitView）。
 */
public final class QuestTreePanel {

    private QuestTreePanel() {}

    /**
     * @return 包含 ScrollerView 的上下文，用于外部注册 onMessage 连线。
     */
    public static TreeContext create() {
        UIElement panel = new UIElement();
        panel.layout(l -> l.widthPercent(100).heightPercent(100)
                .flexDirection(FlexDirection.COLUMN).gapAll(QuestUIRegistration.GAP_SMALL));
        panel.style(s -> s.background(IGuiTexture.EMPTY));

        QuestSyncData.TreeListSnapshot snapshot = QuestUiHelper.buildStaticTree();
        TextElement title = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.tree_title", snapshot.getTotal()))
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
        tree.setNodeUISupplier(node -> buildTreeNodeUI(node));

        // 点击节点 → 发送消息
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

        // 构建静态树数据
        for (QuestTreeNode root : snapshot.getRoots()) {
            root.reattachParents();
        }
        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode root : snapshot.getRoots()) {
            dummyRoot.addChildNode(root);
        }
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);

        panel.addChild(scroller);
        return new TreeContext(panel, scroller);
    }

    private static UIElement buildTreeNodeUI(QuestTreeNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.isGroup()) sb.append("\uD83D\uDCC1 ");
        sb.append(node.getId());
        sb.append(" [").append(node.getType()).append("]");
        if (!"SIMPLE".equals(node.getBehavior())) {
            sb.append(" (").append(node.getBehavior());
            if (node.getRequiredCompletions() > 1) sb.append("\u00D7").append(node.getRequiredCompletions());
            sb.append(")");
        }
        if (!node.getPoolIds().isEmpty()) {
            sb.append(" \uD83C\uDFB2[").append(String.join(", ", node.getPoolIds())).append("]");
        }
        if (!node.getDependentNodes().isEmpty()) {
            sb.append(" \u2B05[").append(String.join(", ", node.getDependentNodes())).append("]");
        }
        return QuestTreeList.textTemplate(n -> Component.literal(sb.toString())).apply(node);
    }

    // ==============================================
    // 内部类型
    // ==============================================

    public record TreeContext(UIElement panel, ScrollerView scroller) {}
}

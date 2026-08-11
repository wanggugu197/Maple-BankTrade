package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import net.minecraft.network.chat.Component;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;

import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 创造模式树状结构标签页 —— 使用 {@link TreeList} 组件渲染完整任务层级。
 */
public final class QuestTreePanel {

    private QuestTreePanel() {}

    public static UIElement create() {
        UIElement panel = new UIElement();
        panel.layout(l -> l.width(320).height(186).flexDirection(FlexDirection.COLUMN)
                .gapAll(2).paddingAll(4));
        panel.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);

        QuestSyncData.TreeListSnapshot snapshot = QuestUiHelper.buildStaticTree();
        TextElement title = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.tree_title", snapshot.getTotal()));

        TreeList<QuestTreeNode> tree = new TreeList<>();
        tree.layout(l -> l.widthPercent(100).heightPercent(100));
        tree.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        tree.set(PropertyRegistry.FONT_SIZE, 7f);
        tree.set(PropertyRegistry.TEXT_SHADOW, false);
        tree.setFlattenRoot(true);
        tree.setStaticTree(true);
        tree.setClickToExpand(true);
        tree.setNodeUISupplier(node -> buildTreeNodeUI(node));

        for (QuestTreeNode root : snapshot.getRoots()) {
            root.reattachParents();
        }
        QuestTreeNode dummyRoot = new QuestTreeNode().setId("__root__");
        for (QuestTreeNode root : snapshot.getRoots()) {
            dummyRoot.addChildNode(root);
        }
        tree.setRoot(dummyRoot);
        tree.setFlattenRoot(true);

        panel.addChild(title);
        panel.addChild(tree);
        return panel;
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
        return TreeList.textTemplate(n -> Component.literal(sb.toString())).apply(node);
    }
}
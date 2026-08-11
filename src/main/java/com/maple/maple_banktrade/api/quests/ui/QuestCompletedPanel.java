package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;

import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * 已完成任务标签页 —— 使用 {@link TreeList} 组件渲染。
 */
public final class QuestCompletedPanel {

    private QuestCompletedPanel() {}

    public static UIElement create(Player player) {
        UIElement panel = new UIElement();
        panel.layout(l -> l.width(320).height(186).flexDirection(FlexDirection.COLUMN)
                .gapAll(2).paddingAll(4));
        panel.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);

        QuestSyncData.TreeListSnapshot data = player instanceof ServerPlayer sp
                ? QuestUiHelper.buildCompletedTreeList(sp)
                : QuestSyncData.TreeListSnapshot.empty();
        TextElement title = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.quest.completed_title", data.getTotal()));

        TreeList<QuestTreeNode> tree = new TreeList<>();
        tree.layout(l -> l.widthPercent(100).heightPercent(100));
        tree.set(PropertyRegistry.BACKGROUND, IGuiTexture.EMPTY);
        tree.set(PropertyRegistry.FONT_SIZE, 7f);
        tree.set(PropertyRegistry.TEXT_SHADOW, false);
        tree.setFlattenRoot(true);
        tree.setStaticTree(true);
        tree.setClickToExpand(true);
        tree.setNodeUISupplier(node -> buildCompletedNodeUI(node));

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
                    title.setText(Component.translatable("ui.maple_banktrade.quest.completed_title", clientData.getTotal()));
                })
                .build());

        panel.addChild(title);
        panel.addChild(tree);
        panel.addChild(sync);
        return panel;
    }

    private static void rebuildCompletedTree(TreeList<QuestTreeNode> tree, QuestSyncData.TreeListSnapshot snapshot) {
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
        String text = "\u2705" + unclaimed + " " + formatName(node.getId())
                + "  \u2014  " + node.getCompletions() + " / " + node.getRequiredCompletions();
        return TreeList.textTemplate(n -> Component.literal(text)).apply(node);
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
}
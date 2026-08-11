package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;

import java.util.Set;
import java.util.function.Consumer;

/**
 * 任务用 TreeList —— 继承 {@link TreeList} 以暴露 protected 方法。
 *
 * <p>
 * LDLib2 的 {@link TreeList} 将 {@code setClickToExpand}、{@code setStaticTree}、
 * {@code setOnSelectedChanged} 等关键方法声明为 protected（Lombok {@code @Setter} 在 protected 字段上），
 * 外部包无法直接调用。本子类将这些方法提升为 public。
 */
public class QuestTreeList extends TreeList<QuestTreeNode> {

    public QuestTreeList() {
        super();
        setStaticTree(true);
        setClickToExpand(true);
    }

    /** 暴露 protected setOnSelectedChanged。 */
    @Override
    public QuestTreeList setOnSelectedChanged(Consumer<Set<QuestTreeNode>> handler) {
        super.setOnSelectedChanged(handler);
        return this;
    }

    /** 暴露 protected setClickToExpand。 */
    @Override
    public QuestTreeList setClickToExpand(boolean v) {
        super.setClickToExpand(v);
        return this;
    }

    /** 暴露 protected setStaticTree。 */
    @Override
    public QuestTreeList setStaticTree(boolean v) {
        super.setStaticTree(v);
        return this;
    }
}

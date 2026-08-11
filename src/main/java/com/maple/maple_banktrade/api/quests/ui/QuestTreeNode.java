package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 任务树节点 —— 同时实现 {@link ITreeNode}（供 TreeList 渲染）和
 * {@link IPersistedSerializable}（供 S2C 同步）。
 *
 * <p>
 * KEY = 任务 ID（String），CONTENT = 自身（QuestTreeNode）。
 * 包含静态结构（父/子关系、类型、深度）和动态状态（进度、奖励领取）。
 */
@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
public class QuestTreeNode implements ITreeNode<String, QuestTreeNode>, IPersistedSerializable {

    // ==============================================
    // 静态字段（来自任务定义）
    // ==============================================

    @Persisted
    private String id = "";

    @Persisted
    private String type = "";

    @Persisted
    private String behavior = "";

    @Persisted
    private boolean isGroup;

    @Persisted
    private int depth;

    @Persisted
    private int requiredCompletions = 1;

    @Persisted
    private List<String> poolIds = new ArrayList<>();

    @Persisted
    private List<String> dependentNodes = new ArrayList<>();

    // ==============================================
    // 动态字段（来自玩家状态）
    // ==============================================

    @Persisted
    private String status = "HIDDEN";

    @Persisted
    private int completions;

    @Persisted
    private boolean hasUnclaimedReward;

    // ==============================================
    // 树结构（父/子）
    // ==============================================

    /** 父节点引用（不由 @Persisted 序列化，在客户端重建）。 */
    @Nullable
    private transient QuestTreeNode parent;

    @Persisted
    private List<QuestTreeNode> children = new ArrayList<>();

    // ==============================================
    // 构造
    // ==============================================

    public QuestTreeNode() {}

    /** 创建叶子/分支节点。 */
    public static QuestTreeNode of(String id, String type, int depth) {
        return new QuestTreeNode().setId(id).setType(type).setDepth(depth);
    }

    /** 创建类型分组节点（仅用于 TreeList 渲染，不参与同步）。 */
    public static QuestTreeNode group(String id, String type) {
        return new QuestTreeNode().setId(id).setType(type).setDepth(0).setGroup(true);
    }

    // ==============================================
    // ITreeNode 实现
    // ==============================================

    @Override
    public int getDimension() {
        return depth;
    }

    @Override
    @Nonnull
    public String getKey() {
        return id;
    }

    @Override
    @Nullable
    public QuestTreeNode getContent() {
        return this;
    }

    @Override
    @Nullable
    public ITreeNode<String, QuestTreeNode> getParent() {
        return parent;
    }

    @Override
    @Nonnull
    public List<? extends ITreeNode<String, QuestTreeNode>> getChildren() {
        return children;
    }

    @Override
    public boolean isLeaf() {
        return children.isEmpty();
    }

    // ==============================================
    // 辅助
    // ==============================================

    /** 将子节点添加到此节点并设置其父引用。 */
    public void addChildNode(QuestTreeNode child) {
        child.parent = this;
        this.children.add(child);
    }

    /** 重新设置所有子节点的父引用（用于客户端反序列化后重建）。 */
    public void reattachParents() {
        for (QuestTreeNode child : children) {
            child.parent = this;
            child.reattachParents();
        }
    }

    @Override
    public String toString() {
        return "QuestTreeNode{id='" + id + "', type=" + type + ", status=" + status + ", depth=" + depth + "}";
    }
}

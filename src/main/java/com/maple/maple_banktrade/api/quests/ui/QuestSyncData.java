package com.maple.maple_banktrade.api.quests.ui;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * UI 同步数据类族 —— 实现 {@link IPersistedSerializable}，
 * 通过 {@code @Persisted} 注解自动处理序列化/反序列化。
 *
 * <p>
 * v3.8 重构：所有数据类实现 {@link IPersistedSerializable}（extends {@code ValueIOSerializable}），
 * 消除手动 {@code toTag()}/{@code fromTag()} 样板代码。
 * 服务端持有稳定可变实例，LDLib2 通过 {@code ValueIOSerializableReadOnlyAccessor}（优先级 2000）自动同步。
 */
public final class QuestSyncData {

    private QuestSyncData() {}

    // ==============================================
    // TaskListItem —— 单条任务列表项
    // ==============================================

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class TaskListItem implements IPersistedSerializable {

        @Persisted
        private String id = "";
        @Persisted
        private String status = "HIDDEN";
        @Persisted
        private String type = "";
        @Persisted
        private String parentId = "";
        @Persisted
        private boolean isGroup;
        @Persisted
        private int completions;
        @Persisted
        private int requiredCompletions = 1;
        @Persisted
        private boolean hasUnclaimedReward;

        public TaskListItem() {}

        /** 从另一实例原地复制字段（用于服务端持有者更新）。 */
        public void copyFrom(TaskListItem other) {
            this.id = other.id;
            this.status = other.status;
            this.type = other.type;
            this.parentId = other.parentId;
            this.isGroup = other.isGroup;
            this.completions = other.completions;
            this.requiredCompletions = other.requiredCompletions;
            this.hasUnclaimedReward = other.hasUnclaimedReward;
        }
    }

    // ==============================================
    // TaskListSnapshot —— 任务列表快照
    // ==============================================

    @Getter
    @Setter
    public static class TaskListSnapshot implements IPersistedSerializable {

        @Persisted
        private List<TaskListItem> tasks = new ArrayList<>();

        public TaskListSnapshot() {}

        public static TaskListSnapshot empty() {
            return new TaskListSnapshot();
        }

        /** 原地更新任务列表（保持 List 实例稳定，满足 LDLib2 只读集合要求）。 */
        public void copyFrom(TaskListSnapshot other) {
            this.tasks.clear();
            for (TaskListItem item : other.tasks) {
                TaskListItem copy = new TaskListItem();
                copy.copyFrom(item);
                this.tasks.add(copy);
            }
        }
    }

    // ==============================================
    // DependentInfo —— 依赖节点信息
    // ==============================================

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class DependentInfo implements IPersistedSerializable {

        @Persisted
        private String id = "";
        @Persisted
        private String status = "HIDDEN";

        public DependentInfo() {}

        public void copyFrom(DependentInfo other) {
            this.id = other.id;
            this.status = other.status;
        }
    }

    // ==============================================
    // RewardInfo —— 奖励信息
    // ==============================================

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class RewardInfo implements IPersistedSerializable {

        @Persisted
        private String type = "";
        @Persisted
        private String item = "";
        @Persisted
        private int count = 1;

        public RewardInfo() {}

        public void copyFrom(RewardInfo other) {
            this.type = other.type;
            this.item = other.item;
            this.count = other.count;
        }
    }

    // ==============================================
    // TaskDetail —— 任务详情
    // ==============================================

    @Getter
    @Setter
    public static class TaskDetail implements IPersistedSerializable {

        @Persisted
        private String id = "";
        @Persisted
        private String status = "HIDDEN";
        @Persisted
        private String type = "";
        @Persisted
        private String behavior = "";
        @Persisted
        private boolean isGroup;
        @Persisted
        private String parentId = "";
        @Persisted
        private String prevSiblingId = "";
        @Persisted
        private String nextTaskInChain = "";
        @Persisted
        private boolean repeatable;
        @Persisted
        private int maxRepeatTimes = -1;
        @Persisted
        private boolean forceParentVisible;
        @Persisted
        private int completions;
        @Persisted
        private int requiredCompletions = 1;
        @Persisted
        private List<DependentInfo> dependents = new ArrayList<>();
        @Persisted
        private String dependencyRequirement = "";
        @Persisted
        private String unlockCond = "";
        @Persisted
        private String visCond = "";
        @Persisted
        private List<String> taskTypeInfos = new ArrayList<>();
        @Persisted
        private List<RewardInfo> rewards = new ArrayList<>();
        @Persisted
        private boolean hasUnclaimedReward;
        @Persisted
        private double nextChainChance;

        public TaskDetail() {}

        public static TaskDetail empty() {
            return new TaskDetail();
        }

        /** 原地更新详情字段（保持 List 实例稳定）。 */
        public void copyFrom(TaskDetail other) {
            this.id = other.id;
            this.status = other.status;
            this.type = other.type;
            this.behavior = other.behavior;
            this.isGroup = other.isGroup;
            this.parentId = other.parentId;
            this.prevSiblingId = other.prevSiblingId;
            this.nextTaskInChain = other.nextTaskInChain;
            this.repeatable = other.repeatable;
            this.maxRepeatTimes = other.maxRepeatTimes;
            this.forceParentVisible = other.forceParentVisible;
            this.completions = other.completions;
            this.requiredCompletions = other.requiredCompletions;
            this.dependencyRequirement = other.dependencyRequirement;
            this.unlockCond = other.unlockCond;
            this.visCond = other.visCond;
            this.hasUnclaimedReward = other.hasUnclaimedReward;
            this.nextChainChance = other.nextChainChance;

            this.dependents.clear();
            for (DependentInfo d : other.dependents) {
                DependentInfo copy = new DependentInfo();
                copy.copyFrom(d);
                this.dependents.add(copy);
            }

            this.taskTypeInfos.clear();
            this.taskTypeInfos.addAll(other.taskTypeInfos);

            this.rewards.clear();
            for (RewardInfo r : other.rewards) {
                RewardInfo copy = new RewardInfo();
                copy.copyFrom(r);
                this.rewards.add(copy);
            }
        }
    }

    // ==============================================
    // CompletedItem —— 已完成条目
    // ==============================================

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class CompletedItem implements IPersistedSerializable {

        @Persisted
        private String id = "";
        @Persisted
        private String type = "";
        @Persisted
        private int completions;
        @Persisted
        private int required = 1;
        @Persisted
        private long latestTime;
        @Persisted
        private boolean hasUnclaimedReward;

        public CompletedItem() {}

        public void copyFrom(CompletedItem other) {
            this.id = other.id;
            this.type = other.type;
            this.completions = other.completions;
            this.required = other.required;
            this.latestTime = other.latestTime;
            this.hasUnclaimedReward = other.hasUnclaimedReward;
        }
    }

    // ==============================================
    // CompletedListSnapshot —— 已完成列表快照
    // ==============================================

    @Getter
    @Setter
    public static class CompletedListSnapshot implements IPersistedSerializable {

        @Persisted
        private int total;
        @Persisted
        private List<CompletedItem> items = new ArrayList<>();

        public CompletedListSnapshot() {}

        public static CompletedListSnapshot empty() {
            return new CompletedListSnapshot();
        }

        public void copyFrom(CompletedListSnapshot other) {
            this.total = other.total;
            this.items.clear();
            for (CompletedItem item : other.items) {
                CompletedItem copy = new CompletedItem();
                copy.copyFrom(item);
                this.items.add(copy);
            }
        }
    }

    // ==============================================
    // TreeNode —— 树节点
    // ==============================================

    @Getter
    @Setter
    public static class TreeNode implements IPersistedSerializable {

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
        private String nextChain = "";
        @Persisted
        private List<String> poolIds = new ArrayList<>();
        @Persisted
        private List<String> dependentNodes = new ArrayList<>();
        @Persisted
        private List<TreeNode> children = new ArrayList<>();

        public TreeNode() {}
    }

    // ==============================================
    // TreeSnapshot —— 树快照（旧 TreePanel 用，保留兼容）
    // ==============================================

    @Getter
    @Setter
    public static class TreeSnapshot implements IPersistedSerializable {

        @Persisted
        private List<TreeNode> roots = new ArrayList<>();
        @Persisted
        private int total;

        public TreeSnapshot() {}

        public static TreeSnapshot empty() {
            return new TreeSnapshot();
        }
    }

    // ==============================================
    // TreeListSnapshot —— TreeList 组件同步快照
    // ==============================================

    /**
     * TreeList 同步数据快照。
     * <p>
     * 包含 QuestTreeNode 树根列表，服务端构建完整树（含动态状态），
     * 客户端 {@code remoteSetter} 重建 {@code TreeList<QuestTreeNode>}。
     */
    @Getter
    @Setter
    public static class TreeListSnapshot implements IPersistedSerializable {

        /** 树根节点列表（服务端构建完整树）。 */
        @Persisted
        private List<QuestTreeNode> roots = new ArrayList<>();
        /** 节点总数。 */
        @Persisted
        private int total;

        public TreeListSnapshot() {}

        public static TreeListSnapshot empty() {
            return new TreeListSnapshot();
        }

        /** 原地更新（保持 List 实例稳定）。 */
        public void copyFrom(TreeListSnapshot other) {
            this.roots.clear();
            this.roots.addAll(other.roots);
            this.total = other.total;
        }
    }
}

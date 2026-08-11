package com.maple.maple_banktrade.api.quests.ui;

import net.minecraft.server.level.ServerPlayer;

import com.lowdragmc.lowdraglib2.gui.util.ITreeNode;
import com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry;
import com.maple.maple_banktrade.api.quests.calculator.VisibilityCalculator;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;
import com.maple.maple_banktrade.api.quests.reward.IReward;
import com.maple.maple_banktrade.api.quests.reward.ItemReward;
import com.maple.maple_banktrade.api.quests.storage.QuestDataManager;
import com.maple.maple_banktrade.api.quests.tasktype.ITaskType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 任务 UI 辅助工具 —— 构建类型化同步数据。
 *
 * <p>
 * v3.8 重构：返回 {@link QuestSyncData} 可变实例（实现 {@code IPersistedSerializable}），
 * 不再手动转换 {@link net.minecraft.nbt.CompoundTag}。
 * 序列化/反序列化由 LDLib2 {@code PersistedParser} 通过 {@code @Persisted} 注解自动处理。
 */
public final class QuestUiHelper {

    private QuestUiHelper() {}

    // ==============================================
    // 任务列表快照
    // ==============================================

    public static QuestSyncData.TaskListSnapshot buildTaskListSnapshot(ServerPlayer player) {
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        QuestSyncData.TaskListSnapshot snapshot = new QuestSyncData.TaskListSnapshot();
        List<QuestSyncData.TaskListItem> items = snapshot.getTasks();

        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
            boolean hasUnclaimed = false;
            if (status == TaskStatus.COMPLETED) {
                List<ICompletionRecord> records = data.getCompletionRecords(def.getId());
                hasUnclaimed = records.stream().anyMatch(r -> !r.isRewardClaimed());
            }

            QuestSyncData.TaskListItem item = new QuestSyncData.TaskListItem()
                    .setId(def.getId())
                    .setStatus(status.name())
                    .setType(def.getType().name())
                    .setParentId(def.getParentId() != null ? def.getParentId() : "")
                    .setGroup(def.isGroup())
                    .setCompletions(ctx.getCompletionCount(def.getId()))
                    .setRequiredCompletions(def.getRequiredCompletions())
                    .setHasUnclaimedReward(hasUnclaimed);
            items.add(item);
        }
        return snapshot;
    }

    // ==============================================
    // 任务详情快照
    // ==============================================

    public static QuestSyncData.TaskDetail buildTaskDetail(ServerPlayer player, String taskId) {
        ITaskDefinition def = QuestDefinitionRegistry.getDefinition(taskId);
        if (def == null) return QuestSyncData.TaskDetail.empty();

        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        TaskStatus status = VisibilityCalculator.resolveStatus(taskId, ctx);

        // 依赖节点
        List<QuestSyncData.DependentInfo> deps = new ArrayList<>();
        for (String depId : def.getDependentNodes()) {
            QuestSyncData.DependentInfo dep = new QuestSyncData.DependentInfo()
                    .setId(depId)
                    .setStatus(ctx.snapshotStatus(depId).name());
            deps.add(dep);
        }

        // 奖励
        List<QuestSyncData.RewardInfo> rewardInfos = new ArrayList<>();
        for (IReward reward : def.getRewards()) {
            if (reward instanceof ItemReward itemReward) {
                rewardInfos.add(new QuestSyncData.RewardInfo()
                        .setType("item")
                        .setItem(itemReward.getItem().toString())
                        .setCount(itemReward.getCount()));
            } else {
                rewardInfos.add(new QuestSyncData.RewardInfo()
                        .setType(reward.getClass().getSimpleName()));
            }
        }

        // 任务完成类型
        List<String> taskTypeInfos = new ArrayList<>();
        for (ITaskType tt : def.getTaskTypes()) {
            taskTypeInfos.add(tt.getClass().getSimpleName());
        }

        // 检查是否有未领取的奖励
        boolean hasUnclaimed = false;
        if (status == TaskStatus.COMPLETED) {
            List<ICompletionRecord> records = data.getCompletionRecords(taskId);
            hasUnclaimed = records.stream().anyMatch(r -> !r.isRewardClaimed());
        }

        QuestSyncData.TaskDetail detail = new QuestSyncData.TaskDetail();
        detail.setId(def.getId());
        detail.setStatus(status.name());
        detail.setType(def.getType().name());
        detail.setBehavior(def.getBehavior().name());
        detail.setGroup(def.isGroup());
        detail.setParentId(def.getParentId() != null ? def.getParentId() : "");
        detail.setPrevSiblingId(def.getPrevSiblingId() != null ? def.getPrevSiblingId() : "");
        detail.setNextTaskInChain(def.getNextTaskInChain() != null ? def.getNextTaskInChain() : "");
        detail.setRepeatable(def.isRepeatable());
        detail.setMaxRepeatTimes(def.getMaxRepeatTimes());
        detail.setForceParentVisible(def.isForceParentVisible());
        detail.setCompletions(ctx.getCompletionCount(taskId));
        detail.setRequiredCompletions(def.getRequiredCompletions());
        detail.setDependents(deps);
        detail.setDependencyRequirement(def.getDependencyRequirement().name());
        detail.setUnlockCond(def.getUnlockCondition() != null ? def.getUnlockCondition().toString() : "");
        detail.setVisCond(def.getVisibilityCondition() != null ? def.getVisibilityCondition().toString() : "");
        detail.setTaskTypeInfos(taskTypeInfos);
        detail.setRewards(rewardInfos);
        detail.setHasUnclaimedReward(hasUnclaimed);
        detail.setNextChainChance(def.getNextChainTriggerChance());
        return detail;
    }

    // ==============================================
    // 已完成列表
    // ==============================================

    public static QuestSyncData.CompletedListSnapshot buildCompletedList(ServerPlayer player) {
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        QuestSyncData.CompletedListSnapshot snapshot = new QuestSyncData.CompletedListSnapshot();
        List<QuestSyncData.CompletedItem> items = snapshot.getItems();
        int total = 0;

        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            int completions = ctx.getCompletionCount(def.getId());
            TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
            if (status != TaskStatus.COMPLETED && completions == 0) continue;

            total++;
            List<ICompletionRecord> records = data.getCompletionRecords(def.getId());
            long latestTime = records.isEmpty() ? 0L : records.get(records.size() - 1).getCompletedGameTime();
            boolean hasUnclaimed = records.stream().anyMatch(r -> !r.isRewardClaimed());

            items.add(new QuestSyncData.CompletedItem()
                    .setId(def.getId())
                    .setType(def.getType().name())
                    .setCompletions(completions)
                    .setRequired(def.getRequiredCompletions())
                    .setLatestTime(latestTime)
                    .setHasUnclaimedReward(hasUnclaimed));
        }
        snapshot.setTotal(total);
        return snapshot;
    }

    // ==============================================
    // 树状结构
    // ==============================================

    public static QuestSyncData.TreeSnapshot buildTreeSnapshot() {
        QuestSyncData.TreeSnapshot snapshot = new QuestSyncData.TreeSnapshot();
        List<QuestSyncData.TreeNode> roots = snapshot.getRoots();
        for (ITaskDefinition def : QuestDefinitionRegistry.getRoots()) {
            roots.add(buildTreeNode(def, 0, new HashSet<>()));
        }
        snapshot.setTotal(QuestDefinitionRegistry.size());
        return snapshot;
    }

    private static QuestSyncData.TreeNode buildTreeNode(ITaskDefinition def, int depth, Set<String> visited) {
        QuestSyncData.TreeNode node = new QuestSyncData.TreeNode();
        if (!visited.add(def.getId())) {
            node.setId(def.getId() + " (循环)");
            node.setDepth(depth);
            node.setRequiredCompletions(1);
            return node;
        }

        node.setId(def.getId());
        node.setType(def.getType().name());
        node.setBehavior(def.getBehavior().name());
        node.setGroup(def.isGroup());
        node.setDepth(depth);
        node.setRequiredCompletions(def.getRequiredCompletions());
        node.setNextChain(def.getNextTaskInChain() != null ? def.getNextTaskInChain() : "");
        node.setPoolIds(new ArrayList<>(def.getPoolIds()));
        node.setDependentNodes(new ArrayList<>(def.getDependentNodes()));

        List<QuestSyncData.TreeNode> children = node.getChildren();
        for (ITaskDefinition child : QuestDefinitionRegistry.getChildren(def.getId())) {
            children.add(buildTreeNode(child, depth + 1, visited));
        }
        return node;
    }

    // ==============================================
    // TreeList 构建（v4.0：使用 TreeList 组件）
    // ==============================================

    /**
     * 构建任务树列表（主标签页）：按类型分组，仅包含进行中和可见但锁定的任务。
     * <p>
     * 过滤规则：跳过 COMPLETED（已完成）和 HIDDEN（隐藏）状态。
     * 排序规则：ACTIVE（进行中）在前，VISIBLE_LOCKED（可见但锁定）在后。
     * 分组：主线 → 支线 → 随机。
     */
    public static QuestSyncData.TreeListSnapshot buildQuestTreeList(ServerPlayer player) {
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        QuestSyncData.TreeListSnapshot snapshot = new QuestSyncData.TreeListSnapshot();
        List<QuestTreeNode> roots = snapshot.getRoots();
        int total = 0;

        // 按类型分组：MAIN, SIDE, TEMPORARY
        Map<TaskType, List<ITaskDefinition>> grouped = new LinkedHashMap<>();
        for (TaskType type : TaskType.values()) grouped.put(type, new ArrayList<>());

        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
            // 过滤：只保留 ACTIVE 和 VISIBLE_LOCKED
            if (status != TaskStatus.ACTIVE && status != TaskStatus.VISIBLE_LOCKED) continue;
            grouped.get(def.getType()).add(def);
        }

        for (Map.Entry<TaskType, List<ITaskDefinition>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            // 排序：ACTIVE 在前，VISIBLE_LOCKED 在后
            List<ITaskDefinition> sorted = entry.getValue().stream()
                    .sorted((a, b) -> {
                        TaskStatus sa = VisibilityCalculator.resolveStatus(a.getId(), ctx);
                        TaskStatus sb = VisibilityCalculator.resolveStatus(b.getId(), ctx);
                        return Integer.compare(statusOrder(sa), statusOrder(sb));
                    })
                    .toList();

            // 类型分组节点
            QuestTreeNode groupNode = QuestTreeNode.group("__group__" + entry.getKey().name(),
                    entry.getKey().name());
            roots.add(groupNode);

            for (ITaskDefinition def : sorted) {
                QuestTreeNode node = buildTreeListNode(def, ctx, data, 1);
                groupNode.addChildNode(node);
                total++;
            }
        }

        snapshot.setTotal(total);
        return snapshot;
    }

    /** 排序权重：ACTIVE=0, VISIBLE_LOCKED=1, 其他=99。 */
    private static int statusOrder(TaskStatus status) {
        return switch (status) {
            case ACTIVE -> 0;
            case VISIBLE_LOCKED -> 1;
            default -> 99;
        };
    }

    /**
     * 构建已完成任务树列表（已完成标签页，扁平列表，不递归子节点）。
     */
    public static QuestSyncData.TreeListSnapshot buildCompletedTreeList(ServerPlayer player) {
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        QuestSyncData.TreeListSnapshot snapshot = new QuestSyncData.TreeListSnapshot();
        List<QuestTreeNode> roots = snapshot.getRoots();
        int total = 0;

        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
            int completions = ctx.getCompletionCount(def.getId());
            if (status != TaskStatus.COMPLETED && completions == 0) continue;

            // 扁平节点：不递归子节点，避免与 getAllDefinitions 外层循环重复
            QuestTreeNode node = buildTreeListNode(def, ctx, data, 0);
            node.getChildren().clear(); // 清除递归产生的子节点
            roots.add(node);
            total++;
        }

        snapshot.setTotal(total);
        return snapshot;
    }

    /**
     * 构建纯树结构（树结构标签页，无动态数据）。
     */
    public static QuestSyncData.TreeListSnapshot buildStaticTree() {
        QuestSyncData.TreeListSnapshot snapshot = new QuestSyncData.TreeListSnapshot();
        List<QuestTreeNode> roots = snapshot.getRoots();
        int total = 0;

        for (ITaskDefinition def : QuestDefinitionRegistry.getRoots()) {
            QuestTreeNode node = buildStaticTreeNode(def, 0, new HashSet<>());
            roots.add(node);
            total += countNodes(node);
        }

        snapshot.setTotal(total);
        return snapshot;
    }

    private static QuestTreeNode buildTreeListNode(ITaskDefinition def, ResolutionContext ctx,
                                                   PlayerQuestData data, int depth) {
        TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
        boolean hasUnclaimed = false;
        if (status == TaskStatus.COMPLETED) {
            List<ICompletionRecord> records = data.getCompletionRecords(def.getId());
            hasUnclaimed = records.stream().anyMatch(r -> !r.isRewardClaimed());
        }

        QuestTreeNode node = new QuestTreeNode()
                .setId(def.getId())
                .setType(def.getType().name())
                .setBehavior(def.getBehavior().name())
                .setGroup(def.isGroup())
                .setDepth(depth)
                .setRequiredCompletions(def.getRequiredCompletions())
                .setPoolIds(new ArrayList<>(def.getPoolIds()))
                .setDependentNodes(new ArrayList<>(def.getDependentNodes()))
                .setStatus(status.name())
                .setCompletions(ctx.getCompletionCount(def.getId()))
                .setHasUnclaimedReward(hasUnclaimed);

        for (ITaskDefinition child : QuestDefinitionRegistry.getChildren(def.getId())) {
            TaskStatus childStatus = VisibilityCalculator.resolveStatus(child.getId(), ctx);
            // 只保留 ACTIVE/VISIBLE_LOCKED，且仅添加同类型子节点，防止支线混入主线
            if (childStatus != TaskStatus.ACTIVE && childStatus != TaskStatus.VISIBLE_LOCKED) continue;
            if (child.getType() != def.getType()) continue;
            node.addChildNode(buildTreeListNode(child, ctx, data, depth + 1));
        }
        return node;
    }

    private static QuestTreeNode buildStaticTreeNode(ITaskDefinition def, int depth, Set<String> visited) {
        if (!visited.add(def.getId())) {
            return new QuestTreeNode()
                    .setId(def.getId() + " (循环)")
                    .setDepth(depth)
                    .setRequiredCompletions(1);
        }

        QuestTreeNode node = new QuestTreeNode()
                .setId(def.getId())
                .setType(def.getType().name())
                .setBehavior(def.getBehavior().name())
                .setGroup(def.isGroup())
                .setDepth(depth)
                .setRequiredCompletions(def.getRequiredCompletions())
                .setPoolIds(new ArrayList<>(def.getPoolIds()))
                .setDependentNodes(new ArrayList<>(def.getDependentNodes()));

        for (ITaskDefinition child : QuestDefinitionRegistry.getChildren(def.getId())) {
            node.addChildNode(buildStaticTreeNode(child, depth + 1, visited));
        }
        return node;
    }

    private static int countNodes(QuestTreeNode node) {
        int count = 1;
        for (ITreeNode<String, QuestTreeNode> child : node.getChildren()) {
            count += countNodes((QuestTreeNode) child);
        }
        return count;
    }

    // ==============================================
    // 格式化
    // ==============================================

    public static String formatStatus(TaskStatus status) {
        return switch (status) {
            case HIDDEN -> "隐藏";
            case VISIBLE_LOCKED -> "未解锁";
            case ACTIVE -> "进行中";
            case COMPLETED -> "已完成";
        };
    }

    public static String formatBehavior(TaskBehavior behavior) {
        return switch (behavior) {
            case SIMPLE -> "单次完成";
            case MULTI_COMPLETION -> "多次完成";
            case RANDOM_POOL -> "随机池";
            case MULTI_RANDOM_POOL -> "多次随机池";
            case HIDDEN_CHAIN -> "隐藏链";
            case RANDOM_ACTIVATE -> "随机激活";
        };
    }

    public static String formatTaskType(TaskType type) {
        return switch (type) {
            case MAIN -> "主线";
            case SIDE -> "支线";
            case TEMPORARY -> "临时";
        };
    }

    public static String formatDependencyRequirement(String requirement) {
        return switch (requirement) {
            case "ALL_COMPLETED" -> "全部完成";
            case "ONE_COMPLETED" -> "任一完成";
            case "ALL_STARTED" -> "全部开始";
            case "ONE_STARTED" -> "任一开始";
            default -> requirement;
        };
    }

    public static String statusIcon(TaskStatus status) {
        return switch (status) {
            case HIDDEN -> "?";
            case VISIBLE_LOCKED -> "🔒";
            case ACTIVE -> "▶";
            case COMPLETED -> "✅";
        };
    }

    // ==============================================
    // 轻量状态快照（v4.1：服务端仅发送完成情况，客户端基于本地蓝图重建 UI）
    // ==============================================

    /** 单调递增版本号，用于客户端缓存失效判断。 */
    private static int revisionCounter = 0;

    /**
     * 构建轻量任务状态快照 —— 服务端计算所有任务的动态状态后发送给客户端。
     * <p>
     * 客户端收到后基于本地 {@link QuestDefinitionRegistry} 蓝图重建 UI 树和详情，
     * 不再传输完整的 {@link QuestTreeNode} 树或 {@link QuestSyncData.TaskDetail}。
     *
     * @param player 目标玩家
     * @return 所有任务的运行时状态快照
     */
    public static QuestSyncData.QuestStatusSnapshot buildStatusSnapshot(ServerPlayer player) {
        PlayerQuestData data = QuestDataManager.getOrCreate(player);
        ResolutionContext ctx = new ResolutionContext(data,
                QuestDataManager.getEvaluator(player), data.getAllStates());

        QuestSyncData.QuestStatusSnapshot snapshot = new QuestSyncData.QuestStatusSnapshot();
        snapshot.setRevision(++revisionCounter);

        List<QuestSyncData.TaskStatusEntry> entries = snapshot.getEntries();
        for (ITaskDefinition def : QuestDefinitionRegistry.getAllDefinitions()) {
            TaskStatus status = VisibilityCalculator.resolveStatus(def.getId(), ctx);
            boolean hasUnclaimed = false;
            if (status == TaskStatus.COMPLETED) {
                List<ICompletionRecord> records = data.getCompletionRecords(def.getId());
                hasUnclaimed = records.stream().anyMatch(r -> !r.isRewardClaimed());
            }

            entries.add(new QuestSyncData.TaskStatusEntry()
                    .setTaskId(def.getId())
                    .setStatus(status.name())
                    .setCompletions(ctx.getCompletionCount(def.getId()))
                    .setHasUnclaimedReward(hasUnclaimed));
        }
        return snapshot;
    }
}

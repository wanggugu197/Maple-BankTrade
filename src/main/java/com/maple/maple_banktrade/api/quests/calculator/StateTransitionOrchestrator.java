package com.maple.maple_banktrade.api.quests.calculator;

import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.impl.BaseCompletionRecord;
import com.maple.maple_banktrade.api.quests.reward.IReward;
import com.maple.maple_banktrade.api.quests.reward.RewardRegistry;
import com.maple.maple_banktrade.api.quests.tasktype.ITaskType;

import java.util.*;

/**
 * 状态流转编排器：处理任务完成事件，驱动状态流转和批量重算。
 *
 * <p>
 * v3.6 重构：任务类型和奖励通过直接对象引用调用，
 * 不再经过注册表查找。
 */
public final class StateTransitionOrchestrator {

    private StateTransitionOrchestrator() {}

    /**
     * 处理任务完成事件。
     *
     * <p>
     * v3.7 重构：检查全部 {@link ITaskType} 的 {@code canComplete()}；
     * 奖励不再自动发放，改为 {@link #claimRewards} 手动领取。
     *
     * @param taskId         完成的任务 ID
     * @param context        计算上下文
     * @param gameTime       当前游戏时间（tick）
     * @param runtimeContext 运行时上下文（如 {@code ServerPlayer}）
     * @return 所有受影响节点的状态变更列表
     */
    public static List<StateDelta> processComplete(String taskId, ResolutionContext context, long gameTime, Object runtimeContext) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null) {
            return Collections.emptyList();
        }

        IQuestRepository repository = context.getRepository();
        ITaskState state = repository.getOrCreateState(taskId);
        TaskStatus oldStatus = state.getStatus();
        List<StateDelta> deltas = new ArrayList<>();

        // 0. 检查全部任务类型条件（v3.7：全部满足才能完成）
        List<ITaskType> taskTypes = def.getTaskTypes();
        for (ITaskType taskType : taskTypes) {
            if (!taskType.canComplete(def, repository, runtimeContext)) {
                return Collections.emptyList();
            }
        }

        // 1. 按行为模式处理
        TaskBehavior behavior = def.getBehavior();
        boolean shouldRecord = behavior.shouldRecordCompletion();
        boolean shouldTrigger = behavior.shouldTriggerSuccessorImmediately();

        if (shouldRecord) {
            int completionIndex = context.getCompletionCount(taskId) + 1;
            ICompletionRecord record = new BaseCompletionRecord.Builder()
                    .taskId(taskId)
                    .completionIndex(completionIndex)
                    .completedGameTime(gameTime)
                    .realTimeStamp(System.currentTimeMillis())
                    .build();
            repository.addCompletionRecord(record);
        }

        // 2. 根据行为更新状态
        TaskStatus newStatus = switch (behavior) {
            case SIMPLE -> TaskStatus.COMPLETED;
            case MULTI_COMPLETION, MULTI_RANDOM_POOL -> {
                int completionCount = context.getCompletionCount(taskId);
                if (completionCount >= def.getRequiredCompletions()) {
                    yield TaskStatus.COMPLETED;
                } else {
                    state.setCurrentProgress(0);
                    yield TaskStatus.VISIBLE_LOCKED;
                }
            }
            case RANDOM_POOL -> TaskStatus.COMPLETED;
            case HIDDEN_CHAIN -> TaskStatus.COMPLETED;
            case RANDOM_ACTIVATE -> TaskStatus.HIDDEN;
        };

        if (oldStatus != newStatus) {
            state.setStatus(newStatus);
            repository.saveState(state);
            deltas.add(StateDelta.of(taskId, oldStatus, newStatus));
        }

        // 3. 执行全部任务类型副作用（v3.7：全部执行）
        for (ITaskType taskType : taskTypes) {
            taskType.onComplete(def, repository, runtimeContext);
        }

        // 4. 奖励不再自动发放（v3.7），需手动调用 claimRewards()

        // 5. 触发后继链
        if (shouldTrigger && def.getNextTaskInChain() != null && !def.getNextTaskInChain().isEmpty()) {
            ITaskDefinition nextDef = context.getDefinition(def.getNextTaskInChain());
            if (nextDef != null) {
                ITaskState nextState = context.snapshotState(def.getNextTaskInChain());
                if (nextState != null) {
                    TaskStatus nextOldStatus = nextState.getStatus();
                    TaskStatus nextNewStatus = VisibilityCalculator.resolveStatus(def.getNextTaskInChain(), context);
                    if (nextOldStatus != nextNewStatus) {
                        nextState.setStatus(nextNewStatus);
                        repository.saveState(nextState);
                        deltas.add(StateDelta.of(def.getNextTaskInChain(), nextOldStatus, nextNewStatus));
                    }
                }
            }
        }

        // 6. 递归重算子树
        recalculateSubtree(taskId, context, deltas, new HashSet<>());

        return deltas;
    }

    /**
     * 领取任务奖励（v3.7 新增）。
     * 仅当任务已完成、奖励未领取时有效。
     *
     * @param taskId          任务 ID
     * @param completionIndex 领取哪一次完成的奖励（1-based）
     * @param context         计算上下文
     * @param runtimeContext  运行时上下文
     * @return true 表示领取成功
     */
    public static boolean claimRewards(String taskId, int completionIndex,
                                       ResolutionContext context, Object runtimeContext) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null) return false;

        IQuestRepository repository = context.getRepository();
        if (!repository.markRewardClaimed(taskId, completionIndex)) {
            return false;
        }

        List<IReward> rewards = def.getRewards();
        if (rewards != null && !rewards.isEmpty()) {
            RewardRegistry.grantRewards(rewards, runtimeContext);
        }
        return true;
    }

    /**
     * 处理任务接取事件（VISIBLE_LOCKED → ACTIVE）。
     */
    public static StateDelta processActivate(String taskId, ResolutionContext context) {
        IQuestRepository repository = context.getRepository();
        ITaskState state = repository.getOrCreateState(taskId);
        TaskStatus oldStatus = state.getStatus();

        if (oldStatus != TaskStatus.VISIBLE_LOCKED) {
            return StateDelta.unchanged(taskId, oldStatus);
        }

        state.setStatus(TaskStatus.ACTIVE);
        state.setActiveTimestamp(System.currentTimeMillis());
        repository.saveState(state);
        return StateDelta.of(taskId, oldStatus, TaskStatus.ACTIVE);
    }

    /**
     * 处理隐藏链的概率触发。
     */
    public static List<StateDelta> tryTriggerHiddenChain(String taskId, ResolutionContext context, double random) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null || def.getBehavior() != TaskBehavior.HIDDEN_CHAIN) {
            return Collections.emptyList();
        }
        if (random >= def.getNextChainTriggerChance()) {
            return Collections.emptyList();
        }
        return triggerChainSuccessor(taskId, context);
    }

    // ==============================================
    // 内部方法
    // ==============================================

    private static List<StateDelta> triggerChainSuccessor(String taskId, ResolutionContext context) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null) return Collections.emptyList();

        String nextId = def.getNextTaskInChain();
        if (nextId == null || nextId.isEmpty()) return Collections.emptyList();

        IQuestRepository repository = context.getRepository();
        ITaskState nextState = context.snapshotState(nextId);
        if (nextState == null) return Collections.emptyList();

        TaskStatus oldStatus = nextState.getStatus();
        TaskStatus newStatus = VisibilityCalculator.resolveStatus(nextId, context);
        if (oldStatus == newStatus) return Collections.emptyList();

        nextState.setStatus(newStatus);
        repository.saveState(nextState);
        return Collections.singletonList(StateDelta.of(nextId, oldStatus, newStatus));
    }

    private static void recalculateSubtree(String taskId, ResolutionContext context,
                                           List<StateDelta> deltas, Set<String> visited) {
        if (!visited.add(taskId)) return;

        IQuestRepository repository = context.getRepository();
        List<String> children = TreeTraversalService.getDescendants(taskId, repository);

        for (String childId : children) {
            if (visited.contains(childId)) continue;
            visited.add(childId);

            ITaskState childState = context.snapshotState(childId);
            if (childState == null) continue;

            TaskStatus oldStatus = childState.getStatus();
            TaskStatus newStatus = VisibilityCalculator.resolveStatus(childId, context);

            if (oldStatus != newStatus) {
                childState.setStatus(newStatus);
                repository.saveState(childState);
                deltas.add(StateDelta.of(childId, oldStatus, newStatus));
                recalculateSubtree(childId, context, deltas, visited);
            }
        }
    }
}

package com.maple.maple_banktrade.api.quests.calculator;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.impl.BaseCompletionRecord;
import com.maple.maple_banktrade.api.quests.reward.RewardDef;
import com.maple.maple_banktrade.api.quests.reward.RewardRegistry;
import com.maple.maple_banktrade.api.quests.tasktype.ITaskType;
import com.maple.maple_banktrade.api.quests.tasktype.TaskTypeRegistry;

import java.util.*;

/**
 * 状态流转编排器：处理任务完成事件，驱动状态流转和批量重算。
 *
 * <p>
 * 核心职责：
 * <ul>
 * <li>接收任务完成事件，按行为模式差异化处理</li>
 * <li>创建/撤销完成记录</li>
 * <li>决定是否触发后继链</li>
 * <li>递归重算受影响子树</li>
 * <li>返回 {@link StateDelta} 列表供外部应用</li>
 * </ul>
 */
public final class StateTransitionOrchestrator {

    private StateTransitionOrchestrator() {
        // 工具类，禁止实例化
    }

    /**
     * 处理任务完成事件。
     *
     * @param taskId         完成的任务 ID
     * @param context        计算上下文
     * @param gameTime       当前游戏时间（tick），用于创建完成记录
     * @param runtimeContext 运行时上下文（如 {@code ServerPlayer}），用于任务类型检查和奖励发放
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

        // 0. 检查任务类型条件
        Identifier typeId = def.getTaskTypeId();
        if (typeId != null) {
            ITaskType taskType = TaskTypeRegistry.get(typeId);
            if (taskType != null && !taskType.canComplete(def, repository, runtimeContext)) {
                return Collections.emptyList(); // 不满足完成条件，拒绝
            }
        }

        // 1. 按行为模式处理
        TaskBehavior behavior = def.getBehavior();
        boolean shouldRecord = behavior.shouldRecordCompletion();
        boolean shouldTrigger = behavior.shouldTriggerSuccessorImmediately();

        if (shouldRecord) {
            // 创建完成记录
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

        // 3. 记录主任务变更
        if (oldStatus != newStatus) {
            state.setStatus(newStatus);
            repository.saveState(state);
            deltas.add(StateDelta.of(taskId, oldStatus, newStatus));
        }

        // 3.5. 执行任务类型副作用（如扣除物品）
        if (typeId != null) {
            ITaskType taskType = TaskTypeRegistry.get(typeId);
            if (taskType != null) {
                taskType.onComplete(def, repository, runtimeContext);
            }
        }

        // 3.6. 发放奖励
        List<RewardDef> rewardDefs = def.getRewards();
        if (rewardDefs != null && !rewardDefs.isEmpty()) {
            RewardRegistry.grantRewards(rewardDefs, runtimeContext);
        }

        // 4. 触发后继链
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

        // 5. 递归重算子树
        recalculateSubtree(taskId, context, deltas, new HashSet<>());

        return deltas;
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
     *
     * @param taskId  隐藏链任务 ID
     * @param context 计算上下文
     * @param random  随机数（0.0~1.0），小于 nextChainTriggerChance 则触发
     * @return 状态变更列表
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

    /**
     * 触发后继链（nextTaskInChain）。
     */
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

    /**
     * 递归重算子树中所有节点的可见性。
     */
    private static void recalculateSubtree(String taskId, ResolutionContext context,
                                           List<StateDelta> deltas, Set<String> visited) {
        if (!visited.add(taskId)) {
            return;
        }

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

                // 递归计算子节点的子节点
                recalculateSubtree(childId, context, deltas, visited);
            }
        }
    }
}

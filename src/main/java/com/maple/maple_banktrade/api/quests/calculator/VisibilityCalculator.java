package com.maple.maple_banktrade.api.quests.calculator;

import com.maple.maple_banktrade.api.quests.condition.BaseQuestCondition;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;

/**
 * 核心可见性计算器。
 *
 * <p>
 * v3.6 重构：条件评估改为直接调用 {@code condition.evaluate()}，
 * 不再经过 {@code Identifier} + {@code CompoundTag} → 注册表查找 → 工厂创建。
 *
 * <p>
 * 检查顺序（短路求值）：
 * <ol>
 * <li>已完成且非循环 → 永久 COMPLETED</li>
 * <li>父节点 HIDDEN（非 forceParentVisible）→ HIDDEN</li>
 * <li>prevSiblingId 未严格完成 → HIDDEN</li>
 * <li>dependentNodes 未有效完成 → HIDDEN</li>
 * <li>unlockCondition 不满足 → HIDDEN</li>
 * <li>当前 ACTIVE → ACTIVE</li>
 * <li>visibilityCondition 不满足 → HIDDEN</li>
 * <li>否则 → VISIBLE_LOCKED</li>
 * </ol>
 */
public final class VisibilityCalculator {

    private VisibilityCalculator() {}

    /**
     * 计算单个任务节点的目标状态。
     */
    public static TaskStatus resolveStatus(String taskId, ResolutionContext context) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null) {
            return TaskStatus.HIDDEN;
        }

        ITaskState state = context.snapshotState(taskId);
        TaskStatus currentStatus = state != null ? state.getStatus() : TaskStatus.HIDDEN;

        // 1. 已完成且非循环 → 永久完成
        if (currentStatus == TaskStatus.COMPLETED) {
            if (!def.isRepeatable() || isMaxRepeatsReached(def, context)) {
                return TaskStatus.COMPLETED;
            }
        }

        // 2. 检查隐藏条件
        if (isHidden(def, context)) {
            return TaskStatus.HIDDEN;
        }

        // 3. 如果当前已是 ACTIVE，保持不变
        if (currentStatus == TaskStatus.ACTIVE) {
            return TaskStatus.ACTIVE;
        }

        // 4. 可见性条件检查（v3.6：直接调用条件对象）
        BaseQuestCondition visCond = def.getVisibilityCondition();
        if (visCond != null && !visCond.evaluate(context.getEvaluationContext())) {
            return TaskStatus.HIDDEN;
        }

        // 5. 默认：可见但锁定
        return TaskStatus.VISIBLE_LOCKED;
    }

    // ==============================================
    // 隐藏条件检查
    // ==============================================

    private static boolean isHidden(ITaskDefinition def, ResolutionContext context) {
        // 父节点检查
        String parentId = def.getParentId();
        if (parentId != null && !parentId.isEmpty()) {
            TaskStatus parentStatus = context.snapshotStatus(parentId);
            if (parentStatus == TaskStatus.HIDDEN && !def.isForceParentVisible()) {
                return true;
            }
        }

        // 兄弟链检查
        String prevSiblingId = def.getPrevSiblingId();
        if (prevSiblingId != null && !prevSiblingId.isEmpty()) {
            if (!context.isStrictlyCompleted(prevSiblingId)) {
                return true;
            }
        }

        // 依赖检查
        if (!areDependenciesSatisfied(def, context)) {
            return true;
        }

        // 解锁条件检查（v3.6：直接调用条件对象）
        BaseQuestCondition unlockCond = def.getUnlockCondition();
        if (unlockCond != null && !unlockCond.evaluate(context.getEvaluationContext())) {
            return true;
        }

        return false;
    }

    // ==============================================
    // 辅助方法
    // ==============================================

    private static boolean areDependenciesSatisfied(ITaskDefinition def, ResolutionContext context) {
        java.util.List<String> deps = def.getDependentNodes();
        if (deps == null || deps.isEmpty()) {
            return true;
        }

        DependencyRequirement requirement = def.getDependencyRequirement();
        return switch (requirement) {
            case ALL_COMPLETED -> deps.stream().allMatch(context::isEffectivelyFinished);
            case ONE_COMPLETED -> deps.stream().anyMatch(context::isEffectivelyFinished);
            case ALL_STARTED -> deps.stream().allMatch(depId -> {
                TaskStatus status = context.snapshotStatus(depId);
                return status == TaskStatus.ACTIVE || status == TaskStatus.COMPLETED;
            });
            case ONE_STARTED -> deps.stream().anyMatch(depId -> {
                TaskStatus status = context.snapshotStatus(depId);
                return status == TaskStatus.ACTIVE || status == TaskStatus.COMPLETED;
            });
        };
    }

    private static boolean isMaxRepeatsReached(ITaskDefinition def, ResolutionContext context) {
        int maxRepeats = def.getMaxRepeatTimes();
        if (maxRepeats < 0) return false;
        return context.getCompletionCount(def.getId()) >= maxRepeats;
    }
}

package com.maple.maple_banktrade.api.quests.condition;

import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 计算上下文，封装仓储快照和条件评估上下文。
 *
 * <p>
 * v3.6 重构：移除 {@code evaluateCondition(Identifier, CompoundTag, String)}，
 * 条件现直接存储在任务定义中，通过 {@link #getEvaluationContext()} 直接评估。
 */
@Getter
public class ResolutionContext {

    private final IQuestRepository repository;
    private final EvaluationContext evaluationContext;
    private final Map<String, ITaskState> stateSnapshot;

    /**
     * 创建计算上下文。
     *
     * @param repository        任务仓储
     * @param evaluationContext 条件评估上下文（可为 null）
     * @param stateSnapshot     当前状态快照
     */
    public ResolutionContext(IQuestRepository repository, EvaluationContext evaluationContext,
                             Map<String, ITaskState> stateSnapshot) {
        this.repository = repository;
        this.evaluationContext = evaluationContext != null ? evaluationContext : EvaluationContext.empty();
        this.stateSnapshot = stateSnapshot;
    }

    // ==============================================
    // 查询方法
    // ==============================================

    public ITaskDefinition getDefinition(String taskId) {
        return repository.getDefinition(taskId).orElse(null);
    }

    public ITaskState snapshotState(String taskId) {
        return stateSnapshot.get(taskId);
    }

    public TaskStatus snapshotStatus(String taskId) {
        ITaskState state = snapshotState(taskId);
        return state != null ? state.getStatus() : TaskStatus.HIDDEN;
    }

    public List<ICompletionRecord> getCompletionRecords(String taskId) {
        return repository.getCompletionRecords(taskId);
    }

    public int getCompletionCount(String taskId) {
        return repository.getCompletionRecords(taskId).size();
    }

    // ==============================================
    // 条件判断
    // ==============================================

    public boolean isStrictlyCompleted(String taskId) {
        return snapshotStatus(taskId) == TaskStatus.COMPLETED;
    }

    public boolean isEffectivelyFinished(String taskId) {
        ITaskDefinition def = getDefinition(taskId);
        if (def != null && !def.getBehavior().shouldRecordCompletion()) {
            return false;
        }
        if (snapshotStatus(taskId) == TaskStatus.COMPLETED) {
            return true;
        }
        return !getCompletionRecords(taskId).isEmpty();
    }
}

package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 计算上下文，封装仓储快照和脚本引擎，为 {@code VisibilityCalculator} 提供统一查询接口。
 *
 * <p>
 * 在计算可见性时，使用 {@link #snapshotStatus(String)} 查询当前快照中的状态，
 * 而非直接查询仓储，避免在批量重算过程中出现状态不一致。
 */
@Getter
public class ResolutionContext {

    private final IQuestRepository repository;
    private final IScriptEvaluator scriptEvaluator;
    private final Map<String, ITaskState> stateSnapshot;

    /**
     * 创建计算上下文。
     *
     * @param repository      任务仓储
     * @param scriptEvaluator 脚本引擎（可为 {@link IScriptEvaluator#noOp()}）
     * @param stateSnapshot   当前状态快照（通常来自 {@link IQuestRepository#getAllStates()}）
     */
    public ResolutionContext(IQuestRepository repository, IScriptEvaluator scriptEvaluator,
                             Map<String, ITaskState> stateSnapshot) {
        this.repository = repository;
        this.scriptEvaluator = scriptEvaluator;
        this.stateSnapshot = stateSnapshot;
    }

    // ==============================================
    // 查询方法
    // ==============================================

    /**
     * 获取任务定义（从仓储中查询）。
     */
    public ITaskDefinition getDefinition(String taskId) {
        return repository.getDefinition(taskId).orElse(null);
    }

    /**
     * 获取快照中的任务状态（避免在批量计算中读到未提交的中间状态）。
     */
    public ITaskState snapshotState(String taskId) {
        return stateSnapshot.get(taskId);
    }

    /**
     * 获取快照中的任务状态枚举值。
     */
    public TaskStatus snapshotStatus(String taskId) {
        ITaskState state = snapshotState(taskId);
        return state != null ? state.getStatus() : TaskStatus.HIDDEN;
    }

    /**
     * 获取某任务的完成记录列表。
     */
    public List<ICompletionRecord> getCompletionRecords(String taskId) {
        return repository.getCompletionRecords(taskId);
    }

    /**
     * 获取某任务的完成次数。
     */
    public int getCompletionCount(String taskId) {
        return repository.getCompletionRecords(taskId).size();
    }

    // ==============================================
    // 条件判断
    // ==============================================

    /**
     * 判断某任务是否"严格完成"（状态为 COMPLETED）。
     * 用于兄弟链检查：前驱必须状态为 COMPLETED 才能解锁后继。
     */
    public boolean isStrictlyCompleted(String taskId) {
        return snapshotStatus(taskId) == TaskStatus.COMPLETED;
    }

    /**
     * 判断某任务是否"有效完成"（用于依赖检查）。
     * 状态为 COMPLETED 或有完成记录即视为有效完成。
     * 随机激活任务永远不算有效完成。
     */
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

    /**
     * 评估条件。
     *
     * @param conditionId 条件 ID（可为 null 表示无条件）
     * @param params      条件参数（不可为 null）
     * @param taskId      任务 ID
     */
    public boolean evaluateCondition(Identifier conditionId, CompoundTag params, String taskId) {
        if (conditionId == null) {
            return true;
        }
        if (params == null) {
            params = new CompoundTag();
        }
        return scriptEvaluator.evaluate(conditionId, params, taskId);
    }
}

package com.maple.maple_banktrade.api.quests.core;

import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.impl.BaseCompletionRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 统一数据访问接口（定义与状态+历史）
 */
public interface IQuestRepository {

    // ----- 定义查询 -----
    Optional<ITaskDefinition> getDefinition(String id);

    List<ITaskDefinition> getRoots();

    List<ITaskDefinition> getChildren(String parentId);

    // ----- 状态查询 & 修改 -----
    ITaskState getOrCreateState(String taskId);   // 若不存在则新建（状态为HIDDEN）

    Map<String, ITaskState> getAllStates();       // 全量快照（用于批量计算）

    void saveState(ITaskState state);             // 持久化（标记脏数据）

    // ----- 历史记录操作 -----
    List<ICompletionRecord> getCompletionRecords(String taskId);

    void addCompletionRecord(ICompletionRecord record);

    void pruneRecords(String taskId, int maxKeep); // 裁剪旧记录

    /**
     * 标记奖励已领取，并将更新后的记录写回仓储。
     *
     * <p>
     * v3.7 改进：优先使用直接替换（避免 pruneRecords 排序不确定性），
     * 当具体仓储实现为 {@code PlayerQuestData} 时直接替换记录。
     *
     * @param taskId          任务 ID
     * @param completionIndex 完成序号（1-based）
     * @return true 表示成功标记，false 表示记录不存在或已领取
     */
    default boolean markRewardClaimed(String taskId, int completionIndex) {
        List<ICompletionRecord> records = getCompletionRecords(taskId);
        if (records.isEmpty() || completionIndex < 1 || completionIndex > records.size()) {
            return false;
        }
        ICompletionRecord record = records.get(completionIndex - 1);
        if (record.isRewardClaimed()) return false;
        if (record instanceof BaseCompletionRecord baseRecord) {
            BaseCompletionRecord updated = baseRecord.markRewardClaimed();
            // 尝试直接替换（PlayerQuestData 支持），否则使用 add + prune 兜底
            if (this instanceof com.maple.maple_banktrade.api.quests.repository.PlayerQuestData pd) {
                return pd.replaceCompletionRecord(taskId, completionIndex - 1, updated);
            }
            addCompletionRecord(updated);
            pruneRecords(taskId, records.size());
            return true;
        }
        return false;
    }

    // ==============================================
    // 便利查询方法（默认实现基于 getDefinition / getAllDefinitions）
    // ==============================================

    /**
     * 获取所有已注册的任务定义。
     * 默认抛 {@link UnsupportedOperationException}，具体仓储需覆盖实现。
     */
    default List<ITaskDefinition> getAllDefinitions() {
        throw new UnsupportedOperationException("getAllDefinitions not implemented");
    }

    /**
     * 按类型筛选任务定义。
     */
    default List<ITaskDefinition> getDefinitionsByType(TaskType type) {
        return getAllDefinitions().stream()
                .filter(d -> d.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 检查指定 ID 的任务定义是否存在。
     */
    default boolean hasDefinition(String id) {
        return getDefinition(id).isPresent();
    }

    /**
     * 获取已注册的任务定义总数。
     */
    default int getDefinitionCount() {
        return getAllDefinitions().size();
    }
}

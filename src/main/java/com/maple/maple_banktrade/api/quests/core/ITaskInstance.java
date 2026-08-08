package com.maple.maple_banktrade.api.quests.core;

import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;

import java.util.List;

/**
 * 任务实例 = 静态定义 + 当前玩家状态 + 历史记录（可选通过仓储获取）
 * 便于外部调用方一次获取完整上下文
 */
public interface ITaskInstance {

    ITaskDefinition getDefinition();

    ITaskState getState();

    // 返回该任务的所有完成记录（从仓储获取，实现类可能需持有仓储引用）
    List<ICompletionRecord> getCompletionRecords();

    // ==============================================
    // 便利方法
    // ==============================================

    /**
     * 判断是否"有效完成"（用于依赖检查）。
     * 随机激活任务永远不算"有效完成"，因为它总是可重新触发。
     */
    default boolean isEffectivelyFinished() {
        if (getDefinition().getBehavior() == TaskBehavior.RANDOM_ACTIVATE) {
            return false;
        }
        return getState().getStatus() == TaskStatus.COMPLETED || !getCompletionRecords().isEmpty();
    }

    /**
     * 获取该任务的累计完成次数（从完成记录中计算）。
     */
    default int getCompletionCount() {
        List<ICompletionRecord> records = getCompletionRecords();
        return records != null ? records.size() : 0;
    }
}

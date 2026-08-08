package com.maple.maple_banktrade.api.quests.core;

import com.maple.maple_banktrade.api.quests.enums.TaskStatus;

/**
 * 可变的任务进度状态（每个玩家一份）
 */
public interface ITaskState {

    String getTaskId();

    TaskStatus getStatus();

    void setStatus(TaskStatus status);

    int getCurrentProgress();

    void setCurrentProgress(int progress);

    long getActiveTimestamp();    // 进入 ACTIVE 状态时的游戏时间（tick）

    void setActiveTimestamp(long timestamp);

    // ==============================================
    // 便利方法
    // ==============================================

    default void addProgress(int delta) {
        setCurrentProgress(getCurrentProgress() + delta);
    }

    /** 当前任务是否处于进行中状态。 */
    default boolean isActive() {
        return getStatus() == TaskStatus.ACTIVE;
    }

    /** 当前任务是否已完成。 */
    default boolean isCompleted() {
        return getStatus() == TaskStatus.COMPLETED;
    }

    /** 当前任务是否处于隐藏状态。 */
    default boolean isHidden() {
        return getStatus() == TaskStatus.HIDDEN;
    }

    /** 当前任务是否可见（非隐藏）。 */
    default boolean isVisible() {
        return getStatus().isVisible();
    }
}

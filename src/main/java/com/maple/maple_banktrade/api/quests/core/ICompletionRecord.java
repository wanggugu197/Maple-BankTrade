package com.maple.maple_banktrade.api.quests.core;

import java.util.Map;

/**
 * 不可变的完成记录（只追加）。
 *
 * <p>
 * v3.7 新增：{@link #isRewardClaimed()} 追踪奖励领取状态。
 * 奖励不再自动发放，需玩家在 UI 手动领取。
 */
public interface ICompletionRecord {

    String getTaskId();

    /** 第几次完成（从1开始）。 */
    int getCompletionIndex();

    /** 完成时的游戏总 tick。 */
    long getCompletedGameTime();

    /** 系统毫秒时间戳。 */
    long getRealTimeStamp();

    /** 扩展快照数据。 */
    Map<String, Object> getSnapshotData();

    /** 奖励是否已领取。 */
    boolean isRewardClaimed();

    /**
     * 是否携带快照数据（扩展数据非空）。
     */
    default boolean hasSnapshot() {
        Map<String, Object> snapshot = getSnapshotData();
        return snapshot != null && !snapshot.isEmpty();
    }
}

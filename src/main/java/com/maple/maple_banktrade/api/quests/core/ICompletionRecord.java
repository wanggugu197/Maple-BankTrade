package com.maple.maple_banktrade.api.quests.core;

import java.util.Map;

/**
 * 不可变的完成记录（只追加）
 */
public interface ICompletionRecord {

    String getTaskId();

    int getCompletionIndex();       // 第几次完成（从1开始）

    long getCompletedGameTime();    // 完成时的游戏总 tick

    long getRealTimeStamp();        // 系统毫秒时间戳

    Map<String, Object> getSnapshotData(); // 扩展快照数据

    /**
     * 是否携带快照数据（扩展数据非空）。
     */
    default boolean hasSnapshot() {
        Map<String, Object> snapshot = getSnapshotData();
        return snapshot != null && !snapshot.isEmpty();
    }
}

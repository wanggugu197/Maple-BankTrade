package com.maple.maple_banktrade.api.quests.impl;

import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 不可变的完成记录（只追加）。
 *
 * <p>
 * v3.7 新增：{@code rewardClaimed} 字段，追踪奖励领取状态。
 * 通过 {@link #markRewardClaimed()} 创建已领取的副本。
 */
@Getter
public class BaseCompletionRecord implements ICompletionRecord {

    private final String taskId;
    private final int completionIndex;
    private final long completedGameTime;
    private final long realTimeStamp;
    private final Map<String, Object> snapshotData;
    private final boolean rewardClaimed;

    private BaseCompletionRecord(Builder builder) {
        this.taskId = builder.taskId;
        this.completionIndex = builder.completionIndex;
        this.completedGameTime = builder.completedGameTime;
        this.realTimeStamp = builder.realTimeStamp;
        this.snapshotData = Collections.unmodifiableMap(new HashMap<>(builder.snapshotData));
        this.rewardClaimed = builder.rewardClaimed;
    }

    /**
     * 创建已领取奖励的副本（不可变模式）。
     */
    public BaseCompletionRecord markRewardClaimed() {
        if (rewardClaimed) return this;
        return new Builder()
                .taskId(taskId)
                .completionIndex(completionIndex)
                .completedGameTime(completedGameTime)
                .realTimeStamp(realTimeStamp)
                .snapshotData(new HashMap<>(snapshotData))
                .rewardClaimed(true)
                .build();
    }

    @Override
    public String toString() {
        return "BaseCompletionRecord{" +
                "taskId='" + taskId + '\'' +
                ", index=" + completionIndex +
                ", gameTime=" + completedGameTime +
                ", rewardClaimed=" + rewardClaimed +
                '}';
    }

    // ==============================================
    // Builder
    // ==============================================

    @Accessors(chain = true, fluent = true)
    @Setter(AccessLevel.PUBLIC)
    @Getter(AccessLevel.PACKAGE)
    public static class Builder {

        private String taskId;
        private int completionIndex;
        private long completedGameTime;
        private long realTimeStamp;
        private Map<String, Object> snapshotData = new HashMap<>();
        private boolean rewardClaimed = false;

        public Builder addSnapshot(String key, Object value) {
            this.snapshotData.put(key, value);
            return this;
        }

        /**
         * @throws IllegalStateException 如果 taskId 为空
         */
        public BaseCompletionRecord build() {
            if (taskId == null || taskId.isEmpty()) {
                throw new IllegalStateException("taskId must not be null");
            }
            return new BaseCompletionRecord(this);
        }
    }
}

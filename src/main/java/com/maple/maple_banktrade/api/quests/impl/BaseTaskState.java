package com.maple.maple_banktrade.api.quests.impl;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 可变的任务进度状态（每个玩家一份）。
 *
 * <p>
 * v3.8 重构：实现 {@link IPersistedSerializable}，通过 {@code @Persisted} 注解
 * 替代手动 {@code RecordCodecBuilder}，简化持久化代码。
 */
@Getter
@Setter
public class BaseTaskState implements ITaskState, IPersistedSerializable {

    @Persisted
    private String taskId;

    @Persisted
    private TaskStatus status = TaskStatus.HIDDEN;

    @Persisted
    private int currentProgress = 0;

    @Persisted
    private long activeTimestamp = 0;

    /** 无参构造（供 PersistedParser 反序列化使用）。 */
    public BaseTaskState() {}

    /** 创建新状态，默认为 {@link TaskStatus#HIDDEN}。 */
    public BaseTaskState(String taskId) {
        this.taskId = taskId;
    }

    /**
     * 创建当前状态的深拷贝（用于快照或回滚）。
     */
    public BaseTaskState copy() {
        BaseTaskState copy = new BaseTaskState(this.taskId);
        copy.status = this.status;
        copy.currentProgress = this.currentProgress;
        copy.activeTimestamp = this.activeTimestamp;
        return copy;
    }

    @Override
    public String toString() {
        return "BaseTaskState{" +
                "taskId='" + taskId + '\'' +
                ", status=" + status +
                ", progress=" + currentProgress +
                '}';
    }
}

package com.maple.maple_banktrade.api.quests.impl;

import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 可变的任务进度状态（每个玩家一份）。
 */
@Getter
@Setter
public class BaseTaskState implements ITaskState {

    private final String taskId;
    private TaskStatus status;
    private int currentProgress;
    private long activeTimestamp;

    /** 创建新状态，默认为 {@link TaskStatus#HIDDEN}。 */
    public BaseTaskState(String taskId) {
        this.taskId = taskId;
        this.status = TaskStatus.HIDDEN;
        this.currentProgress = 0;
        this.activeTimestamp = 0;
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

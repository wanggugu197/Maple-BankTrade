package com.maple.maple_banktrade.api.quests.calculator;

import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 状态变更记录，由 {@code StateTransitionOrchestrator} 产出，
 * 外部调用方负责将变更应用到仓储。
 */
@Getter
@ToString
@AllArgsConstructor
public class StateDelta {

    /** 受影响的任务 ID。 */
    private final String taskId;

    /** 变更前状态。 */
    private final TaskStatus oldStatus;

    /** 变更后状态。 */
    private final TaskStatus newStatus;

    /**
     * 是否为有效变更（新旧状态不同）。
     */
    public boolean isChanged() {
        return oldStatus != newStatus;
    }

    /**
     * 创建无变更的 delta（用于标记无需更新）。
     */
    public static StateDelta unchanged(String taskId, TaskStatus status) {
        return new StateDelta(taskId, status, status);
    }

    /**
     * 创建状态变更的 delta。
     */
    public static StateDelta of(String taskId, TaskStatus oldStatus, TaskStatus newStatus) {
        return new StateDelta(taskId, oldStatus, newStatus);
    }
}

package com.maple.maple_banktrade.api.quests.enums;

/**
 * 任务节点的四态。
 *
 * <p>
 * 状态流转：
 * 
 * <pre>
 *   HIDDEN → VISIBLE_LOCKED → ACTIVE → COMPLETED
 *      ↑                              │
 *      └──────────────────────────────┘ (循环任务重置 / 随机激活)
 * </pre>
 */
public enum TaskStatus {

    /** 隐藏：不可见，不参与任何 UI 展示 */
    HIDDEN,
    /** 可见但锁定：显示在 UI 中，但无法接取/进行 */
    VISIBLE_LOCKED,
    /** 进行中：玩家已接取，正在追踪进度 */
    ACTIVE,
    /** 已完成：当前周期结束（对于循环任务可能进入重置） */
    COMPLETED;

    /**
     * 该状态下任务是否对玩家可见（UI 中可展示）。
     */
    public boolean isVisible() {
        return this != HIDDEN;
    }

    /**
     * 该状态下任务是否可交互（可接取或进行中）。
     */
    public boolean isInteractive() {
        return this == ACTIVE || this == VISIBLE_LOCKED;
    }

    /**
     * 该状态是否为终态（已完成）。
     */
    public boolean isTerminal() {
        return this == COMPLETED;
    }
}

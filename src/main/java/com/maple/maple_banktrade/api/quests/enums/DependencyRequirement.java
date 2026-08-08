package com.maple.maple_banktrade.api.quests.enums;

/**
 * 依赖满足模式，定义前置任务需要满足什么条件才能解锁当前任务。
 *
 * <ul>
 * <li>{@link #ALL_COMPLETED} —— 所有依赖任务必须完成（默认）</li>
 * <li>{@link #ONE_COMPLETED} —— 至少一个依赖任务完成</li>
 * <li>{@link #ALL_STARTED} —— 所有依赖任务必须已开始（ACTIVE 或 COMPLETED）</li>
 * <li>{@link #ONE_STARTED} —— 至少一个依赖任务已开始</li>
 * </ul>
 */
public enum DependencyRequirement {

    /** 所有依赖任务必须完成（默认）。 */
    ALL_COMPLETED("all_completed", false, true),
    /** 至少一个依赖任务完成。 */
    ONE_COMPLETED("one_completed", true, true),
    /** 所有依赖任务必须已开始（ACTIVE 或 COMPLETED）。 */
    ALL_STARTED("all_started", false, false),
    /** 至少一个依赖任务已开始。 */
    ONE_STARTED("one_started", true, false);

    private final String id;
    private final boolean needOne;
    private final boolean completed;

    DependencyRequirement(String id, boolean needOne, boolean completed) {
        this.id = id;
        this.needOne = needOne;
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    /**
     * @return 是否只需要一个依赖满足（而非全部）
     */
    public boolean needOnlyOne() {
        return needOne;
    }

    /**
     * @return 是否需要依赖任务处于完成状态（而非仅开始）
     */
    public boolean needCompletion() {
        return completed;
    }
}

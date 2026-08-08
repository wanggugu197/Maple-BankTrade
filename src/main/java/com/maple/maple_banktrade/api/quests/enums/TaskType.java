package com.maple.maple_banktrade.api.quests.enums;

/**
 * 任务类型，定义任务在游戏中的分类与调度优先级。
 *
 * <p>
 * 不同类型影响任务的调度方式：
 * <ul>
 * <li>{@link #MAIN} —— 按兄弟链顺序自动推进，不可跳过</li>
 * <li>{@link #SIDE} —— 依赖前置节点解锁，可手动/自动接取</li>
 * <li>{@link #TEMPORARY} —— 随机/时间触发，可重复完成，由逻辑层控制数量上限</li>
 * </ul>
 */
public enum TaskType {
    /** 主线任务：剧情推进的核心任务链，通常不可跳过 */
    MAIN,
    /** 支线任务：可选任务，通常依赖主线进度解锁 */
    SIDE,
    /** 临时任务：循环/每日/随机触发类任务，可重复完成 */
    TEMPORARY
}

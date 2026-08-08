package com.maple.maple_banktrade.api.quests.enums;

/**
 * 任务行为模式，定义任务完成后的状态流转及后继触发规则。
 *
 * <p>
 * 六种行为模式覆盖了常见的 RPG 任务设计需求：
 * <ul>
 * <li>{@link #SIMPLE} —— 普通单次完成</li>
 * <li>{@link #MULTI_COMPLETION} —— 多次完成计数</li>
 * <li>{@link #RANDOM_POOL} —— 随机池（单次）</li>
 * <li>{@link #MULTI_RANDOM_POOL} —— 随机池（多次）</li>
 * <li>{@link #HIDDEN_CHAIN} —— 隐藏链（概率触发）</li>
 * <li>{@link #RANDOM_ACTIVATE} —— 随机激活（可重复触发）</li>
 * </ul>
 */
public enum TaskBehavior {

    /** 普通单次任务：完成一次即结束，立即触发后继链（若有） */
    SIMPLE,
    /** 多次完成：需完成指定次数（requiredCompletions）才可触发后继 */
    MULTI_COMPLETION,
    /** 随机池：激活时从 poolIds 中随机选一个子任务执行，子任务完成后本任务即完成 */
    RANDOM_POOL,
    /** 多次随机池：需完成指定次数，每次激活时从 poolIds 中随机选子任务 */
    MULTI_RANDOM_POOL,
    /** 隐藏链：完成后不立即解锁后继，而是按概率（nextChainTriggerChance）随机触发 */
    HIDDEN_CHAIN,
    /** 随机激活：完成后自动变为 HIDDEN，可再次被随机触发为 ACTIVE（不记录永久完成） */
    RANDOM_ACTIVATE;

    // ==============================================
    // 便利方法
    // ==============================================

    /**
     * 该行为模式是否支持多次完成计数。
     */
    public boolean isMultiCompletion() {
        return this == MULTI_COMPLETION || this == MULTI_RANDOM_POOL;
    }

    /**
     * 该行为模式是否使用随机池机制。
     */
    public boolean isRandomPool() {
        return this == RANDOM_POOL || this == MULTI_RANDOM_POOL;
    }

    /**
     * 该行为模式是否为隐藏链（概率触发后继）。
     */
    public boolean isHiddenChain() {
        return this == HIDDEN_CHAIN;
    }

    /**
     * 完成时是否需要记录永久完成记录。
     * {@link #RANDOM_ACTIVATE} 不记录，因为它总是可重新触发。
     */
    public boolean shouldRecordCompletion() {
        return this != RANDOM_ACTIVATE;
    }

    /**
     * 完成后是否立即触发后继链。
     * {@link #HIDDEN_CHAIN} 和 {@link #RANDOM_ACTIVATE} 不立即触发。
     */
    public boolean shouldTriggerSuccessorImmediately() {
        return this != HIDDEN_CHAIN && this != RANDOM_ACTIVATE;
    }

    /**
     * 该行为模式是否需要配置 {@code poolIds}。
     */
    public boolean requiresPool() {
        return this == RANDOM_POOL || this == MULTI_RANDOM_POOL;
    }

    /**
     * 该行为模式是否天然支持重复完成（无需显式设置 repeatable）。
     * {@link #RANDOM_ACTIVATE} 天然支持重复。
     */
    public boolean supportsRepeat() {
        return this == RANDOM_ACTIVATE;
    }
}

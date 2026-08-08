package com.maple.maple_banktrade.api.quests.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.reward.RewardDef;

import java.util.Collections;
import java.util.List;

/**
 * 不可变的任务定义（数据驱动）
 */
public interface ITaskDefinition {

    String getId();

    TaskType getType();          // 定义 TaskType 枚举：MAIN, SIDE, TEMPORARY

    boolean isGroup();

    String getParentId();

    String getPrevSiblingId();

    List<String> getDependentNodes();

    String getNextTaskInChain();

    boolean isRepeatable();

    int getMaxRepeatTimes();

    boolean isForceParentVisible();

    /**
     * @return 可见性脚本条件 ID（可为 null 表示无条件）
     */
    Identifier getVisibilityConditionId();

    /**
     * @return 可见性脚本条件参数（不可为 null，无参数时返回空 CompoundTag）
     */
    CompoundTag getVisibilityConditionParams();

    /**
     * @return 解锁脚本条件 ID（可为 null 表示无条件）
     */
    Identifier getUnlockConditionId();

    /**
     * @return 解锁脚本条件参数（不可为 null，无参数时返回空 CompoundTag）
     */
    CompoundTag getUnlockConditionParams();

    // 可选：获取预计算的子节点列表（由加载器填充）
    List<String> getChildrenIds();

    // 辅助方法（可选默认实现）
    default boolean isRoot() {
        return getParentId() == null || getParentId().isEmpty();
    }

    /**
     * @return 任务行为模式，默认 {@link TaskBehavior#SIMPLE}
     */
    TaskBehavior getBehavior();

    /**
     * @return 需要完成的次数（用于 MULTI_COMPLETION 和 MULTI_RANDOM_POOL），默认1
     */
    int getRequiredCompletions();

    /**
     * @return 随机池子任务ID列表（用于 RANDOM_POOL 和 MULTI_RANDOM_POOL）
     */
    List<String> getPoolIds();

    /**
     * @return 触发后继链的概率 (0.0~1.0)，用于 HIDDEN_CHAIN，默认1.0（必定触发）
     */
    double getNextChainTriggerChance();

    /**
     * @return 完成后是否重置为 HIDDEN（用于 RANDOM_ACTIVATE），默认 false
     */
    boolean isAutoResetToHidden();

    // ==============================================
    // 任务完成类型
    // ==============================================

    /**
     * @return 任务完成类型 ID（如 {@code maple_banktrade:submit_item}），默认 null 表示确认完成
     */
    default Identifier getTaskTypeId() {
        return null;
    }

    /**
     * @return 任务完成类型参数（不可为 null，无参数时返回空 CompoundTag）
     */
    default CompoundTag getTaskTypeParams() {
        return new CompoundTag();
    }

    // ==============================================
    // 奖励
    // ==============================================

    /**
     * @return 奖励定义列表，每个元素包含 {@link Identifier}（奖励类型）和 {@link CompoundTag}（参数）
     *         默认返回空列表
     */
    default List<RewardDef> getRewards() {
        return Collections.emptyList();
    }

    // ==============================================
    // 依赖模式
    // ==============================================

    /**
     * @return 依赖满足模式，默认 {@link DependencyRequirement#ALL_COMPLETED}
     */
    default DependencyRequirement getDependencyRequirement() {
        return DependencyRequirement.ALL_COMPLETED;
    }

    // ==============================================
    // 便利方法
    // ==============================================

    default boolean isMultiCompletion() {
        return getBehavior().isMultiCompletion();
    }

    default boolean isRandomPool() {
        return getBehavior().isRandomPool();
    }

    /**
     * 是否为叶子节点（无子节点）。
     */
    default boolean isLeaf() {
        List<String> children = getChildrenIds();
        return children == null || children.isEmpty();
    }

    /**
     * 是否有依赖节点（前置任务）。
     */
    default boolean hasDependencies() {
        List<String> deps = getDependentNodes();
        return deps != null && !deps.isEmpty();
    }

    /**
     * 获取有效的完成阈值。
     * 对于多完成行为，返回 {@link #getRequiredCompletions()}；
     * 对于普通行为，返回 1。
     */
    default int getEffectiveCompletionThreshold() {
        return getBehavior().isMultiCompletion() ? getRequiredCompletions() : 1;
    }
}

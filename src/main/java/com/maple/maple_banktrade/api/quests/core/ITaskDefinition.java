package com.maple.maple_banktrade.api.quests.core;

import com.maple.maple_banktrade.api.quests.condition.BaseQuestCondition;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.reward.IReward;
import com.maple.maple_banktrade.api.quests.tasktype.ITaskType;

import java.util.Collections;
import java.util.List;

/**
 * 不可变的任务定义（数据驱动）。
 *
 * <p>
 * v3.6 重构：条件、奖励、任务完成类型改为直接存储对象引用，
 * 不再通过 {@code Identifier} + {@code CompoundTag} 间接存储。
 */
public interface ITaskDefinition {

    String getId();

    TaskType getType();

    boolean isGroup();

    String getParentId();

    String getPrevSiblingId();

    List<String> getDependentNodes();

    String getNextTaskInChain();

    boolean isRepeatable();

    int getMaxRepeatTimes();

    boolean isForceParentVisible();

    /**
     * @return 可见性条件（可为 null 表示无条件）
     */
    BaseQuestCondition getVisibilityCondition();

    /**
     * @return 解锁条件（可为 null 表示无条件）
     */
    BaseQuestCondition getUnlockCondition();

    /** 预计算的子节点列表（由加载器填充）。 */
    List<String> getChildrenIds();

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
     * @return 触发后继链的概率 (0.0~1.0)，用于 HIDDEN_CHAIN，默认1.0
     */
    double getNextChainTriggerChance();

    /**
     * @return 完成后是否重置为 HIDDEN（用于 RANDOM_ACTIVATE），默认 false
     */
    boolean isAutoResetToHidden();

    // ==============================================
    // 任务完成类型（v3.7：改为列表，需全部满足）
    // ==============================================

    /**
     * @return 任务完成类型列表，全部满足才能完成。默认空列表表示确认完成。
     */
    default List<ITaskType> getTaskTypes() {
        return Collections.emptyList();
    }

    // ==============================================
    // 奖励
    // ==============================================

    /**
     * @return 奖励实例列表，默认返回空列表
     */
    default List<IReward> getRewards() {
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

    default boolean isLeaf() {
        List<String> children = getChildrenIds();
        return children == null || children.isEmpty();
    }

    default boolean hasDependencies() {
        List<String> deps = getDependentNodes();
        return deps != null && !deps.isEmpty();
    }

    default int getEffectiveCompletionThreshold() {
        return getBehavior().isMultiCompletion() ? getRequiredCompletions() : 1;
    }
}

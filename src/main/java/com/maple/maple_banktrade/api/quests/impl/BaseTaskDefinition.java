package com.maple.maple_banktrade.api.quests.impl;

import com.maple.maple_banktrade.api.quests.condition.BaseQuestCondition;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.reward.IReward;
import com.maple.maple_banktrade.api.quests.tasktype.ITaskType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基础任务蓝图（不可变）。
 *
 * <p>
 * v3.6 重构：条件、奖励、任务类型直接存储对象引用。
 * v3.7 重构：{@code taskType} 改为 {@code List<ITaskType>}，全部满足才能完成；
 * 奖励不再自动发放，改为手动领取。
 *
 * <p>
 * 使用 {@link Builder} 构建实例，构建时会自动进行参数校验。
 */
@Getter
public class BaseTaskDefinition implements ITaskDefinition {

    private final String id;
    private final TaskType type;
    private final boolean isGroup;
    private final String parentId;
    private final String prevSiblingId;
    private final List<String> dependentNodes;
    private final String nextTaskInChain;
    private final boolean repeatable;
    private final int maxRepeatTimes;
    private final boolean forceParentVisible;

    // v3.6：直接存储条件对象
    private final BaseQuestCondition visibilityCondition;
    private final BaseQuestCondition unlockCondition;

    private final List<String> childrenIds;
    private final TaskBehavior behavior;
    private final int requiredCompletions;
    private final List<String> poolIds;
    private final double nextChainTriggerChance;
    private final boolean autoResetToHidden;

    // v3.7：直接存储任务类型列表和奖励对象
    private final List<ITaskType> taskTypes;
    private final List<IReward> rewards;

    private final DependencyRequirement dependencyRequirement;

    private BaseTaskDefinition(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.isGroup = builder.isGroup;
        this.parentId = builder.parentId;
        this.prevSiblingId = builder.prevSiblingId;
        this.dependentNodes = Collections.unmodifiableList(builder.dependentNodes);
        this.nextTaskInChain = builder.nextTaskInChain;
        this.repeatable = builder.repeatable;
        this.maxRepeatTimes = builder.maxRepeatTimes;
        this.forceParentVisible = builder.forceParentVisible;
        this.visibilityCondition = builder.visibilityCondition;
        this.unlockCondition = builder.unlockCondition;
        this.childrenIds = Collections.unmodifiableList(builder.childrenIds);
        this.behavior = builder.behavior;
        this.requiredCompletions = builder.requiredCompletions;
        this.poolIds = Collections.unmodifiableList(builder.poolIds);
        this.nextChainTriggerChance = builder.nextChainTriggerChance;
        this.autoResetToHidden = builder.autoResetToHidden;
        this.taskTypes = Collections.unmodifiableList(builder.taskTypes);
        this.rewards = Collections.unmodifiableList(builder.rewards);
        this.dependencyRequirement = builder.dependencyRequirement;
    }

    @Override
    public String toString() {
        return "BaseTaskDefinition{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", behavior=" + behavior +
                ", isGroup=" + isGroup +
                ", parentId='" + parentId + '\'' +
                ", visibilityCondition=" + visibilityCondition +
                ", unlockCondition=" + unlockCondition +
                '}';
    }

    // ==============================================
    // Builder
    // ==============================================

    @Accessors(chain = true, fluent = true)
    @Setter(AccessLevel.PUBLIC)
    @Getter(AccessLevel.PACKAGE)
    public static class Builder {

        private String id;
        private TaskType type = TaskType.MAIN;
        private boolean isGroup = false;
        private String parentId = null;
        private String prevSiblingId = null;
        private List<String> dependentNodes = new ArrayList<>();
        private String nextTaskInChain = null;
        private boolean repeatable = false;
        private int maxRepeatTimes = -1;
        private boolean forceParentVisible = false;

        // v3.6：直接存储条件对象
        private BaseQuestCondition visibilityCondition = null;
        private BaseQuestCondition unlockCondition = null;

        private List<String> childrenIds = new ArrayList<>();
        private TaskBehavior behavior = TaskBehavior.SIMPLE;
        private int requiredCompletions = 1;
        private List<String> poolIds = new ArrayList<>();
        private double nextChainTriggerChance = 1.0;
        private boolean autoResetToHidden = false;

        // v3.7：直接存储任务类型列表和奖励对象
        private List<ITaskType> taskTypes = new ArrayList<>();
        private List<IReward> rewards = new ArrayList<>();

        private DependencyRequirement dependencyRequirement = DependencyRequirement.ALL_COMPLETED;

        /**
         * 构建并校验任务定义。
         *
         * @throws IllegalStateException 如果参数校验失败
         */
        public BaseTaskDefinition build() {
            if (dependentNodes == null) dependentNodes = new ArrayList<>();
            if (childrenIds == null) childrenIds = new ArrayList<>();
            if (poolIds == null) poolIds = new ArrayList<>();
            if (taskTypes == null) taskTypes = new ArrayList<>();
            if (rewards == null) rewards = new ArrayList<>();

            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Task id must not be null");
            }

            if (behavior.requiresPool() && poolIds.isEmpty()) {
                throw new IllegalStateException("Task '" + id + "': " + behavior + " behavior requires poolIds");
            }

            if (behavior.isMultiCompletion() && requiredCompletions < 1) {
                throw new IllegalStateException("Task '" + id + "': requiredCompletions must be at least 1 for " + behavior);
            }

            if (behavior == TaskBehavior.HIDDEN_CHAIN) {
                if (nextChainTriggerChance < 0.0 || nextChainTriggerChance > 1.0) {
                    throw new IllegalStateException("Task '" + id + "': nextChainTriggerChance must be between 0.0 and 1.0");
                }
            }

            if (behavior == TaskBehavior.RANDOM_ACTIVATE && !autoResetToHidden) {
                autoResetToHidden = true;
            }

            return new BaseTaskDefinition(this);
        }
    }
}

package com.maple.maple_banktrade.api.quests.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.reward.RewardDef;
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
    private final Identifier visibilityConditionId;
    private final CompoundTag visibilityConditionParams;
    private final Identifier unlockConditionId;
    private final CompoundTag unlockConditionParams;
    private final List<String> childrenIds;
    private final TaskBehavior behavior;
    private final int requiredCompletions;
    private final List<String> poolIds;
    private final double nextChainTriggerChance;
    private final boolean autoResetToHidden;
    private final Identifier taskTypeId;
    private final CompoundTag taskTypeParams;
    private final List<RewardDef> rewards;
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
        this.visibilityConditionId = builder.visibilityConditionId;
        this.visibilityConditionParams = builder.visibilityConditionParams != null ? builder.visibilityConditionParams : new CompoundTag();
        this.unlockConditionId = builder.unlockConditionId;
        this.unlockConditionParams = builder.unlockConditionParams != null ? builder.unlockConditionParams : new CompoundTag();
        this.childrenIds = Collections.unmodifiableList(builder.childrenIds);
        this.behavior = builder.behavior;
        this.requiredCompletions = builder.requiredCompletions;
        this.poolIds = Collections.unmodifiableList(builder.poolIds);
        this.nextChainTriggerChance = builder.nextChainTriggerChance;
        this.autoResetToHidden = builder.autoResetToHidden;
        this.taskTypeId = builder.taskTypeId;
        this.taskTypeParams = builder.taskTypeParams != null ? builder.taskTypeParams : new CompoundTag();
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
                ", visibilityCondition=" + visibilityConditionId +
                ", unlockCondition=" + unlockConditionId +
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
        private Identifier visibilityConditionId = null;
        private CompoundTag visibilityConditionParams = new CompoundTag();
        private Identifier unlockConditionId = null;
        private CompoundTag unlockConditionParams = new CompoundTag();
        private List<String> childrenIds = new ArrayList<>();
        private TaskBehavior behavior = TaskBehavior.SIMPLE;
        private int requiredCompletions = 1;
        private List<String> poolIds = new ArrayList<>();
        private double nextChainTriggerChance = 1.0;
        private boolean autoResetToHidden = false;
        private Identifier taskTypeId = null;
        private CompoundTag taskTypeParams = new CompoundTag();
        private List<RewardDef> rewards = new ArrayList<>();
        private DependencyRequirement dependencyRequirement = DependencyRequirement.ALL_COMPLETED;

        /**
         * 构建并校验任务定义。
         *
         * @throws IllegalStateException 如果参数校验失败
         */
        public BaseTaskDefinition build() {
            // 空安全：null 集合替换为空列表
            if (dependentNodes == null) dependentNodes = new ArrayList<>();
            if (childrenIds == null) childrenIds = new ArrayList<>();
            if (poolIds == null) poolIds = new ArrayList<>();

            // 必填校验
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Task id must not be null");
            }

            // 随机池行为必须配置 poolIds
            if (behavior.requiresPool() && poolIds.isEmpty()) {
                throw new IllegalStateException("Task '" + id + "': " + behavior + " behavior requires poolIds");
            }

            // 多次完成行为必须设置 requiredCompletions >= 1
            if (behavior.isMultiCompletion() && requiredCompletions < 1) {
                throw new IllegalStateException("Task '" + id + "': requiredCompletions must be at least 1 for " + behavior);
            }

            // 隐藏链行为验证概率范围
            if (behavior == TaskBehavior.HIDDEN_CHAIN) {
                if (nextChainTriggerChance < 0.0 || nextChainTriggerChance > 1.0) {
                    throw new IllegalStateException("Task '" + id + "': nextChainTriggerChance must be between 0.0 and 1.0");
                }
            }

            // RANDOM_ACTIVATE 强制 autoResetToHidden = true
            if (behavior == TaskBehavior.RANDOM_ACTIVATE && !autoResetToHidden) {
                autoResetToHidden = true;
            }

            return new BaseTaskDefinition(this);
        }
    }
}

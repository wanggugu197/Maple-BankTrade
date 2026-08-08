package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 脚本条件求值接口。
 *
 * <p>
 * 用于解析 {@code visibilityCondition} 和 {@code unlockCondition} 字段。
 * 具体实现通过 {@link QuestConditionRegistry} 查找条件并评估。
 */
@FunctionalInterface
public interface IScriptEvaluator {

    /**
     * 评估条件。
     *
     * @param conditionId 条件标识符（可为 null 表示无条件）
     * @param params      条件参数（不可为 null，无参数时为空 CompoundTag）
     * @param taskId      当前任务 ID（用于日志上下文）
     * @return true 表示条件满足
     */
    boolean evaluate(Identifier conditionId, CompoundTag params, String taskId);

    /**
     * 返回一个总是返回 true 的空实现（跳过所有条件检查）。
     */
    static IScriptEvaluator noOp() {
        return (conditionId, params, taskId) -> true;
    }
}

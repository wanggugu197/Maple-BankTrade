package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * 基于注册表的脚本评估器 —— 实现 {@link IScriptEvaluator}。
 *
 * <p>
 * 直接通过 {@link Identifier} + {@link CompoundTag} 从 {@link QuestConditionRegistry} 获取条件，
 * 不再需要解析字符串脚本。使用 {@link EvaluationContext} 提供类型化上下文。
 *
 * <p>
 * 使用示例：
 * 
 * <pre>{@code
 * 
 * RegistryScriptEvaluator evaluator = RegistryScriptEvaluator.withFixedContext(EvaluationContext.of(player));
 * boolean result = evaluator.evaluate(id("level_ge"), tagWithLevel, "main_forest");
 * }</pre>
 */
public class RegistryScriptEvaluator implements IScriptEvaluator {

    private final Supplier<EvaluationContext> contextSupplier;

    public RegistryScriptEvaluator(Supplier<EvaluationContext> contextSupplier) {
        this.contextSupplier = contextSupplier != null ? contextSupplier : () -> null;
    }

    /**
     * 创建固定上下文的评估器。
     */
    public static RegistryScriptEvaluator withFixedContext(EvaluationContext context) {
        return new RegistryScriptEvaluator(() -> context);
    }

    @Override
    public boolean evaluate(Identifier conditionId, CompoundTag params, String taskId) {
        if (conditionId == null) {
            return true; // 无条件：直接通过
        }
        if (params == null) {
            params = new CompoundTag();
        }
        BaseQuestCondition condition = QuestConditionRegistry.getCondition(conditionId, params);
        EvaluationContext ctx = contextSupplier.get();
        // 优先使用类型化评估，回退到 Object 评估
        if (ctx != null) {
            return condition.evaluate(ctx);
        }
        return condition.evaluate((Object) null);
    }

    @Override
    public String toString() {
        return "RegistryScriptEvaluator{registered=" + QuestConditionRegistry.size() + " conditions}";
    }
}

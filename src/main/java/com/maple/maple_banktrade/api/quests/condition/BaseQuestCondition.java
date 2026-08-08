package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;

/**
 * 任务条件基类——所有具体条件逻辑的抽象。
 *
 * <p>
 * 仿照 {@code MachineTradeHooks} 模式设计：
 * <ul>
 * <li>具体条件子类存储实际 Minecraft 实例（Item、MobEffect 等），不存储字符串 ID</li>
 * <li>{@link ConditionFactory} 接收 {@link CompoundTag} 参数创建条件实例</li>
 * <li>通过 {@link com.maple.maple_banktrade.api.quests.condition.QuestConditionRegistry} 注册</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * // 注册工厂
 * QuestConditionRegistry.register(id("my_cond"), tag -> new LevelCondition(tag.getInt("level")));
 *
 * // 评估
 * boolean result = condition.evaluate(player);
 * }</pre>
 */
public abstract class BaseQuestCondition {

    /** 默认条件：无条件通过。 */
    public static final BaseQuestCondition ALWAYS_PASS = new BaseQuestCondition() {

        @Override
        public boolean evaluate(Object context) {
            return true;
        }

        @Override
        public String toString() {
            return "ALWAYS_PASS";
        }
    };

    /**
     * 评估条件是否满足。
     *
     * @param context 上下文对象（通常为 {@code net.minecraft.world.entity.player.Player} 或 {@code ServerLevel}）
     * @return true 表示条件满足
     */
    public abstract boolean evaluate(Object context);

    /**
     * 使用类型化上下文评估条件。
     * 默认回退到 {@link #evaluate(Object)}，将 {@link EvaluationContext#getPlayer()} 作为 context 传入。
     * 子类可覆盖此方法以利用类型化上下文。
     *
     * @param ctx 评估上下文
     * @return true 表示条件满足
     */
    public boolean evaluate(EvaluationContext ctx) {
        return evaluate((Object) ctx.getPlayer());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * 安全类型转换辅助方法。
     */
    @SuppressWarnings("unchecked")
    protected static <T> T safeCast(Object context, Class<T> type) {
        if (type.isInstance(context)) {
            return (T) context;
        }
        return null;
    }

    // ==============================================
    // 工厂接口（供注册表使用）
    // ==============================================

    /**
     * 条件工厂：从 {@link CompoundTag} 配置创建条件实例。
     * 仿照 {@code MachineTradeHooks.VisibilityCheckFactory} 模式。
     * <p>
     * 工厂在创建时从 Tag 中解析参数并查找实际 Minecraft 实例（Item、MobEffect 等），
     * 条件实例存储的是实际实例，不是字符串 ID。
     */
    @FunctionalInterface
    public interface ConditionFactory {

        /**
         * 从配置创建条件实例。
         *
         * @param config 参数配置（可为空 CompoundTag）
         * @return 条件实例
         */
        BaseQuestCondition create(CompoundTag config);
    }
}

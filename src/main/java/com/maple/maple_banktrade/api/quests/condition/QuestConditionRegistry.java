package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 任务条件注册表 —— 仿照 {@code MachineTradeHookRegistry} 模式。
 *
 * <p>
 * 核心设计：
 * <ul>
 * <li>通过 {@link Identifier} 索引条件工厂（{@link BaseQuestCondition.ConditionFactory}）</li>
 * <li>工厂接收 {@link CompoundTag} 参数，创建存储实际 Minecraft 实例的条件</li>
 * <li>默认条件：无条件通过（{@link #DEFAULT_CONDITION}）</li>
 * <li>未注册 ID 自动回退到默认条件并记录警告</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * // 注册工厂
 * QuestConditionRegistry.register(id("level_ge"), LevelCondition::fromTag);
 *
 * // 获取条件实例
 * CompoundTag config = new CompoundTag();
 * config.putInt("level", 1);
 * BaseQuestCondition cond = QuestConditionRegistry.getCondition(id("level_ge"), config);
 * boolean result = cond.evaluate(player);
 * }</pre>
 */
public final class QuestConditionRegistry {

    /** 初始化占位方法，触发静态块注册。 */
    public static void init() {}

    private static final Map<Identifier, BaseQuestCondition.ConditionFactory> FACTORIES = new LinkedHashMap<>();

    /** 默认条件 ID：无条件通过。 */
    public static final Identifier DEFAULT_CONDITION = MapleBankTrade.id("always_pass");

    static {
        // 注册默认条件
        register(DEFAULT_CONDITION, _ -> BaseQuestCondition.ALWAYS_PASS);
        // 注册所有预定义条件
        registerAllDefaults();
    }

    // ==============================================
    // 注册方法
    // ==============================================

    /**
     * 注册一个条件工厂。
     *
     * @param id      条件标识符
     * @param factory 条件工厂（接收 CompoundTag，返回条件实例）
     */
    public static void register(Identifier id, BaseQuestCondition.ConditionFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (FACTORIES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting quest condition factory for id: {}", id);
        }
        FACTORIES.put(id, factory);
    }

    // ==============================================
    // 获取条件实例
    // ==============================================

    /**
     * 根据 ID 和配置创建条件实例。
     *
     * @param id     条件标识符（可为 null，自动回退到默认）
     * @param config 参数配置（可为 null，自动使用空 CompoundTag）
     * @return 条件实例，不会返回 null
     */
    public static BaseQuestCondition getCondition(Identifier id, CompoundTag config) {
        if (id == null) id = DEFAULT_CONDITION;
        if (config == null) config = new CompoundTag();

        BaseQuestCondition.ConditionFactory factory = FACTORIES.get(id);
        if (factory == null) {
            MapleBankTrade.LOGGER.warn("Unknown quest condition id: {}, using default", id);
            factory = FACTORIES.get(DEFAULT_CONDITION);
        }
        return factory.create(config);
    }

    /**
     * 根据 ID 创建条件实例（无参数）。
     */
    public static BaseQuestCondition getCondition(Identifier id) {
        return getCondition(id, new CompoundTag());
    }

    // ==============================================
    // 查询
    // ==============================================

    /**
     * 检查指定条件 ID 是否已注册。
     */
    public static boolean isRegistered(Identifier id) {
        return id != null && FACTORIES.containsKey(id);
    }

    /**
     * 获取已注册的条件数量。
     */
    public static int size() {
        return FACTORIES.size();
    }

    /**
     * 清空所有注册条件（用于测试重置）。
     */
    public static void clear() {
        FACTORIES.clear();
        // 重新注册默认条件
        register(DEFAULT_CONDITION, _ -> BaseQuestCondition.ALWAYS_PASS);
    }

    // ==============================================
    // 预注册（初始化时调用）
    // ==============================================

    /**
     * 注册所有蓝图预定义的条件工厂。
     */
    public static void registerAllDefaults() {
        register(MapleBankTrade.id("level_ge"), LevelCondition::fromTag);
        register(MapleBankTrade.id("has_item"), HasItemCondition::fromTag);
        register(MapleBankTrade.id("has_effect"), HasPotionEffectCondition::fromTag);
        register(MapleBankTrade.id("is_raining"), IsRainingCondition::fromTag);
        register(MapleBankTrade.id("low_health"), HealthCondition::fromTag);
        // 组合条件
        register(MapleBankTrade.id("and"), CompositeCondition.AndCondition::fromTag);
        register(MapleBankTrade.id("or"), CompositeCondition.OrCondition::fromTag);
        register(MapleBankTrade.id("not"), CompositeCondition.NotCondition::fromTag);
    }

    private QuestConditionRegistry() {}
}

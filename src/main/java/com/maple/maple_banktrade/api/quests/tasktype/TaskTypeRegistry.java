package com.maple.maple_banktrade.api.quests.tasktype;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 任务完成类型注册表 —— 仿照 {@link com.maple.maple_banktrade.api.quests.condition.QuestConditionRegistry} 模式。
 *
 * <p>
 * 通过 {@link Identifier} 索引任务类型，提供统一查询入口。
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * // 注册
 * TaskTypeRegistry.register(id("confirm"), new ConfirmTaskType());
 *
 * // 查询
 * ITaskType type = TaskTypeRegistry.get(id("confirm"));
 * }</pre>
 */
public final class TaskTypeRegistry {

    /** 初始化占位方法，触发静态块注册。 */
    public static void init() {}

    private static final Map<Identifier, ITaskType> TYPES = new LinkedHashMap<>();

    static {
        registerAllDefaults();
    }

    // ==============================================
    // 注册方法
    // ==============================================

    /**
     * 注册一个任务类型。
     *
     * @param id   类型标识符
     * @param type 任务类型实例
     */
    public static void register(Identifier id, ITaskType type) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        if (TYPES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting task type registration for id: {}", id);
        }
        TYPES.put(id, type);
    }

    // ==============================================
    // 查询
    // ==============================================

    /**
     * 根据 ID 获取任务类型。
     *
     * @param id 类型标识符
     * @return 任务类型实例，未注册返回 null
     */
    public static ITaskType get(Identifier id) {
        return id != null ? TYPES.get(id) : null;
    }

    /**
     * 检查指定任务类型 ID 是否已注册。
     */
    public static boolean isRegistered(Identifier id) {
        return id != null && TYPES.containsKey(id);
    }

    /**
     * 获取已注册的任务类型数量。
     */
    public static int size() {
        return TYPES.size();
    }

    // ==============================================
    // 预注册
    // ==============================================

    private static void registerAllDefaults() {
        register(MapleBankTrade.id("confirm"), new ConfirmTaskType());
        register(MapleBankTrade.id("submit_item"), new SubmitItemTaskType());
    }

    private TaskTypeRegistry() {}
}

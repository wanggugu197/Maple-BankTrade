package com.maple.maple_banktrade.api.quests;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.TaskType;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * 全局静态任务定义注册表 —— 所有玩家共享的只读任务图。
 *
 * <p>
 * 职责：
 * <ul>
 * <li>持有所有任务蓝图（{@link ITaskDefinition}），服务器启动时初始化一次</li>
 * <li>提供按 ID、类型、父子关系的查询方法</li>
 * <li>与 {@link com.maple.maple_banktrade.api.quests.repository.PlayerQuestData} 分离：
 * 定义是静态的（注册表），状态是动态的（PlayerQuestData）</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * // 服务器启动时初始化一次
 * QuestDefinitionRegistry.init(QuestBlueprints::getAllBlueprints);
 *
 * // 任意位置查询
 * ITaskDefinition def = QuestDefinitionRegistry.getDefinition("main_forest");
 * List<ITaskDefinition> roots = QuestDefinitionRegistry.getRoots();
 * }</pre>
 */
public final class QuestDefinitionRegistry {

    private static final Map<String, ITaskDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static volatile boolean initialized = false;

    private QuestDefinitionRegistry() {
        // 工具类，禁止实例化
    }

    // ==============================================
    // 初始化
    // ==============================================

    /**
     * 初始化任务定义注册表（服务器启动时调用一次）。
     * 重复调用会覆盖已有定义。
     *
     * @param supplier 蓝图供应器（如 {@code QuestBlueprints::getAllBlueprints}）
     */
    public static void init(Supplier<Collection<ITaskDefinition>> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        DEFINITIONS.clear();
        Collection<ITaskDefinition> defs = supplier.get();
        for (ITaskDefinition def : defs) {
            DEFINITIONS.put(def.getId(), def);
        }
        initialized = true;
        MapleBankTrade.LOGGER.info("[QuestDefinitionRegistry] Initialized with {} task definitions", DEFINITIONS.size());
    }

    /**
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    // ==============================================
    // 查询
    // ==============================================

    /**
     * 根据 ID 获取任务定义。
     */
    @Nullable
    public static ITaskDefinition getDefinition(String id) {
        return DEFINITIONS.get(id);
    }

    /**
     * 获取所有根节点（无父节点的任务）。
     */
    public static List<ITaskDefinition> getRoots() {
        return DEFINITIONS.values().stream()
                .filter(ITaskDefinition::isRoot)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定父节点的所有子任务。
     */
    public static List<ITaskDefinition> getChildren(String parentId) {
        return DEFINITIONS.values().stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有任务定义。
     */
    public static List<ITaskDefinition> getAllDefinitions() {
        return new ArrayList<>(DEFINITIONS.values());
    }

    /**
     * 按类型过滤任务定义。
     */
    public static List<ITaskDefinition> getDefinitionsByType(TaskType type) {
        return DEFINITIONS.values().stream()
                .filter(d -> d.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 检查指定 ID 是否存在。
     */
    public static boolean hasDefinition(String id) {
        return DEFINITIONS.containsKey(id);
    }

    /**
     * 获取已注册的任务定义数量。
     */
    public static int size() {
        return DEFINITIONS.size();
    }

    /**
     * 获取所有任务的 ID 集合。
     */
    public static Set<String> getAllIds() {
        return Collections.unmodifiableSet(DEFINITIONS.keySet());
    }
}

package com.maple.maple_banktrade.api.quests.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.condition.EvaluationContext;
import com.maple.maple_banktrade.api.quests.condition.RegistryScriptEvaluator;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家任务数据管理器 —— 委托 {@link QuestDataHelper} 进行持久化存储。
 *
 * <p>
 * 职责：
 * <ul>
 * <li>按玩家 UUID 管理任务数据访问（底层委托 {@link QuestSavedData} 持久化）</li>
 * <li>玩家登录时从存储层获取数据</li>
 * <li>玩家登出时清理评估器缓存</li>
 * <li>为触发层提供统一的数据访问入口</li>
 * </ul>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * 
 * PlayerQuestData data = QuestDataManager.getOrCreate(player);
 * RegistryScriptEvaluator evaluator = QuestDataManager.getEvaluator(player);
 * }</pre>
 */
public final class QuestDataManager {

    /** 评估器缓存（与玩家实例绑定，登出时清理）。 */
    private static final Map<UUID, RegistryScriptEvaluator> EVALUATORS = new ConcurrentHashMap<>();

    private QuestDataManager() {
        // 工具类，禁止实例化
    }

    /**
     * 初始化：在模组启动时调用一次。
     */
    public static void init() {
        MapleBankTrade.LOGGER.info("[QuestDataManager] Initialized (storage delegated to QuestSavedData)");
    }

    /**
     * 获取或创建玩家的任务数据。
     * 底层委托 {@link QuestDataHelper#getOrCreate(ServerPlayer)}，从服务器 SavedData 存储获取。
     */
    public static PlayerQuestData getOrCreate(ServerPlayer player) {
        return QuestDataHelper.getOrCreate(player);
    }

    /**
     * 获取或创建玩家的条件评估器（与玩家数据绑定）。
     */
    public static RegistryScriptEvaluator getEvaluator(ServerPlayer player) {
        // 确保任务数据已加载
        getOrCreate(player);
        return EVALUATORS.computeIfAbsent(player.getUUID(), uuid -> RegistryScriptEvaluator.withFixedContext(EvaluationContext.of(player)));
    }

    /**
     * 获取玩家的任务数据（可能为 null）。
     */
    public static PlayerQuestData get(MinecraftServer server, UUID uuid) {
        return QuestDataHelper.get(server, uuid);
    }

    /**
     * 玩家登出时清理评估器缓存。
     * 任务数据本身保留在服务端存储中，不随登出删除。
     */
    public static void remove(UUID uuid) {
        EVALUATORS.remove(uuid);
        MapleBankTrade.LOGGER.debug("[QuestDataManager] Removed evaluator cache for UUID {}", uuid);
    }

    /**
     * 获取当前缓存的评估器数量。
     */
    public static int getEvaluatorCount() {
        return EVALUATORS.size();
    }
}

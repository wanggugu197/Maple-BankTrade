package com.maple.maple_banktrade.api.quests.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.condition.EvaluationContext;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家任务数据管理器 —— 委托 {@link QuestDataHelper} 进行持久化存储。
 *
 * <p>
 * v3.6 重构：{@code getEvaluator()} 改为直接返回 {@link EvaluationContext}，
 * 条件现直接存储在任务定义中，不再需要注册表查找。
 */
public final class QuestDataManager {

    private static final Map<UUID, EvaluationContext> EVALUATORS = new ConcurrentHashMap<>();

    private QuestDataManager() {}

    public static void init() {
        MapleBankTrade.LOGGER.info("[QuestDataManager] Initialized (storage delegated to QuestSavedData)");
    }

    public static PlayerQuestData getOrCreate(ServerPlayer player) {
        return QuestDataHelper.getOrCreate(player);
    }

    /**
     * 获取或创建玩家的条件评估上下文。
     */
    public static EvaluationContext getEvaluator(ServerPlayer player) {
        getOrCreate(player);
        return EVALUATORS.computeIfAbsent(player.getUUID(), uuid -> EvaluationContext.of(player));
    }

    public static PlayerQuestData get(MinecraftServer server, UUID uuid) {
        return QuestDataHelper.get(server, uuid);
    }

    public static void remove(UUID uuid) {
        EVALUATORS.remove(uuid);
        MapleBankTrade.LOGGER.debug("[QuestDataManager] Removed evaluator cache for UUID {}", uuid);
    }

    public static int getEvaluatorCount() {
        return EVALUATORS.size();
    }
}

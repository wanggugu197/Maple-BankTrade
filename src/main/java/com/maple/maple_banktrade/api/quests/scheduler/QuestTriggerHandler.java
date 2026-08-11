package com.maple.maple_banktrade.api.quests.scheduler;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.calculator.VisibilityCalculator;
import com.maple.maple_banktrade.api.quests.condition.EvaluationContext;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;
import com.maple.maple_banktrade.api.quests.storage.QuestDataManager;

import java.util.List;
import java.util.Random;

/**
 * 任务触发层 —— 负责周期性刷新任务状态，不负责完成任务。
 *
 * <p>
 * 设计原则：
 * <ul>
 * <li><b>仅触发刷新，不处理完成</b>：任务完成由 UI 点击触发 {@code StateTransitionOrchestrator.processComplete()}</li>
 * <li><b>定时刷新</b>：每 200 ticks（10 秒）检查所有在线玩家</li>
 * <li><b>刷新内容</b>：冷却/每日重置 → 随机临时任务激活 → 全量可见性重算</li>
 * <li><b>玩家登录/登出</b>：初始化/清理数据</li>
 * </ul>
 *
 * <p>
 * 注册方式：在 {@code CommonInit} 中调用 {@link #init()}。
 */
public final class QuestTriggerHandler {

    /** 刷新间隔（tick）：200 ticks = 10 秒。 */
    private static final int REFRESH_INTERVAL = 200;

    /** 最大同时激活的临时任务数。 */
    private static final int MAX_ACTIVE_TEMP_TASKS = 3;

    private static int tickCounter = 0;
    private static final Random RANDOM = new Random();

    private QuestTriggerHandler() {
        // 工具类，禁止实例化
    }

    /**
     * 注册所有任务触发事件监听器。
     * 在 {@code CommonInit} 中调用此方法。
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(QuestTriggerHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(QuestTriggerHandler::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(QuestTriggerHandler::onPlayerLogout);
        MapleBankTrade.LOGGER.info("[QuestTriggerHandler] Registered task refresh handlers");
    }

    // ==============================================
    // 事件处理
    // ==============================================

    /**
     * 服务器 tick 事件：每 REFRESH_INTERVAL tick 刷新所有在线玩家的任务状态。
     */
    private static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % REFRESH_INTERVAL != 0) return;

        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;

        long gameTime = server.overworld().getGameTime();

        for (ServerPlayer player : players) {
            refreshPlayerTasks(player, gameTime);
        }
    }

    /**
     * 玩家登录：初始化任务数据。
     */
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestDataManager.getOrCreate(player);
            MapleBankTrade.LOGGER.debug("[QuestTriggerHandler] Player {} logged in, quest data initialized",
                    player.getName().getString());
        }
    }

    /**
     * 玩家登出：清理任务数据。
     */
    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            QuestDataManager.remove(player.getUUID());
        }
    }

    // ==============================================
    // 刷新逻辑
    // ==============================================

    /**
     * 刷新单个玩家的全部任务状态。
     *
     * <p>
     * 刷新顺序：
     * <ol>
     * <li>冷却/每日重置：将符合条件的已完成任务重置为 VISIBLE_LOCKED</li>
     * <li>随机临时任务激活：尝试随机激活一个隐藏的临时任务</li>
     * <li>全量可见性重算：对所有任务重新计算目标状态</li>
     * </ol>
     */
    private static void refreshPlayerTasks(ServerPlayer player, long gameTime) {
        try {
            PlayerQuestData data = QuestDataManager.getOrCreate(player);
            EvaluationContext evalCtx = QuestDataManager.getEvaluator(player);

            // 1. 冷却/每日重置
            List<String> resetIds = CooldownResetService.processCooldownResets(data, gameTime, evalCtx);
            if (!resetIds.isEmpty()) {
                MapleBankTrade.LOGGER.debug("[QuestTriggerHandler] Player {} reset {} tasks: {}",
                        player.getName().getString(), resetIds.size(), resetIds);
            }

            // 2. 随机临时任务激活
            String triggered = CooldownResetService.tryRandomTempTask(
                    data, evalCtx, MAX_ACTIVE_TEMP_TASKS, RANDOM);
            if (triggered != null) {
                MapleBankTrade.LOGGER.debug("[QuestTriggerHandler] Player {} triggered temp task: {}",
                        player.getName().getString(), triggered);
            }

            // 3. 全量可见性重算
            recalculateAllVisibility(data, evalCtx);

        } catch (Exception e) {
            MapleBankTrade.LOGGER.error("[QuestTriggerHandler] Error refreshing tasks for player {}",
                    player.getName().getString(), e);
        }
    }

    /**
     * 对所有任务重新计算可见性。
     *
     * <p>
     * 遍历所有任务定义，将当前状态与目标状态比较，不一致时更新。
     */
    private static void recalculateAllVisibility(PlayerQuestData data, EvaluationContext evalCtx) {
        List<ITaskDefinition> allDefs = data.getAllDefinitions();
        if (allDefs.isEmpty()) return;

        ResolutionContext context = new ResolutionContext(data, evalCtx, data.getAllStates());

        for (ITaskDefinition def : allDefs) {
            try {
                String taskId = def.getId();
                ITaskState state = data.getOrCreateState(taskId);
                TaskStatus currentStatus = state.getStatus();

                TaskStatus targetStatus = VisibilityCalculator.resolveStatus(taskId, context);

                if (currentStatus != targetStatus) {
                    // COMPLETED 状态仅在完成时由 StateTransitionOrchestrator 设置，不在此处覆盖
                    if (currentStatus == TaskStatus.COMPLETED) {
                        continue;
                    }
                    state.setStatus(targetStatus);
                    data.saveState(state);
                }
            } catch (Exception e) {
                MapleBankTrade.LOGGER.warn("[QuestTriggerHandler] Error recalculating visibility for task {}",
                        def.getId(), e);
            }
        }
    }
}

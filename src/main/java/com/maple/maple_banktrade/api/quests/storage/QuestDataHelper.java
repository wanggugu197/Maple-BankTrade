package com.maple.maple_banktrade.api.quests.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * 任务数据统一 UUID 入口 —— 类似 {@code BankHelper} 的集中访问工具。
 *
 * <p>
 * 职责：
 * <ul>
 * <li>从服务器 {@link net.minecraft.world.level.storage.DimensionDataStorage} 获取或创建 {@link QuestSavedData}</li>
 * <li>按 UUID 访问玩家任务数据，不绑定 {@link Player} 实例</li>
 * <li>封装所有存储层操作，对外提供简洁 API</li>
 * </ul>
 *
 * <p>
 * 任务定义由 {@link com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry} 全局静态持有，
 * 无需在本类中处理。
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * 
 * // 获取某 UUID 的任务数据
 * PlayerQuestData data = QuestDataHelper.getOrCreate(server, playerUUID);
 *
 * // 从 ServerPlayer 获取
 * PlayerQuestData data = QuestDataHelper.getOrCreate(player);
 * }</pre>
 */
public final class QuestDataHelper {

    private QuestDataHelper() {
        // 工具类，禁止实例化
    }

    // ==============================================
    // 存储层访问
    // ==============================================

    /**
     * 从服务器获取或创建任务存储。
     * 参考 {@code MBTBankStates.getBankCards(MinecraftServer)} 模式。
     *
     * @param server 服务器实例
     * @return 任务存储，不会返回 null
     */
    public static QuestSavedData getOrCreateStorage(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.getDataStorage().computeIfAbsent(QuestSavedData.TYPE);
    }

    /**
     * 从 ServerLevel 获取或创建任务存储。
     */
    public static QuestSavedData getOrCreateStorage(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        return getOrCreateStorage(level.getServer());
    }

    /**
     * 修改任务存储，回调结束后标记需保存。
     */
    public static void modifyStorage(MinecraftServer server, Consumer<QuestSavedData> action) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(action, "action");
        QuestSavedData storage = getOrCreateStorage(server);
        try {
            action.accept(storage);
        } finally {
            storage.setDirty();
        }
    }

    /**
     * 修改任务存储，回调结束后标记需保存。
     */
    public static void modifyStorage(ServerLevel level, Consumer<QuestSavedData> action) {
        Objects.requireNonNull(level, "level");
        modifyStorage(level.getServer(), action);
    }

    // ==============================================
    // UUID 入口（不绑定 Player）
    // ==============================================

    /**
     * 获取指定 UUID 的任务数据（可能为 null）。
     *
     * @param server 服务器实例
     * @param uuid   玩家 UUID
     * @return 任务数据，不存在返回 null
     */
    @Nullable
    public static PlayerQuestData get(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        return getOrCreateStorage(server).get(uuid);
    }

    /**
     * 获取或创建指定 UUID 的任务数据。
     *
     * @param server 服务器实例
     * @param uuid   玩家 UUID
     * @return 任务数据，不会返回 null
     */
    public static PlayerQuestData getOrCreate(MinecraftServer server, UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return getOrCreateStorage(server).getOrCreate(uuid);
    }

    /**
     * 获取或创建指定 ServerPlayer 的任务数据。
     * 内部使用 {@link Player#getUUID()} 获取 UUID。
     *
     * @param player 服务器玩家
     * @return 任务数据，不会返回 null
     */
    public static PlayerQuestData getOrCreate(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return getOrCreate(player.level().getServer(), player.getUUID());
    }

    /**
     * 检查指定 UUID 是否存在任务数据。
     */
    public static boolean contains(MinecraftServer server, UUID uuid) {
        if (uuid == null) return false;
        return getOrCreateStorage(server).contains(uuid);
    }

    /**
     * 移除指定 UUID 的任务数据（玩家登出时清理）。
     *
     * @param server 服务器实例
     * @param uuid   玩家 UUID
     * @return 被移除的数据，如果不存在返回 null
     */
    @Nullable
    public static PlayerQuestData remove(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        return getOrCreateStorage(server).remove(uuid);
    }

    // ==============================================
    // 便利方法
    // ==============================================

    /**
     * 获取当前存储的 UUID 总数。
     */
    public static int getUuidCount(MinecraftServer server) {
        return getOrCreateStorage(server).size();
    }
}

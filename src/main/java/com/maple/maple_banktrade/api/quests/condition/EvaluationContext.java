package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * 类型化评估上下文，替代裸 {@link Object} 参数。
 *
 * <p>
 * 封装条件评估所需的运行时环境：
 * <ul>
 * <li>{@code player} —— 可为 null（无玩家上下文时，如服务器级条件）</li>
 * <li>{@code level} —— 不可为 null</li>
 * <li>{@code server} —— 不可为 null</li>
 * </ul>
 *
 * <p>
 * 使用工厂方法创建：
 * 
 * <pre>{@code
 * 
 * EvaluationContext ctx = EvaluationContext.of(player);
 * EvaluationContext ctx = EvaluationContext.of(level);
 * }</pre>
 */
public class EvaluationContext {

    @Nullable
    private final ServerPlayer player;
    private final ServerLevel level;
    private final MinecraftServer server;

    private EvaluationContext(@Nullable ServerPlayer player, ServerLevel level, MinecraftServer server) {
        this.player = player;
        this.level = level;
        this.server = server;
    }

    /**
     * 从玩家创建评估上下文。
     */
    public static EvaluationContext of(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return new EvaluationContext(player, (ServerLevel) player.level(), player.level().getServer());
    }

    /**
     * 从 ServerLevel 创建评估上下文（无玩家）。
     */
    public static EvaluationContext of(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        return new EvaluationContext(null, level, level.getServer());
    }

    /**
     * @return 玩家实例，可能为 null
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * @return 当前维度，不可为 null
     */
    public ServerLevel getLevel() {
        return level;
    }

    /**
     * @return 服务器实例，不可为 null
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * @return 是否有玩家上下文
     */
    public boolean hasPlayer() {
        return player != null;
    }

    @Override
    public String toString() {
        return "EvaluationContext{" +
                "player=" + (player != null ? player.getName().getString() : "null") +
                ", level=" + level.dimension().toString() +
                '}';
    }
}

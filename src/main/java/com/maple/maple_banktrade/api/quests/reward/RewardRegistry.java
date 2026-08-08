package com.maple.maple_banktrade.api.quests.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 奖励注册表 —— 仿照 {@link com.maple.maple_banktrade.api.quests.condition.QuestConditionRegistry} 模式。
 *
 * <p>
 * 通过 {@link Identifier} 索引奖励工厂，支持从字符串格式的奖励 ID 解析并发放奖励。
 *
 * <p>
 * 奖励 ID 格式：{@code "typeId:key1=value1,key2=value2"}
 * 
 * <pre>{@code
 * "maple_banktrade:item:item=minecraft:diamond,count=3"
 * }</pre>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * // 注册工厂
 * RewardRegistry.register(id("item"), ItemReward::new);
 *
 * // 批量发放
 * RewardRegistry.grantRewards(List.of("maple_banktrade:item:item=minecraft:diamond,count=3"), player);
 * }</pre>
 */
public final class RewardRegistry {

    /** 初始化占位方法，触发静态块注册。 */
    public static void init() {}

    /**
     * 奖励工厂：从 {@link CompoundTag} 参数创建奖励实例。
     */
    @FunctionalInterface
    public interface RewardFactory {

        IReward create(CompoundTag params);
    }

    private static final Map<Identifier, RewardFactory> FACTORIES = new LinkedHashMap<>();

    static {
        registerAllDefaults();
    }

    // ==============================================
    // 注册方法
    // ==============================================

    /**
     * 注册一个奖励工厂。
     *
     * @param id      奖励类型标识符
     * @param factory 奖励工厂
     */
    public static void register(Identifier id, RewardFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (FACTORIES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting reward factory for id: {}", id);
        }
        FACTORIES.put(id, factory);
    }

    // ==============================================
    // 查询
    // ==============================================

    /**
     * 根据 ID 获取奖励实例。
     */
    public static IReward get(Identifier id, CompoundTag params) {
        if (id == null) return null;
        RewardFactory factory = FACTORIES.get(id);
        if (factory == null) {
            MapleBankTrade.LOGGER.warn("Unknown reward id: {}, skipping", id);
            return null;
        }
        return factory.create(params != null ? params : new CompoundTag());
    }

    /**
     * 检查指定奖励 ID 是否已注册。
     */
    public static boolean isRegistered(Identifier id) {
        return id != null && FACTORIES.containsKey(id);
    }

    /**
     * 获取已注册的奖励类型数量。
     */
    public static int size() {
        return FACTORIES.size();
    }

    // ==============================================
    // 批量发放（Identifier + CompoundTag，推荐）
    // ==============================================

    /**
     * 批量发放奖励（类型安全方式）。
     *
     * @param rewards 奖励定义列表，每个元素包含 {@link Identifier}（类型）和 {@link CompoundTag}（参数）
     * @param context 运行时上下文（通常为 {@code ServerPlayer}）
     */
    public static void grantRewards(List<RewardDef> rewards, Object context) {
        if (rewards == null || rewards.isEmpty()) return;

        for (RewardDef reward : rewards) {
            try {
                grantDef(reward, context);
            } catch (Exception e) {
                MapleBankTrade.LOGGER.error("Failed to grant reward '{}'", reward.typeId(), e);
            }
        }
    }

    /**
     * 发放单个奖励定义。
     */
    private static void grantDef(RewardDef def, Object context) {
        if (def.typeId() == null) {
            MapleBankTrade.LOGGER.warn("RewardDef with null typeId, skipping");
            return;
        }
        IReward reward = get(def.typeId(), def.params());
        if (reward != null) {
            reward.grant(def.params(), context);
        }
    }

    // ==============================================
    // 批量发放（字符串格式，保留向后兼容）
    // ==============================================

    /**
     * 批量发放奖励（字符串格式）。
     *
     * @param rewardIdStrs 奖励 ID 列表，格式为 {@code "typeId:key1=value1,key2=value2"}
     * @param context      运行时上下文（通常为 {@code ServerPlayer}）
     * @deprecated 推荐使用 {@link #grantRewards(List, Object)}（接收 {@link RewardDef} 列表）
     */
    @Deprecated
    public static void grantRewardStrings(List<String> rewardIdStrs, Object context) {
        if (rewardIdStrs == null || rewardIdStrs.isEmpty()) return;

        for (String rewardIdStr : rewardIdStrs) {
            try {
                grantSingle(rewardIdStr, context);
            } catch (Exception e) {
                MapleBankTrade.LOGGER.error("Failed to grant reward '{}'", rewardIdStr, e);
            }
        }
    }

    /**
     * 发放单个奖励。
     */
    private static void grantSingle(String rewardIdStr, Object context) {
        // 解析格式: "typeId:key1=value1,key2=value2"
        int colonIdx = rewardIdStr.indexOf(':');
        if (colonIdx < 0) {
            MapleBankTrade.LOGGER.warn("Invalid reward id format: '{}', expected 'typeId:key=val,...'", rewardIdStr);
            return;
        }

        // 解析 typeId（需要考虑 namespace:path 格式）
        // 格式: "namespace:path:key1=val1,key2=val2" 或 "path:key1=val1"
        // 找到第一个 ":" 后，再找第二个 ":" 或第一个 "="
        int secondColon = rewardIdStr.indexOf(':', colonIdx + 1);
        String typeIdStr;
        String paramsStr;

        if (secondColon > 0 && !rewardIdStr.substring(colonIdx + 1, secondColon).contains("=")) {
            // 有 namespace:path 格式
            typeIdStr = rewardIdStr.substring(0, secondColon);
            paramsStr = rewardIdStr.substring(secondColon + 1);
        } else {
            // 单个标识符格式
            typeIdStr = rewardIdStr.substring(0, colonIdx);
            paramsStr = rewardIdStr.substring(colonIdx + 1);
        }

        Identifier typeId;
        try {
            typeId = Identifier.parse(typeIdStr);
        } catch (Exception e) {
            MapleBankTrade.LOGGER.warn("Invalid reward type id: '{}'", typeIdStr, e);
            return;
        }

        CompoundTag params = parseParams(paramsStr);
        IReward reward = get(typeId, params);
        if (reward != null) {
            reward.grant(params, context);
        }
    }

    /**
     * 解析参数字符串为 CompoundTag。
     * 格式: "key1=value1,key2=value2"
     */
    private static CompoundTag parseParams(String paramsStr) {
        CompoundTag tag = new CompoundTag();
        if (paramsStr == null || paramsStr.isEmpty()) return tag;

        String[] pairs = paramsStr.split(",");
        for (String pair : pairs) {
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                String key = pair.substring(0, eqIdx).trim();
                String value = pair.substring(eqIdx + 1).trim();
                // 尝试解析为数字
                try {
                    tag.putInt(key, Integer.parseInt(value));
                } catch (NumberFormatException e1) {
                    try {
                        tag.putDouble(key, Double.parseDouble(value));
                    } catch (NumberFormatException e2) {
                        tag.putString(key, value);
                    }
                }
            }
        }
        return tag;
    }

    // ==============================================
    // 预注册
    // ==============================================

    private static void registerAllDefaults() {
        register(MapleBankTrade.id("item"), params -> new ItemReward());
    }

    private RewardRegistry() {}
}

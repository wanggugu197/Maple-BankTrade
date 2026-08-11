package com.maple.maple_banktrade.api.quests.reward;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.List;

/**
 * 奖励发放工具 —— 批量发放奖励。
 *
 * <p>
 * v3.6 重构：移除工厂/CompoundTag 查找模式，
 * 奖励实例直接存储参数，只需调用 {@code grant()}。
 */
public final class RewardRegistry {

    /** 初始化占位方法。 */
    public static void init() {}

    // ==============================================
    // 批量发放
    // ==============================================

    /**
     * 批量发放奖励。
     *
     * @param rewards 奖励实例列表
     * @param context 运行时上下文（通常为 {@code ServerPlayer}）
     */
    public static void grantRewards(List<IReward> rewards, Object context) {
        if (rewards == null || rewards.isEmpty()) return;

        for (IReward reward : rewards) {
            try {
                reward.grant(context);
            } catch (Exception e) {
                MapleBankTrade.LOGGER.error("Failed to grant reward: {}", reward, e);
            }
        }
    }

    private RewardRegistry() {}
}

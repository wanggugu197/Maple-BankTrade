package com.maple.maple_banktrade.api.quests.reward;

/**
 * 奖励接口 —— 任务完成后的奖励发放逻辑。
 *
 * <p>
 * v3.6 重构：移除 {@code getId()} 和 {@code CompoundTag} 参数，
 * 改为子类直接存储自身参数（物品实例、数量等），
 * {@code grant()} 仅接收运行时上下文。
 *
 * <p>
 * 使用示例：
 * 
 * <pre>{@code
 * IReward reward = ItemReward.of(Items.DIAMOND, 3);
 * reward.grant(player);
 * }</pre>
 */
public interface IReward {

    /**
     * 发放奖励。
     *
     * @param context 运行时上下文（通常为 {@code ServerPlayer}）
     */
    void grant(Object context);
}

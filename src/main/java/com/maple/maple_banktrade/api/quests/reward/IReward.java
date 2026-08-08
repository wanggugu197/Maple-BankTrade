package com.maple.maple_banktrade.api.quests.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 奖励接口 —— 定义任务完成后的奖励发放逻辑。
 *
 * <p>
 * 通过 {@link RewardRegistry} 注册和查找具体实现。
 *
 * <p>
 * 使用示例：
 * 
 * <pre>{@code
 * IReward reward = RewardRegistry.get(id("item"));
 * reward.grant(params, player);
 * }</pre>
 */
public interface IReward {

    /**
     * @return 奖励类型的唯一标识符
     */
    Identifier getId();

    /**
     * 发放奖励。
     *
     * @param params  奖励参数（从任务定义中解析）
     * @param context 运行时上下文（通常为 {@code ServerPlayer}）
     */
    void grant(CompoundTag params, Object context);
}

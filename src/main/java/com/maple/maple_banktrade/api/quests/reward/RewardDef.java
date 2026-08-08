package com.maple.maple_banktrade.api.quests.reward;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/**
 * 奖励定义 —— 不可变数据类，描述一个奖励的完整信息。
 *
 * <p>
 * 采用 {@link Identifier} + {@link CompoundTag} 存储，与任务类型、条件系统保持一致的
 * 类型安全设计。
 *
 * <p>
 * 示例：
 * 
 * <pre>{@code
 * new RewardDef(MapleBankTrade.id("item"), new CompoundTag() {
 * 
 *     {
 *         putString("item", "minecraft:diamond");
 *         putInt("count", 3);
 *     }
 * });
 * }</pre>
 */
public record RewardDef(Identifier typeId, CompoundTag params) {

    /**
     * @param typeId 奖励类型标识符（非 null）
     * @param params 奖励参数（非 null，无参数时传空 CompoundTag）
     */
    public RewardDef(Identifier typeId, CompoundTag params) {
        this.typeId = typeId;
        this.params = params != null ? params : new CompoundTag();
    }

    /**
     * 创建无参数的奖励定义。
     */
    public static RewardDef of(Identifier typeId) {
        return new RewardDef(typeId, new CompoundTag());
    }
}

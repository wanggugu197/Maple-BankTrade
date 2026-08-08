package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;

/**
 * 药水效果条件 —— 存储实际 {@link Holder}<{@link MobEffect}> 实例，不存储字符串 ID。
 */
public class HasPotionEffectCondition extends BaseQuestCondition {

    /** 实际药水效果 Holder 实例，不是字符串。 */
    private final Holder<MobEffect> effect;

    public HasPotionEffectCondition(Holder<MobEffect> effect) {
        this.effect = effect;
    }

    /** 从 CompoundTag 构造：读取 "effect" (string id)，构建时查找实际实例。 */
    public static HasPotionEffectCondition fromTag(CompoundTag tag) {
        String rawId = tag.getStringOr("effect", "");
        // 自动补全 minecraft 命名空间
        Identifier id = rawId.contains(":") ? Identifier.parse(rawId) : Identifier.parse("minecraft:" + rawId);
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        return new HasPotionEffectCondition(effect);
    }

    @Override
    public boolean evaluate(Object context) {
        Player player = safeCast(context, Player.class);
        if (player == null || effect == null) return false;
        return player.hasEffect(effect);
    }

    @Override
    public String toString() {
        return "HasPotionEffectCondition{effect=" + (effect != null ? effect.getRegisteredName() : "null") + "}";
    }
}

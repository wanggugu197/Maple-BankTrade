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

    private final Holder<MobEffect> effect;

    public HasPotionEffectCondition(Holder<MobEffect> effect) {
        this.effect = effect;
    }

    /** 静态工厂：通过效果 ID 字符串创建条件。 */
    public static HasPotionEffectCondition of(String effectId) {
        Identifier id = effectId.contains(":") ? Identifier.parse(effectId) : Identifier.parse("minecraft:" + effectId);
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        return new HasPotionEffectCondition(effect);
    }

    /** 从 CompoundTag 构造（保留注册表兼容）。 */
    public static HasPotionEffectCondition fromTag(CompoundTag tag) {
        String rawId = tag.getStringOr("effect", "");
        return of(rawId);
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

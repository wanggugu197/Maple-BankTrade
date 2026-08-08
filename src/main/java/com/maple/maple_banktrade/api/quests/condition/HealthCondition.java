package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家血量条件 —— 存储实际阈值，不存储字符串。
 */
public class HealthCondition extends BaseQuestCondition {

    private final float threshold;
    private final boolean lessThan;

    public HealthCondition(float threshold, boolean lessThan) {
        this.threshold = threshold;
        this.lessThan = lessThan;
    }

    /** 从 CompoundTag 构造：读取 "health" (float) 和 "compare" (string, 默认 "<")。 */
    public static HealthCondition fromTag(CompoundTag tag) {
        float health = tag.getFloatOr("health", 0);
        String compare = tag.getStringOr("compare", "");
        boolean lt = !">".equals(compare);
        return new HealthCondition(health, lt);
    }

    @Override
    public boolean evaluate(Object context) {
        Player player = safeCast(context, Player.class);
        if (player == null) return false;
        float health = player.getHealth();
        return lessThan ? health < threshold : health > threshold;
    }

    @Override
    public String toString() {
        return "HealthCondition{health " + (lessThan ? "<" : ">") + " " + threshold + "}";
    }
}

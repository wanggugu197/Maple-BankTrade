package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家等级条件 —— 存储实际阈值，不存储字符串。
 *
 * <p>
 * 使用 {@link CompoundTag} 构造：
 * 
 * <pre>{@code
 * CompoundTag tag = new CompoundTag();
 * tag.putInt("level", 1);
 * LevelCondition cond = LevelCondition.fromTag(tag);
 * }</pre>
 */
public class LevelCondition extends BaseQuestCondition {

    private final int threshold;
    private final boolean greaterOrEqual;

    public LevelCondition(int threshold, boolean greaterOrEqual) {
        this.threshold = threshold;
        this.greaterOrEqual = greaterOrEqual;
    }

    /** 从 CompoundTag 构造：读取 "level" (int) 和 "compare" (string, 默认 ">=")。 */
    public static LevelCondition fromTag(CompoundTag tag) {
        int level = tag.getIntOr("level", 0);
        String compare = tag.getStringOr("compare", "");
        boolean ge = !"<=".equals(compare);
        return new LevelCondition(level, ge);
    }

    @Override
    public boolean evaluate(Object context) {
        Player player = safeCast(context, Player.class);
        if (player == null) return false;
        int level = player.experienceLevel;
        return greaterOrEqual ? level >= threshold : level <= threshold;
    }

    @Override
    public String toString() {
        return "LevelCondition{level " + (greaterOrEqual ? ">=" : "<=") + " " + threshold + "}";
    }
}

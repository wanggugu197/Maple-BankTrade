package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 天气条件（下雨）—— 无状态，不需要存储参数。
 */
public class IsRainingCondition extends BaseQuestCondition {

    /** 从 CompoundTag 构造（无参数，忽略 tag 内容）。 */
    public static IsRainingCondition fromTag(CompoundTag tag) {
        return new IsRainingCondition();
    }

    @Override
    public boolean evaluate(Object context) {
        Player player = safeCast(context, Player.class);
        if (player != null) {
            return player.level().isRaining();
        }
        ServerLevel level = safeCast(context, ServerLevel.class);
        if (level != null) {
            return level.isRaining();
        }
        return false;
    }
}

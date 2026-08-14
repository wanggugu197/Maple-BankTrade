package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 玩家经验等级钩子：当触发实体为玩家且经验等级 ≥ {@link #level} 时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class PlayerExperienceVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private int level;
    @Persisted
    private boolean flip;

    public PlayerExperienceVisibleHook(int level) {
        this(level, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        if (!(context.entity() instanceof Player player)) {
            return flip;
        }
        return flip != (player.experienceLevel >= level);
    }
}

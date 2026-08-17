package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 玩家经验等级钩子：当触发实体为玩家且经验等级 ≥ {@link #level} 时可见。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class PlayerExperienceVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private int level;
    @Persisted
    private boolean flip;

    public PlayerExperienceVisibleHook(int level) {
        this(level, false);
    }

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        if (!(context.entity() instanceof Player player)) {
            return flip ? FLAG_VISIBLE : 0;
        }
        return (flip != (player.experienceLevel >= level)) ? FLAG_VISIBLE : 0;
    }
}

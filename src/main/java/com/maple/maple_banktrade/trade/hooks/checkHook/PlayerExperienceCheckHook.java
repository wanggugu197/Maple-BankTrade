package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 玩家经验等级钩子：当触发实体为玩家且经验等级 ≥ {@link #level} 时返回 true。
 * {@link #flip} 用于反转逻辑
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class PlayerExperienceCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private int level;
    @Persisted
    private boolean flip;

    public PlayerExperienceCheckHook(int level) {
        this(level, false);
    }

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        if (!(context.entity() instanceof Player player)) {
            return flip;
        }
        return flip != (player.experienceLevel >= level);
    }
}

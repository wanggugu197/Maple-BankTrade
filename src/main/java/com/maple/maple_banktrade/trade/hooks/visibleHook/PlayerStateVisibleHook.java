package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.UUID;

import javax.annotation.Nullable;

/**
 * 玩家身份钩子：当触发实体为玩家且 UUID / 玩家名匹配时返回 true。
 * <p>
 * {@link #flip} 用于反转逻辑；{@link #playerUuid} 与 {@link #playerName} 均为空时匹配任意玩家；
 * 非玩家触发者（如纯机器）时返回 {@link #flip}。
 * </p>
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class PlayerStateVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    @Nullable
    private UUID playerUuid;
    @Persisted
    @Nullable
    private String playerName;
    @Persisted
    private boolean flip;

    public PlayerStateVisibleHook(UUID playerUuid) {
        this(playerUuid, null, false);
    }

    public PlayerStateVisibleHook(String playerName) {
        this(null, playerName, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        if (!(context.entity() instanceof Player player)) {
            return flip;
        }
        boolean match;
        if (playerUuid != null) {
            match = playerUuid.equals(player.getUUID());
        } else if (playerName != null) {
            match = playerName.equals(player.getGameProfile().name());
        } else {
            match = true;
        }
        return flip != match;
    }
}

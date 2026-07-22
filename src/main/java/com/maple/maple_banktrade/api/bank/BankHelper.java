package com.maple.maple_banktrade.api.bank;

import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 玩家 UUID 读取工具，集中封装 {@link Player#getUUID()}。
 */
public final class BankHelper {

    public static UUID getUuid(Player player) {
        return player.getUUID();
    }

    private BankHelper() {}
}

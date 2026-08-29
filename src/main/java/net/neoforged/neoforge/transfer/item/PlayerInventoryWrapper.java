package net.neoforged.neoforge.transfer.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.maple.maple_banktrade.utils.PlayerInventoryItemStacksResourceHandler;

/**
 * 兼容层：模拟 NeoForge 26.x 的玩家物品栏包装。
 */
public final class PlayerInventoryWrapper {

    private final Player player;

    private PlayerInventoryWrapper(Player player) {
        this.player = player;
    }

    /** 包装玩家物品栏。 */
    public static PlayerInventoryWrapper of(Player player) {
        return new PlayerInventoryWrapper(player);
    }

    /** 获取主物品栏（0~35）的资源处理器；非服务端玩家返回空处理器。 */
    public ItemStacksResourceHandler getMainSlots() {
        if (player instanceof ServerPlayer serverPlayer) {
            return new PlayerInventoryItemStacksResourceHandler(serverPlayer);
        }
        return new ItemStacksResourceHandler(0);
    }
}

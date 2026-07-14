package com.example.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import static net.neoforged.neoforge.common.NeoForge.EVENT_BUS;

// 跟据需要注册NeoForge的事件监听器
public class NeoForgeCommonEvent {

    public static void init() {
        EVENT_BUS.register(NeoForgeCommonEvent.class);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Level level = event.getLevel();
        // if (level == null) return;
        // BlockPos pos = event.getPos();
        // Player player = event.getEntity();
        // InteractionHand hand = event.getHand();
        // ItemStack itemStack = player.getItemInHand(hand);
        // Item item = itemStack.getItem();
    }
}

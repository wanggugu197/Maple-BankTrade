package com.maple.maple_banktrade.api.quests.reward;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.MapleBankTrade;

/**
 * 物品奖励 —— 向玩家背包中发放物品。
 *
 * <p>
 * 参数格式：
 * 
 * <pre>{@code
 * {
 *   "item": "minecraft:diamond",  // 物品 ID
 *   "count": 3                     // 数量，默认 1
 * }
 * }</pre>
 */
public class ItemReward implements IReward {

    private static final Identifier ID = MapleBankTrade.id("item");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void grant(CompoundTag params, Object context) {
        if (!(context instanceof ServerPlayer player)) return;

        String itemId = params.getStringOr("item", "");
        if (itemId.isEmpty()) {
            MapleBankTrade.LOGGER.warn("ItemReward: missing 'item' parameter");
            return;
        }

        Item item;
        try {
            item = BuiltInRegistries.ITEM.get(Identifier.parse(itemId)).orElse(null).value();
        } catch (Exception e) {
            MapleBankTrade.LOGGER.warn("ItemReward: invalid item id '{}'", itemId, e);
            return;
        }

        if (item == null) {
            MapleBankTrade.LOGGER.warn("ItemReward: item not found '{}'", itemId);
            return;
        }

        int count = params.getIntOr("count", 1);
        if (count <= 0) count = 1;

        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            // 背包满时掉落在地面
            player.drop(stack, false);
        }

        MapleBankTrade.LOGGER.debug("ItemReward: granted {}x {} to {}",
                count, itemId, player.getName().getString());
    }

    @Override
    public String toString() {
        return "ItemReward";
    }
}

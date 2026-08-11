package com.maple.maple_banktrade.api.quests.reward;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.MapleBankTrade;

/**
 * 物品奖励 —— 直接存储 {@link Item} 实例和数量，不存字符串 ID。
 *
 * <p>
 * v3.6 重构：移除 {@code CompoundTag} 解析，改为直接存储物品实例。
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * 
 * IReward reward = ItemReward.of(Items.DIAMOND, 3);
 * }</pre>
 */
public class ItemReward implements IReward {

    private final Item item;
    private final int count;

    // ==============================================
    // 构造
    // ==============================================

    public ItemReward(Item item, int count) {
        this.item = item;
        this.count = count > 0 ? count : 1;
    }

    /** 静态工厂：创建指定数量的物品奖励。 */
    public static ItemReward of(Item item, int count) {
        return new ItemReward(item, count);
    }

    /** 静态工厂：创建单个物品奖励。 */
    public static ItemReward of(Item item) {
        return new ItemReward(item, 1);
    }

    // ==============================================
    // 奖励发放
    // ==============================================

    @Override
    public void grant(Object context) {
        if (!(context instanceof ServerPlayer player)) return;
        if (item == null) {
            MapleBankTrade.LOGGER.warn("ItemReward: item is null, skipping");
            return;
        }

        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        MapleBankTrade.LOGGER.debug("ItemReward: granted {}x {} to {}",
                count, item, player.getName().getString());
    }

    // ==============================================
    // 查询
    // ==============================================

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ItemReward{" + item + " x" + count + "}";
    }
}

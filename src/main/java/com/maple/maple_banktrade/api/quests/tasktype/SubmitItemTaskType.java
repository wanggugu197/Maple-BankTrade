package com.maple.maple_banktrade.api.quests.tasktype;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

/**
 * 提交物品类型 —— 直接存储 {@link Item} 实例和数量，不存字符串 ID。
 *
 * <p>
 * v3.6 重构：移除 {@code CompoundTag} 解析，改为直接存储物品实例。
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * 
 * ITaskType type = SubmitItemTaskType.of(Items.DIAMOND, 3);
 * }</pre>
 */
public class SubmitItemTaskType implements ITaskType {

    private final Item item;
    private final int count;

    // ==============================================
    // 构造
    // ==============================================

    public SubmitItemTaskType(Item item, int count) {
        this.item = item;
        this.count = count > 0 ? count : 1;
    }

    /** 静态工厂：创建指定数量的提交物品任务类型。 */
    public static SubmitItemTaskType of(Item item, int count) {
        return new SubmitItemTaskType(item, count);
    }

    /** 静态工厂：创建单个物品的提交任务类型。 */
    public static SubmitItemTaskType of(Item item) {
        return new SubmitItemTaskType(item, 1);
    }

    // ==============================================
    // 任务完成逻辑
    // ==============================================

    @Override
    public boolean canComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        if (!(context instanceof ServerPlayer player)) return false;
        if (item == null) return false;
        return countItems(player) >= count;
    }

    @Override
    public void onComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        if (!(context instanceof ServerPlayer player)) return;
        if (item == null) return;
        removeItems(player, count);
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

    private int countItems(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private void removeItems(ServerPlayer player, int toRemove) {
        int remaining = toRemove;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    @Override
    public String toString() {
        return "SubmitItemTaskType{" + item + " x" + count + "}";
    }
}

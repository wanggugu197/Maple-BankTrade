package com.maple.maple_banktrade.api.quests.tasktype;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

/**
 * 提交物品类型 —— 检查背包中是否有足够物品，完成时扣除。
 *
 * <p>
 * 参数通过 {@link ITaskDefinition#getTaskTypeParams()} 配置：
 * 
 * <pre>{@code
 * {
 *   "item": "minecraft:diamond",   // 物品 ID
 *   "count": 3                      // 需要数量，默认 1
 * }
 * }</pre>
 */
public class SubmitItemTaskType implements ITaskType {

    private static final Identifier ID = MapleBankTrade.id("submit_item");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean canComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        if (!(context instanceof ServerPlayer player)) return false;

        CompoundTag params = def.getTaskTypeParams();
        Item item = resolveItem(params);
        int count = params.getIntOr("count", 1);

        if (item == null || count <= 0) return false;

        return countItems(player, item) >= count;
    }

    @Override
    public void onComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        if (!(context instanceof ServerPlayer player)) return;

        CompoundTag params = def.getTaskTypeParams();
        Item item = resolveItem(params);
        int count = params.getIntOr("count", 1);

        if (item == null || count <= 0) return;

        removeItems(player, item, count);
    }

    // ==============================================
    // 内部方法
    // ==============================================

    private static Item resolveItem(CompoundTag params) {
        String itemId = params.getStringOr("item", "");
        if (itemId.isEmpty()) return null;
        try {
            return BuiltInRegistries.ITEM.get(Identifier.parse(itemId)).orElse(null).value();
        } catch (Exception e) {
            MapleBankTrade.LOGGER.warn("SubmitItemTaskType: invalid item id '{}'", itemId, e);
            return null;
        }
    }

    private static int countItems(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeItems(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (remaining <= 0) break;
            }
        }
    }

    @Override
    public String toString() {
        return "SubmitItemTaskType";
    }
}

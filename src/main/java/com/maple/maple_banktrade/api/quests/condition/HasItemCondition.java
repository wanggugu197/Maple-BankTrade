package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 背包物品条件 —— 存储实际 {@link Item} 实例，不存储字符串 ID。
 *
 * <p>
 * 物品查找在 {@link #fromTag(CompoundTag)} 时完成，评估时直接使用实例。
 */
public class HasItemCondition extends BaseQuestCondition {

    /** 实际物品实例，不是字符串。 */
    private final Item item;

    public HasItemCondition(Item item) {
        this.item = item;
    }

    /** 从 CompoundTag 构造：读取 "item" (string id)，构建时查找实际 Item 实例。 */
    public static HasItemCondition fromTag(CompoundTag tag) {
        Identifier id = Identifier.parse(tag.getStringOr("item", ""));
        Item item = BuiltInRegistries.ITEM.get(id).orElse(null).value();
        return new HasItemCondition(item);
    }

    @Override
    public boolean evaluate(Object context) {
        Player player = safeCast(context, Player.class);
        if (player == null || item == null) return false;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "HasItemCondition{item=" + BuiltInRegistries.ITEM.getKey(item) + "}";
    }
}

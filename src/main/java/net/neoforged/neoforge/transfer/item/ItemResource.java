package net.neoforged.neoforge.transfer.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.transfer.resource.Resource;

/**
 * 兼容层：模拟 NeoForge 26.x 的物品资源标识（不含数量，按物品与组件判等）。
 */
public final class ItemResource implements Resource {

    private static final ItemResource EMPTY = new ItemResource(ItemStack.EMPTY);

    private final ItemStack stack;

    private ItemResource(ItemStack stack) {
        this.stack = stack;
    }

    /** 由物品堆叠创建资源（忽略数量）。 */
    public static ItemResource of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY;
        }
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new ItemResource(copy);
    }

    /** 由物品创建资源。 */
    public static ItemResource of(Item item) {
        return item == null ? EMPTY : of(new ItemStack(item));
    }

    /** 由 ItemLike 创建资源。 */
    public static ItemResource of(ItemLike item) {
        return item == null ? EMPTY : of(new ItemStack(item));
    }

    /** 空物品资源。 */
    public static ItemResource empty() {
        return EMPTY;
    }

    /** 底层物品。 */
    public Item item() {
        return stack.getItem();
    }

    /** 是否为空资源。 */
    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** 匹配给定堆叠（按物品与组件）。 */
    public boolean matches(ItemStack other) {
        return other != null && !other.isEmpty() && ItemStack.isSameItemSameComponents(stack, other);
    }

    /** 转为指定数量的物品堆叠。 */
    public ItemStack toStack(int count) {
        if (count <= 0 || isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = stack.copy();
        out.setCount(count);
        return out;
    }

    /** 该物品的最大堆叠数。 */
    public int getMaxStackSize() {
        return stack.getMaxStackSize();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemResource other)) {
            return false;
        }
        if (isEmpty() || other.isEmpty()) {
            return isEmpty() && other.isEmpty();
        }
        return ItemStack.isSameItemSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }
        return ItemStack.hashItemAndComponents(stack);
    }

    @Override
    public String toString() {
        return "ItemResource{" + stack + "}";
    }
}

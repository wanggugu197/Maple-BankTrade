package net.neoforged.neoforge.transfer.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * 兼容层：模拟 NeoForge 26.x 的 {@code ItemStacksResourceHandler}。
 * <p>
 * 1.21.1 中物品存储基于 {@link ObservableItemResourceHandler}（MapleUtilLib 0.3.0，
 * 继承自 NeoForge {@code ItemStackHandler}），本基类将其适配为资源处理器语义。
 * 子类可覆盖全部资源方法以对接自定义存储（如玩家物品栏）。
 * </p>
 */
public class ItemStacksResourceHandler extends ObservableItemResourceHandler
                                       implements ResourceHandler<ItemResource> {

    /** 兼容构造：创建指定槽位数的内部存储。 */
    public ItemStacksResourceHandler(int size) {
        super(size);
    }

    // ── ResourceHandler 默认实现（基于 ItemStackHandler API） ──

    @Override
    public ItemResource getResource(int index) {
        return ItemResource.of(getStackInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return getStackInSlot(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, @NonNull ItemResource resource) {
        return getSlotCapacity(index, resource.toStack(1));
    }

    @Override
    public boolean isValid(int index, @NonNull ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        int before = getStackInSlot(index).getCount();
        ItemStack remaining = insertItem(index, resource.toStack(amount), false);
        int inserted = amount - remaining.getCount();
        if (inserted > 0 && transaction instanceof net.neoforged.neoforge.transfer.transaction.Transaction tx) {
            int snapshot = before;
            tx.addRollback(() -> {
                ItemStack cur = getStackInSlot(index);
                if (cur.isEmpty()) {
                    if (snapshot > 0) {
                        setStackInSlot(index, resource.toStack(snapshot));
                    }
                } else {
                    cur.setCount(snapshot);
                    if (snapshot <= 0) {
                        setStackInSlot(index, ItemStack.EMPTY);
                    }
                }
            });
        }
        return inserted;
    }

    @Override
    public int extract(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        ItemStack current = getStackInSlot(index);
        if (current.isEmpty() || !resource.matches(current)) {
            return 0;
        }
        int extracted = Math.min(amount, current.getCount());
        if (extracted <= 0) {
            return 0;
        }
        if (transaction instanceof net.neoforged.neoforge.transfer.transaction.Transaction tx) {
            ItemStack snapshot = current.copy();
            tx.addRollback(() -> setStackInSlot(index, snapshot));
        }
        ItemStack out = extractItem(index, extracted, false);
        return out.getCount();
    }

    /** 直接设置槽位内容（用于完整性与兼容）。 */
    public void set(int index, ItemResource resource, int amount) {
        setStackInSlot(index, resource.toStack(amount));
    }

    /** 复制当前全部堆叠。 */
    public NonNullList<ItemStack> copyToList() {
        NonNullList<ItemStack> list = NonNullList.withSize(size(), ItemStack.EMPTY);
        for (int i = 0; i < size(); i++) {
            list.set(i, getStackInSlot(i).copy());
        }
        return list;
    }

    /** 获取指定槽位中物品的总数量（按物品匹配，用于搜索）。 */
    public int count(ItemResource resource) {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty() && resource.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * 将现有 {@link ObservableItemResourceHandler} 包装为资源处理器（委托模式）。
     * <p>
     * 用于将 MapleUtilLib 的可观察处理器接入 {@link ResourceHandler} 语义
     * （如 {@link net.neoforged.neoforge.transfer.CombinedResourceHandler}）。
     * </p>
     */
    public static ResourceHandler<ItemResource> wrap(ObservableItemResourceHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return new ResourceHandler<>() {

            @Override
            public int size() {
                return handler.size();
            }

            @Override
            public @NonNull ItemResource getResource(int index) {
                return ItemResource.of(handler.getStackInSlot(index));
            }

            @Override
            public long getAmountAsLong(int index) {
                return handler.getStackInSlot(index).getCount();
            }

            @Override
            public long getCapacityAsLong(int index, @NonNull ItemResource resource) {
                return handler.getSlotCapacity(index, resource.toStack(1));
            }

            @Override
            public boolean isValid(int index, @NonNull ItemResource resource) {
                return true;
            }

            @Override
            public int insert(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
                if (amount <= 0 || resource.isEmpty()) {
                    return 0;
                }
                ItemStack remaining = handler.insertItem(index, resource.toStack(amount), false);
                return amount - remaining.getCount();
            }

            @Override
            public int extract(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
                if (amount <= 0 || resource.isEmpty()) {
                    return 0;
                }
                return handler.extractItem(index, amount, false).getCount();
            }
        };
    }
}

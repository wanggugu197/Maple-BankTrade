package com.maple.maple_banktrade.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 直接操作玩家主物品栏（0~35）的物品资源处理器，支持事务回滚。
 * <p>
 * 兼容层说明：1.21.1 没有 NeoForge 26.x 的事务 API，本类基于
 * {@link SnapshotJournal} 兼容实现记录修改前状态，配合
 * {@link net.neoforged.neoforge.transfer.transaction.Transaction} 回滚。
 * </p>
 */
public final class PlayerInventoryItemStacksResourceHandler extends ItemStacksResourceHandler {

    private final ServerPlayer player;
    private final NonNullList<ItemStack> backingList;
    private final List<SlotJournal> slotJournals;

    public PlayerInventoryItemStacksResourceHandler(ServerPlayer player) {
        super(0); // 父类内部列表未被使用
        this.player = player;
        this.backingList = player.getInventory().items;
        this.slotJournals = new ArrayList<>(backingList.size());
        for (int i = 0; i < backingList.size(); i++) {
            slotJournals.add(new SlotJournal(i));
        }
    }

    // ── 基础访问（重定向到玩家物品栏） ──

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    public int getSlots() {
        return backingList.size();
    }

    @Override
    public @NonNull ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= backingList.size()) {
            return ItemStack.EMPTY;
        }
        return backingList.get(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    /**
     * 槽位容量：与 {@link #insertBypassFilter} 实际可插入上限保持一致。
     * <p>
     * 本类以 {@code super(0)} 构造，父类（继承链自 {@code ObservableItemResourceHandler}）的内部
     * 容量/锁定数组为空，其 {@code getSlotCapacity} 对任意槽位恒返回 0；而机器交易流程
     * （{@code MachineTradeDefinition}）会先调用 {@code getSlotCapacity} 计算剩余空间、
     * 再决定是否执行插入。若此处返回 0，手持交易符的机器交易会一直判定「无可执行次数」，
     * 永远无法产出。玩家主物品栏每槽上限为 64 与物品自身最大堆叠的较小值。
     * </p>
     */
    @Override
    public synchronized int getSlotCapacity(int slot, ItemStack stack) {
        if (slot < 0 || slot >= backingList.size()) {
            return 0;
        }
        return stack == null || stack.isEmpty() ? 64 : stack.getMaxStackSize();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < backingList.size()) {
            backingList.set(slot, stack);
        }
    }

    @Override
    public @NonNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return insertBypassFilter(slot, stack, simulate);
    }

    @Override
    public @NonNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractBypassFilter(slot, amount, simulate);
    }

    // ── 跳过过滤器的 I/O（玩家物品栏无过滤器，直接操作） ──

    @Override
    public synchronized ItemStack insertBypassFilter(int slot, ItemStack stack, boolean simulate) {
        if (stack == null || stack.isEmpty() || slot < 0 || slot >= backingList.size()) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        ItemStack current = backingList.get(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, stack)) {
            return stack;
        }
        int cap = Math.min(64, stack.getMaxStackSize());
        int space = Math.max(0, cap - current.getCount());
        int insert = Math.min(stack.getCount(), space);
        if (insert <= 0) {
            return stack;
        }
        if (!simulate) {
            if (current.isEmpty()) {
                backingList.set(slot, stack.copyWithCount(insert));
            } else {
                current.grow(insert);
            }
            player.getInventory().setChanged();
        }
        ItemStack rem = stack.copy();
        rem.shrink(insert);
        return rem;
    }

    @Override
    public synchronized ItemStack extractBypassFilter(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= backingList.size() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = backingList.get(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int extract = Math.min(amount, current.getCount());
        if (extract <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = current.copyWithCount(extract);
        if (!simulate) {
            current.shrink(extract);
            if (current.isEmpty()) {
                backingList.set(slot, ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
        }
        return out;
    }

    // ── ResourceHandler 实现 ──

    @Override
    public @NonNull ItemResource getResource(int index) {
        return ItemResource.of(backingList.get(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return backingList.get(index).getCount();
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return resource.isEmpty() ? 64 : resource.getMaxStackSize();
    }

    @Override
    public boolean isValid(int index, @NonNull ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        ItemStack current = backingList.get(index);
        if (!current.isEmpty() && !resource.matches(current)) {
            return 0;
        }

        int maxInsert = Math.min(amount, (int) getCapacityAsLong(index, resource) - current.getCount());
        if (maxInsert <= 0) {
            return 0;
        }

        // 更新快照（记录修改前状态）
        slotJournals.get(index).updateSnapshots(transaction);

        // 执行修改
        if (current.isEmpty()) {
            backingList.set(index, resource.toStack(maxInsert));
        } else {
            current.grow(maxInsert);
        }
        player.getInventory().setChanged();
        return maxInsert;
    }

    @Override
    public int extract(int index, @NonNull ItemResource resource, int amount, @NonNull TransactionContext transaction) {
        ItemStack current = backingList.get(index);
        if (current.isEmpty() || !resource.matches(current)) {
            return 0;
        }

        int extracted = Math.min(amount, current.getCount());
        if (extracted <= 0) {
            return 0;
        }

        // 更新快照
        slotJournals.get(index).updateSnapshots(transaction);

        // 执行修改
        current.shrink(extracted);
        if (current.isEmpty()) {
            backingList.set(index, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
        return extracted;
    }

    @Override
    public void set(int index, ItemResource resource, int amount) {
        // 通常交易不用 set，但为了完整性，简单实现（不支持事务回滚）
        backingList.set(index, resource.toStack(amount));
        player.getInventory().setChanged();
    }

    @Override
    public NonNullList<ItemStack> copyToList() {
        return NonNullList.of(ItemStack.EMPTY, backingList.toArray(new ItemStack[0]));
    }

    /**
     * 每个槽位的快照 journal，直接操作 backingList。
     */
    private class SlotJournal extends SnapshotJournal<ItemStack> {

        private final int index;

        private SlotJournal(int index) {
            this.index = index;
        }

        @Override
        protected ItemStack createSnapshot() {
            // 复制当前堆栈
            return backingList.get(index).copy();
        }

        @Override
        protected void revertToSnapshot(ItemStack snapshot) {
            // 回滚到快照
            backingList.set(index, snapshot);
            player.getInventory().setChanged();
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            // 提交时不做特别处理（可触发变更事件）
        }
    }
}

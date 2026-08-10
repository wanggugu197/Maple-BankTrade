package com.maple.maple_banktrade.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 直接操作玩家主物品栏（0~35）的 ItemStacksResourceHandler，支持事务回滚。
 * 使用 NeoForge 的 SnapshotJournal 机制。
 */
public final class PlayerInventoryItemStacksResourceHandler extends ItemStacksResourceHandler {

    private final ServerPlayer player;
    private final NonNullList<ItemStack> backingList;
    private final List<SlotJournal> slotJournals;

    public PlayerInventoryItemStacksResourceHandler(ServerPlayer player) {
        super(0); // 父类内部列表未被使用
        this.player = player;
        this.backingList = player.getInventory().getNonEquipmentItems();
        this.slotJournals = new ArrayList<>(backingList.size());
        for (int i = 0; i < backingList.size(); i++) {
            slotJournals.add(new SlotJournal(i));
        }
    }

    @Override
    public int size() {
        return backingList.size();
    }

    @Override
    public ItemResource getResource(int index) {
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
    public boolean isValid(int index, ItemResource resource) {
        return true;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        ItemStack current = backingList.get(index);
        if (!current.isEmpty() && !resource.matches(current)) {
            return 0;
        }

        int maxInsert = Math.min(amount, (int) getCapacityAsLong(index, resource) - current.getCount());
        if (maxInsert <= 0) {
            return 0;
        }

        // 更新快照（记录修改前状态）
        if (transaction != null) {
            slotJournals.get(index).updateSnapshots(transaction);
        }

        // 执行修改
        if (current.isEmpty()) {
            backingList.set(index, resource.toStack(maxInsert));
        } else {
            current.grow(maxInsert);
        }
        return maxInsert;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        ItemStack current = backingList.get(index);
        if (current.isEmpty() || !resource.matches(current)) {
            return 0;
        }

        int extracted = Math.min(amount, current.getCount());
        if (extracted <= 0) {
            return 0;
        }

        // 更新快照
        if (transaction != null) {
            slotJournals.get(index).updateSnapshots(transaction);
        }

        // 执行修改
        current.shrink(extracted);
        if (current.isEmpty()) {
            backingList.set(index, ItemStack.EMPTY);
        }
        return extracted;
    }

    @Override
    public void set(int index, ItemResource resource, int amount) {
        // 通常交易不用 set，但为了完整性，简单实现（不支持事务回滚）
        backingList.set(index, resource.toStack(amount));
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
        }

        @Override
        protected void onRootCommit(ItemStack originalState) {
            // 提交时不做特别处理（可触发变更事件）
        }
    }
}

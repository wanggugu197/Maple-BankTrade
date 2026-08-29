package net.neoforged.neoforge.transfer.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * 兼容层：模拟 NeoForge 26.x 的简单能量处理器，同时实现 1.21.1 的 {@link IEnergyStorage}。
 * <p>
 * 实现 {@link INBTSerializable} 以满足 LDLib2 同步系统对 {@code @Persisted}/{@code @DescSynced}
 * 字段的访问器要求（否则 {@code AccessorRegistries.findByType} 找不到类型会崩溃）。
 * </p>
 */
public class SimpleEnergyHandler implements EnergyHandler, IEnergyStorage, INBTSerializable<CompoundTag> {

    /**
     * -- GETTER --
     * 当前储存能量。
     */
    @Getter
    private int stored;
    private final int capacity;

    public SimpleEnergyHandler(int capacity) {
        this.capacity = Math.max(0, capacity);
        this.stored = 0;
    }

    /** 直接设置储存能量（越界自动钳制），变化时回调 {@link #onEnergyChanged}。 */
    public void setStored(int amount) {
        int previous = stored;
        int next = Math.clamp(amount, 0, capacity);
        if (next != previous) {
            stored = next;
            onEnergyChanged(previous);
        }
    }

    /** 能量变化回调（可覆盖）。 */
    protected void onEnergyChanged(int previousAmount) {}

    // ── INBTSerializable ──

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NonNull Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stored", stored);
        tag.putInt("capacity", capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NonNull Provider provider, @NonNull CompoundTag tag) {
        if (tag.contains("stored", Tag.TAG_INT)) {
            // 容量由构造决定（final），此处仅恢复当前储量并按容量钳制
            setStored(tag.getInt("stored"));
        }
    }

    // ── EnergyHandler ──

    @Override
    public long getAmountAsLong() {
        return stored;
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }
        int space = capacity - stored;
        if (space <= 0) {
            return 0;
        }
        int inserted = Math.min(amount, space);
        int before = stored;
        if (transaction instanceof Transaction tx) {
            tx.addRollback(() -> setStored(before));
        }
        setStored(stored + inserted);
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, stored);
        if (extracted <= 0) {
            return 0;
        }
        int before = stored;
        if (transaction instanceof Transaction tx) {
            tx.addRollback(() -> setStored(before));
        }
        setStored(stored - extracted);
        return extracted;
    }

    // ── IEnergyStorage ──

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }
        int space = capacity - stored;
        if (space <= 0) {
            return 0;
        }
        int received = Math.min(maxReceive, space);
        if (!simulate) {
            setStored(stored + received);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }
        int extracted = Math.min(maxExtract, stored);
        if (extracted <= 0) {
            return 0;
        }
        if (!simulate) {
            setStored(stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return stored;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}

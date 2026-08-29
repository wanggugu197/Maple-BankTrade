package net.neoforged.neoforge.transfer.energy;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 兼容层：模拟 NeoForge 26.x 的能量处理器接口。
 */
public interface EnergyHandler {

    /** 当前储存能量（long）。 */
    long getAmountAsLong();

    /** 最大容量（long）。 */
    long getCapacityAsLong();

    /** 当前储存能量（int）。 */
    default int getAmountAsInt() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, getAmountAsLong()));
    }

    /** 最大容量（int）。 */
    default int getCapacityAsInt() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, getCapacityAsLong()));
    }

    /** 插入能量，返回实际插入数量。 */
    int insert(int amount, TransactionContext transaction);

    /** 提取能量，返回实际提取数量。 */
    int extract(int amount, TransactionContext transaction);
}

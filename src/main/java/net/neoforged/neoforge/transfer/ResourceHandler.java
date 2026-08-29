package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.NonNull;

/**
 * 兼容层：模拟 NeoForge 26.x 的 {@code ResourceHandler} 接口。
 */
public interface ResourceHandler<T extends Resource> {

    /** 槽位数量。 */
    int size();

    /** 获取指定槽位的资源。 */
    @NonNull
    T getResource(int index);

    /** 获取指定槽位的资源数量（long）。 */
    long getAmountAsLong(int index);

    /** 获取指定槽位的资源数量（int）。 */
    default int getAmountAsInt(int index) {
        return (int) getAmountAsLong(index);
    }

    /** 获取指定槽位对给定资源的容量（long）。 */
    long getCapacityAsLong(int index, @NonNull T resource);

    /** 获取指定槽位对给定资源的容量（int）。 */
    default int getCapacityAsInt(int index, @NonNull T resource) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, getCapacityAsLong(index, resource)));
    }

    /** 判断资源是否可写入该槽位。 */
    boolean isValid(int index, @NonNull T resource);

    /** 向指定槽位插入资源，返回实际插入数量。 */
    int insert(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction);

    /** 从指定槽位提取资源，返回实际提取数量。 */
    int extract(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction);

    /** 跨槽位插入资源，返回实际插入数量。 */
    default int insert(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        int inserted = 0;
        for (int i = 0; i < size() && inserted < amount; i++) {
            if (!isValid(i, resource)) {
                continue;
            }
            int got = insert(i, resource, amount - inserted, transaction);
            if (got <= 0) {
                continue;
            }
            inserted += got;
        }
        return inserted;
    }

    /** 跨槽位提取资源，返回实际提取数量。 */
    default int extract(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        int extracted = 0;
        for (int i = 0; i < size() && extracted < amount; i++) {
            T current = getResource(i);
            if (current.isEmpty() || !current.equals(resource)) {
                continue;
            }
            int got = extract(i, current, amount - extracted, transaction);
            if (got <= 0) {
                continue;
            }
            extracted += got;
        }
        return extracted;
    }
}

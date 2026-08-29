package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * 兼容层：模拟 NeoForge 26.x 的组合资源处理器（两个处理器按顺序拼接为一个）。
 */
public class CombinedResourceHandler<T extends Resource> implements ResourceHandler<T> {

    private final ResourceHandler<T> first;
    private final ResourceHandler<T> second;

    public CombinedResourceHandler(ResourceHandler<T> first, ResourceHandler<T> second) {
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
    }

    private ResourceHandler<T> handlerFor(int index) {
        return index < first.size() ? first : second;
    }

    private int localIndex(int index) {
        return index < first.size() ? index : index - first.size();
    }

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public @NonNull T getResource(int index) {
        return handlerFor(index).getResource(localIndex(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return handlerFor(index).getAmountAsLong(localIndex(index));
    }

    @Override
    public long getCapacityAsLong(int index, @NonNull T resource) {
        return handlerFor(index).getCapacityAsLong(localIndex(index), resource);
    }

    @Override
    public boolean isValid(int index, @NonNull T resource) {
        return handlerFor(index).isValid(localIndex(index), resource);
    }

    @Override
    public int insert(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return handlerFor(index).insert(localIndex(index), resource, amount, transaction);
    }

    @Override
    public int extract(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return handlerFor(index).extract(localIndex(index), resource, amount, transaction);
    }
}

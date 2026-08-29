package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * 兼容层：模拟 NeoForge 26.x 的委托资源处理器基类。
 * <p>
 * 子类覆盖特定方法以改变行为（如 {@code InsertOnly} 禁止提取）。
 * </p>
 */
public abstract class DelegatingResourceHandler<T extends Resource> implements ResourceHandler<T> {

    protected final ResourceHandler<T> delegate;

    protected DelegatingResourceHandler(ResourceHandler<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public @NonNull T getResource(int index) {
        return delegate.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return delegate.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, @NonNull T resource) {
        return delegate.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, @NonNull T resource) {
        return delegate.isValid(index, resource);
    }

    @Override
    public int insert(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return delegate.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, @NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return delegate.extract(index, resource, amount, transaction);
    }

    @Override
    public int insert(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return delegate.insert(resource, amount, transaction);
    }

    @Override
    public int extract(@NonNull T resource, int amount, @NonNull TransactionContext transaction) {
        return delegate.extract(resource, amount, transaction);
    }
}

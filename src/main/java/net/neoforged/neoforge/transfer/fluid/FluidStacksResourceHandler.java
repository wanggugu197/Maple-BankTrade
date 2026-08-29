package net.neoforged.neoforge.transfer.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.mapleutillib.api.resource.ObservableFluidResourceHandler;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 兼容层：模拟 NeoForge 26.x 的 {@code FluidStacksResourceHandler}。
 * <p>
 * 1.21.1 中流体存储基于 {@link ObservableFluidResourceHandler}（MapleUtilLib 0.3.0，
 * 实现 NeoForge {@code IFluidHandler}），本基类将其适配为资源处理器语义。
 * </p>
 */
public class FluidStacksResourceHandler extends ObservableFluidResourceHandler
                                        implements ResourceHandler<FluidResource> {

    /** 兼容构造：指定槽位数与默认容量（mB）。 */
    public FluidStacksResourceHandler(int size, int capacity) {
        super(size, capacity);
    }

    // ── ResourceHandler 默认实现（基于 IFluidHandler API） ──

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(getFluidInSlot(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        return getFluidInSlot(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, @NonNull FluidResource resource) {
        return getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, @NonNull FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        FluidStack before = getFluidInSlot(index).copy();
        int inserted = insert(index, resource.toStack(amount), amount);
        if (inserted > 0 && transaction instanceof net.neoforged.neoforge.transfer.transaction.Transaction tx) {
            tx.addRollback(() -> setFluidInSlot(index, before));
        }
        return inserted;
    }

    @Override
    public int extract(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        FluidStack current = getFluidInSlot(index);
        if (current.isEmpty() || !resource.matches(current)) {
            return 0;
        }
        int extracted = Math.min(amount, current.getAmount());
        if (extracted <= 0) {
            return 0;
        }
        if (transaction instanceof net.neoforged.neoforge.transfer.transaction.Transaction tx) {
            FluidStack snapshot = current.copy();
            tx.addRollback(() -> setFluidInSlot(index, snapshot));
        }
        FluidStack out = extract(index, resource.toStack(amount), extracted);
        return out.getAmount();
    }

    /** 直接设置槽位内容（用于完整性与兼容）。 */
    public void set(int index, FluidResource resource, int amount) {
        setFluidInSlot(index, resource.toStack(amount));
    }

    /** 复制当前全部流体堆叠。 */
    public List<FluidStack> copyToList() {
        List<FluidStack> list = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            list.add(getFluidInSlot(i).copy());
        }
        return list;
    }

    /** 获取指定槽位中流体的总数量（按流体匹配，用于搜索）。 */
    public int count(FluidResource resource) {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            FluidStack stack = getFluidInSlot(i);
            if (!stack.isEmpty() && resource.matches(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * 将现有 {@link ObservableFluidResourceHandler} 包装为资源处理器（委托模式）。
     */
    public static ResourceHandler<FluidResource> wrap(ObservableFluidResourceHandler handler) {
        Objects.requireNonNull(handler, "handler");
        return new ResourceHandler<>() {

            @Override
            public int size() {
                return handler.size();
            }

            @Override
            public @NonNull FluidResource getResource(int index) {
                return FluidResource.of(handler.getFluidInSlot(index));
            }

            @Override
            public long getAmountAsLong(int index) {
                return handler.getFluidInSlot(index).getAmount();
            }

            @Override
            public long getCapacityAsLong(int index, @NonNull FluidResource resource) {
                return handler.getTankCapacity(index);
            }

            @Override
            public boolean isValid(int index, @NonNull FluidResource resource) {
                return true;
            }

            @Override
            public int insert(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
                if (amount <= 0 || resource.isEmpty()) {
                    return 0;
                }
                return handler.insert(index, resource.toStack(amount), amount);
            }

            @Override
            public int extract(int index, @NonNull FluidResource resource, int amount, @NonNull TransactionContext transaction) {
                if (amount <= 0 || resource.isEmpty()) {
                    return 0;
                }
                return handler.extract(index, amount).getAmount();
            }
        };
    }
}

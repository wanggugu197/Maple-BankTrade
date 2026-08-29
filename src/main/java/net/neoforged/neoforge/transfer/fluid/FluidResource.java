package net.neoforged.neoforge.transfer.fluid;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.resource.Resource;

/**
 * 兼容层：模拟 NeoForge 26.x 的流体资源标识（不含数量，按流体与组件判等）。
 */
public final class FluidResource implements Resource {

    private static final FluidResource EMPTY = new FluidResource(FluidStack.EMPTY);

    private final FluidStack stack;

    private FluidResource(FluidStack stack) {
        this.stack = stack;
    }

    /** 由流体堆叠创建资源（忽略数量）。 */
    public static FluidResource of(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return EMPTY;
        }
        FluidStack copy = stack.copyWithAmount(1);
        return new FluidResource(copy);
    }

    /** 由流体创建资源。 */
    public static FluidResource of(Fluid fluid) {
        return fluid == null ? EMPTY : of(new FluidStack(fluid, 1));
    }

    /** 空流体资源。 */
    public static FluidResource empty() {
        return EMPTY;
    }

    /** 底层流体。 */
    public Fluid fluid() {
        return stack.getFluid();
    }

    /** 是否为空资源。 */
    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** 匹配给定流体堆叠（按流体与组件）。 */
    public boolean matches(FluidStack other) {
        return other != null && !other.isEmpty() && FluidStack.isSameFluidSameComponents(stack, other);
    }

    /** 转为指定数量的流体堆叠。 */
    public FluidStack toStack(int amount) {
        if (amount <= 0 || isEmpty()) {
            return FluidStack.EMPTY;
        }
        return stack.copyWithAmount(amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FluidResource other)) {
            return false;
        }
        if (isEmpty() || other.isEmpty()) {
            return isEmpty() && other.isEmpty();
        }
        return FluidStack.isSameFluidSameComponents(stack, other.stack);
    }

    @Override
    public int hashCode() {
        if (isEmpty()) {
            return 0;
        }
        return FluidStack.hashFluidAndComponents(stack);
    }

    @Override
    public String toString() {
        return "FluidResource{" + stack + "}";
    }
}

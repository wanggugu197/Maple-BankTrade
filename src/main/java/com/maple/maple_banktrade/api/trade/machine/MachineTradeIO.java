package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * 机器交易单次 I/O 规格与按次数放大后的快照。
 * <p>
 * 物品/流体资源使用 {@link Supplier} 惰性解析，避免模组构造期
 * {@code ItemResource.of} / {@code FluidResource.of} 触发
 * “Components not bound yet”。
 * </p>
 */
@UtilityClass
public class MachineTradeIO {

    // ==============================================
    // 物品
    // ==============================================

    /**
     * 物品规格：惰性资源 + 数量。
     */
    public record ItemIO(@NonNull Supplier<ItemResource> resourceSupplier, int amount) {

        public ItemIO {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }

        /** 解析物品资源（须在 components 绑定后调用）。 */
        public ItemResource resource() {
            ItemResource resource = resourceSupplier.get();
            return resource == null ? ItemResource.EMPTY : resource;
        }

        /**
         * 由物品栈创建。仅保存栈副本与 Supplier，不在此处强制解析 components。
         */
        public static ItemIO of(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return new ItemIO(() -> ItemResource.EMPTY, 0);
            }
            ItemStack held = stack.copy();
            int count = held.getCount();
            return new ItemIO(() -> ItemResource.of(held), count);
        }

        /**
         * 由物品与数量创建。注册期只持有 {@link ItemLike} 引用，运行时再 {@link ItemResource#of}。
         */
        public static ItemIO of(@NonNull ItemLike item, int amount) {
            return new ItemIO(() -> ItemResource.of(item), amount);
        }

        /**
         * 结构校验（不解析 ItemResource，可在模组构造期安全调用）。
         */
        public boolean isValid() {
            return amount > 0;
        }

        /**
         * 运行时校验：结构合法且资源非空。
         */
        public boolean isResolvable() {
            return isValid() && !resource().isEmpty();
        }

        /** 按次数放大；溢出时返回 null。 */
        @Nullable
        public ItemIO scale(int count) {
            if (count <= 0 || !isValid()) return null;
            try {
                return new ItemIO(resourceSupplier, Math.multiplyExact(amount, count));
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
    }

    // ==============================================
    // 流体
    // ==============================================

    /**
     * 流体规格：惰性资源 + 数量。
     */
    public record FluidIO(@NonNull Supplier<FluidResource> resourceSupplier, int amount) {

        public FluidIO {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }

        /** 解析流体资源。 */
        public FluidResource resource() {
            FluidResource resource = resourceSupplier.get();
            return resource == null ? FluidResource.EMPTY : resource;
        }

        /** 由流体栈创建（栈副本 + 惰性 of）。 */
        public static FluidIO of(FluidStack stack) {
            if (stack == null || stack.isEmpty()) {
                return new FluidIO(() -> FluidResource.EMPTY, 0);
            }
            FluidStack held = stack.copy();
            int count = held.getAmount();
            return new FluidIO(() -> FluidResource.of(held), count);
        }

        /** 由流体与数量创建（惰性 of）。 */
        public static FluidIO of(@NonNull Fluid fluid, int amount) {
            return new FluidIO(() -> FluidResource.of(fluid), amount);
        }

        /** 结构校验（不解析 FluidResource）。 */
        public boolean isValid() {
            return amount > 0;
        }

        /** 运行时校验。 */
        public boolean isResolvable() {
            return isValid() && !resource().isEmpty();
        }

        /** 按次数放大；溢出时返回 null。 */
        @Nullable
        public FluidIO scale(int count) {
            if (count <= 0 || !isValid()) return null;
            try {
                return new FluidIO(resourceSupplier, Math.multiplyExact(amount, count));
            } catch (ArithmeticException ignored) {
                return null;
            }
        }
    }

    // ==============================================
    // 货币
    // ==============================================

    /**
     * 货币规格：资源 + BigInteger 数量。
     */
    public record CurrencyIO(CurrencyResource resource, BigInteger amount) {

        public CurrencyIO {
            resource = resource == null ? CurrencyResource.EMPTY : resource;
            amount = amount == null ? BigInteger.ZERO : amount;
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }

        /** 由货币资源与 long 数量创建。 */
        public static CurrencyIO of(CurrencyResource resource, long amount) {
            return new CurrencyIO(resource, BigInteger.valueOf(amount));
        }

        /** 由货币 ID 与 long 数量创建。 */
        public static CurrencyIO of(net.minecraft.resources.Identifier currencyTypeId, long amount) {
            return of(CurrencyResource.of(currencyTypeId), amount);
        }

        /** 规格是否有效。 */
        public boolean isValid() {
            return !resource.isEmpty() && amount.signum() > 0;
        }

        /** 按次数放大。 */
        public CurrencyIO scale(int count) {
            if (count <= 0 || !isValid()) {
                return new CurrencyIO(resource, BigInteger.ZERO);
            }
            return new CurrencyIO(resource, amount.multiply(BigInteger.valueOf(count)));
        }
    }

    // ==============================================
    // 放大后的完整 I/O 快照
    // ==============================================

    /**
     * 按实际交易次数放大后的全部 I/O，供 plan / execute 共用。
     */
    public record ScaledIO(
                           List<ItemIO> itemInputs,
                           List<ItemIO> itemOutputs,
                           List<FluidIO> fluidInputs,
                           List<FluidIO> fluidOutputs,
                           int energyExtract,
                           int energyInsert,
                           List<CurrencyIO> currencyExtract,
                           List<CurrencyIO> currencyInsert) {

        public ScaledIO {
            itemInputs = List.copyOf(Objects.requireNonNullElse(itemInputs, List.of()));
            itemOutputs = List.copyOf(Objects.requireNonNullElse(itemOutputs, List.of()));
            fluidInputs = List.copyOf(Objects.requireNonNullElse(fluidInputs, List.of()));
            fluidOutputs = List.copyOf(Objects.requireNonNullElse(fluidOutputs, List.of()));
            currencyExtract = List.copyOf(Objects.requireNonNullElse(currencyExtract, List.of()));
            currencyInsert = List.copyOf(Objects.requireNonNullElse(currencyInsert, List.of()));
            if (energyExtract < 0 || energyInsert < 0) {
                throw new IllegalArgumentException("energy amounts must be non-negative");
            }
        }

        /** 空快照。 */
        public static ScaledIO empty() {
            return new ScaledIO(List.of(), List.of(), List.of(), List.of(), 0, 0, List.of(), List.of());
        }

        /**
         * 将条目单次配方按 {@code count} 放大；溢出或非法返回 null。
         * 会解析物品/流体资源，须在 components 绑定后调用。
         */
        @Nullable
        public static ScaledIO scale(@NonNull MachineTrade trade, int count) {
            if (count <= 0) return null;

            List<ItemIO> itemIn = scaleItems(trade.itemInputs(), count);
            List<ItemIO> itemOut = scaleItems(trade.itemOutputs(), count);
            List<FluidIO> fluidIn = scaleFluids(trade.fluidInputs(), count);
            List<FluidIO> fluidOut = scaleFluids(trade.fluidOutputs(), count);
            if (itemIn == null || itemOut == null || fluidIn == null || fluidOut == null) {
                return null;
            }

            final long energyExtractLong;
            final long energyInsertLong;
            try {
                energyExtractLong = Math.multiplyExact(trade.energyExtract(), count);
                energyInsertLong = Math.multiplyExact(trade.energyInsert(), count);
            } catch (ArithmeticException ignored) {
                return null;
            }
            if (energyExtractLong > Integer.MAX_VALUE || energyInsertLong > Integer.MAX_VALUE) {
                return null;
            }

            List<CurrencyIO> currencyIn = new ArrayList<>(trade.currencyExtract().size());
            for (CurrencyIO io : trade.currencyExtract()) {
                if (!io.isValid()) return null;
                currencyIn.add(io.scale(count));
            }
            List<CurrencyIO> currencyOut = new ArrayList<>(trade.currencyInsert().size());
            for (CurrencyIO io : trade.currencyInsert()) {
                if (!io.isValid()) return null;
                currencyOut.add(io.scale(count));
            }

            return new ScaledIO(
                    itemIn,
                    itemOut,
                    fluidIn,
                    fluidOut,
                    (int) energyExtractLong,
                    (int) energyInsertLong,
                    currencyIn,
                    currencyOut);
        }

        @Nullable
        private static List<ItemIO> scaleItems(List<ItemIO> source, int count) {
            List<ItemIO> result = new ArrayList<>(source.size());
            for (ItemIO io : source) {
                if (!io.isResolvable()) return null;
                ItemIO scaled = io.scale(count);
                if (scaled == null) return null;
                result.add(scaled);
            }
            return result;
        }

        @Nullable
        private static List<FluidIO> scaleFluids(List<FluidIO> source, int count) {
            List<FluidIO> result = new ArrayList<>(source.size());
            for (FluidIO io : source) {
                if (!io.isResolvable()) return null;
                FluidIO scaled = io.scale(count);
                if (scaled == null) return null;
                result.add(scaled);
            }
            return result;
        }
    }
}

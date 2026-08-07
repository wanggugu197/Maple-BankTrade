package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

@UtilityClass
public class MachineTradeIO {

    // ==============================================
    // 物品 I/O
    // ==============================================

    @Getter
    @Setter(AccessLevel.PACKAGE)
    @Accessors(fluent = true)
    public static final class ItemIO implements IPersistedSerializable {

        @Persisted
        private ItemStack itemStack; // 仅作为类型标识，数量为1

        @Persisted
        private int amount; // 单次交易数量

        public ItemIO() {
            this(ItemStack.EMPTY, 0);
        }

        public ItemIO(@NonNull ItemStack itemStack, int amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            this.itemStack = itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copy();
            if (!this.itemStack.isEmpty()) {
                this.itemStack.setCount(1); // 只保留类型
            }
            this.amount = amount;
        }

        public static ItemIO of(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return new ItemIO(ItemStack.EMPTY, 0);
            }
            return new ItemIO(stack, stack.getCount());
        }

        public static ItemIO of(@NonNull ItemLike item, int amount) {
            return new ItemIO(new ItemStack(item, 1), amount);
        }

        public boolean isValid() {
            return amount > 0;
        }

        public boolean isResolvable() {
            return isValid() && !itemStack.isEmpty();
        }

        @Nullable
        public ItemIO scale(int count) {
            if (count <= 0 || !isValid()) return null;
            try {
                return new ItemIO(itemStack, Math.multiplyExact(amount, count));
            } catch (ArithmeticException ignored) {
                return null;
            }
        }

        public ItemStack toStack() {
            return itemStack.copyWithCount(amount);
        }

        public static final Codec<ItemIO> CODEC = PersistedParser.createCodec(ItemIO::new);

        @Override
        public void serialize(@NonNull ValueOutput output) {
            PersistedParser.serialize(this, output);
        }

        @Override
        public void deserialize(@NonNull ValueInput input) {
            PersistedParser.deserialize(this, input);
        }

        @Override
        public String toString() {
            return "ItemIO{itemStack=" + itemStack + ", amount=" + amount + "}";
        }
    }

    // ==============================================
    // 流体 I/O
    // ==============================================

    @Getter
    @Setter(AccessLevel.PACKAGE)
    @Accessors(fluent = true)
    public static final class FluidIO implements IPersistedSerializable {

        @Persisted
        private FluidStack fluidStack; // 仅作为类型标识，数量为1

        @Persisted
        private int amount; // 单次交易数量

        public FluidIO() {
            this(FluidStack.EMPTY, 0);
        }

        public FluidIO(@NonNull FluidStack fluidStack, int amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            this.fluidStack = fluidStack.isEmpty() ? FluidStack.EMPTY : fluidStack.copy();
            if (!this.fluidStack.isEmpty()) {
                this.fluidStack.setAmount(1); // 只保留类型
            }
            this.amount = amount;
        }

        public static FluidIO of(FluidStack stack) {
            if (stack == null || stack.isEmpty()) {
                return new FluidIO(FluidStack.EMPTY, 0);
            }
            return new FluidIO(stack, stack.getAmount());
        }

        public static FluidIO of(@NonNull Fluid fluid, int amount) {
            return new FluidIO(new FluidStack(fluid, 1), amount);
        }

        public boolean isValid() {
            return amount > 0;
        }

        public boolean isResolvable() {
            return isValid() && !fluidStack.isEmpty();
        }

        @Nullable
        public FluidIO scale(int count) {
            if (count <= 0 || !isValid()) return null;
            try {
                return new FluidIO(fluidStack, Math.multiplyExact(amount, count));
            } catch (ArithmeticException ignored) {
                return null;
            }
        }

        public FluidStack toStack() {
            return fluidStack.copyWithAmount(amount);
        }

        public static final Codec<FluidIO> CODEC = PersistedParser.createCodec(FluidIO::new);

        @Override
        public void serialize(@NonNull ValueOutput output) {
            PersistedParser.serialize(this, output);
        }

        @Override
        public void deserialize(@NonNull ValueInput input) {
            PersistedParser.deserialize(this, input);
        }

        @Override
        public String toString() {
            return "FluidIO{fluidStack=" + fluidStack + ", amount=" + amount + "}";
        }
    }

    // ==============================================
    // 货币 I/O（不变）
    // ==============================================

    @Getter
    @Setter(AccessLevel.PACKAGE)
    @Accessors(fluent = true)
    public static final class CurrencyIO implements IPersistedSerializable {

        @Persisted
        private CurrencyResource resource;

        @Persisted
        private BigInteger amount;

        public CurrencyIO() {
            this(CurrencyResource.EMPTY, BigInteger.ZERO);
        }

        public CurrencyIO(@NonNull CurrencyResource resource, @NonNull BigInteger amount) {
            this.resource = resource;
            this.amount = amount;
            if (this.amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
        }

        public static CurrencyIO of(CurrencyResource resource, BigInteger amount) {
            return new CurrencyIO(resource, amount);
        }

        public static CurrencyIO of(CurrencyResource resource, long amount) {
            return new CurrencyIO(resource, BigInteger.valueOf(amount));
        }

        public static CurrencyIO of(Identifier currencyTypeId, long amount) {
            return of(CurrencyResource.of(currencyTypeId), amount);
        }

        public boolean isValid() {
            return !resource.isEmpty() && amount.signum() > 0;
        }

        public CurrencyIO scale(int count) {
            if (count <= 0 || !isValid()) {
                return new CurrencyIO(resource, BigInteger.ZERO);
            }
            return new CurrencyIO(resource, amount.multiply(BigInteger.valueOf(count)));
        }

        public static final Codec<CurrencyIO> CODEC = PersistedParser.createCodec(CurrencyIO::new);

        @Override
        public void serialize(@NonNull ValueOutput output) {
            PersistedParser.serialize(this, output);
        }

        @Override
        public void deserialize(@NonNull ValueInput input) {
            PersistedParser.deserialize(this, input);
        }

        @Override
        public String toString() {
            return "CurrencyIO{resource=" + resource + ", amount=" + amount + "}";
        }
    }

    // ==============================================
    // 放大后的完整 I/O 快照
    // ==============================================

    @Getter
    @Setter(AccessLevel.PACKAGE)
    @Accessors(fluent = true)
    public static final class ScaledIO implements IPersistedSerializable {

        @Persisted
        private List<ItemIO> itemInputs;

        @Persisted
        private List<ItemIO> itemOutputs;

        @Persisted
        private List<FluidIO> fluidInputs;

        @Persisted
        private List<FluidIO> fluidOutputs;

        @Persisted
        private int energyExtract;

        @Persisted
        private int energyInsert;

        @Persisted
        private List<CurrencyIO> currencyExtract;

        @Persisted
        private List<CurrencyIO> currencyInsert;

        public ScaledIO() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0, 0, new ArrayList<>(), new ArrayList<>());
        }

        public ScaledIO(List<ItemIO> itemInputs, List<ItemIO> itemOutputs,
                        List<FluidIO> fluidInputs, List<FluidIO> fluidOutputs,
                        int energyExtract, int energyInsert,
                        List<CurrencyIO> currencyExtract, List<CurrencyIO> currencyInsert) {
            this.itemInputs = new ArrayList<>(Objects.requireNonNullElse(itemInputs, new ArrayList<>()));
            this.itemOutputs = new ArrayList<>(Objects.requireNonNullElse(itemOutputs, new ArrayList<>()));
            this.fluidInputs = new ArrayList<>(Objects.requireNonNullElse(fluidInputs, new ArrayList<>()));
            this.fluidOutputs = new ArrayList<>(Objects.requireNonNullElse(fluidOutputs, new ArrayList<>()));
            this.energyExtract = Math.max(0, energyExtract);
            this.energyInsert = Math.max(0, energyInsert);
            this.currencyExtract = new ArrayList<>(Objects.requireNonNullElse(currencyExtract, new ArrayList<>()));
            this.currencyInsert = new ArrayList<>(Objects.requireNonNullElse(currencyInsert, new ArrayList<>()));
        }

        public static ScaledIO empty() {
            return new ScaledIO();
        }

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
                    itemIn, itemOut,
                    fluidIn, fluidOut,
                    (int) energyExtractLong, (int) energyInsertLong,
                    currencyIn, currencyOut);
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

        public static final Codec<ScaledIO> CODEC = PersistedParser.createCodec(ScaledIO::new);

        @Override
        public void serialize(@NonNull ValueOutput output) {
            PersistedParser.serialize(this, output);
        }

        @Override
        public void deserialize(@NonNull ValueInput input) {
            PersistedParser.deserialize(this, input);
        }

        @Override
        public String toString() {
            return "ScaledIO{itemInputs=" + itemInputs.size() + ", itemOutputs=" + itemOutputs.size() +
                    ", fluidInputs=" + fluidInputs.size() + ", fluidOutputs=" + fluidOutputs.size() +
                    ", energyExtract=" + energyExtract + ", energyInsert=" + energyInsert +
                    ", currencyExtract=" + currencyExtract.size() + ", currencyInsert=" + currencyInsert.size() + "}";
        }
    }
}

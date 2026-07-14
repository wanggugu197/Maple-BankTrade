package com.maple.maple_banktrade.trade.machine;

import net.minecraft.network.chat.Component;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.maple.maple_banktrade.api.trade.registry.TradeInfo;
import com.maple.maple_banktrade.trade.machine.MachineTradeHooks.MachineTradeCheckHook;
import com.maple.maple_banktrade.trade.machine.MachineTradeHooks.MachineTradeSuccessHook;
import com.maple.maple_banktrade.trade.machine.MachineTradeHooks.MachineTradeVisibilityCheck;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ItemIO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * 机器多资源交易条目：单次配方 I/O + 可选展示信息 + 三个自定义钩子。
 */
@Getter
@Accessors(fluent = true)
public final class MachineTrade implements TradeInfo {

    // ==============================================
    // 字段
    // ==============================================

    private final List<ItemIO> itemInputs;
    private final List<ItemIO> itemOutputs;
    private final List<FluidIO> fluidInputs;
    private final List<FluidIO> fluidOutputs;
    private final long energyExtract;
    private final long energyInsert;
    private final List<CurrencyIO> currencyExtract;
    private final List<CurrencyIO> currencyInsert;
    /** 可选 UI 图标；未设置时为 {@link IGuiTexture#EMPTY}。 */
    private final IGuiTexture machineTradeIcon;
    /** 可选描述行；未设置时为空列表。 */
    private final List<Component> description;
    private final MachineTradeVisibilityCheck visibility;
    private final MachineTradeCheckHook extraCheck;
    private final MachineTradeSuccessHook afterSuccess;

    // ==============================================
    // 构造
    // ==============================================

    private MachineTrade(Builder builder) {
        this.itemInputs = List.copyOf(builder.itemInputs);
        this.itemOutputs = List.copyOf(builder.itemOutputs);
        this.fluidInputs = List.copyOf(builder.fluidInputs);
        this.fluidOutputs = List.copyOf(builder.fluidOutputs);
        this.energyExtract = builder.energyExtract;
        this.energyInsert = builder.energyInsert;
        this.currencyExtract = List.copyOf(builder.currencyExtract);
        this.currencyInsert = List.copyOf(builder.currencyInsert);
        this.machineTradeIcon = builder.machineTradeIcon == null ? IGuiTexture.EMPTY : builder.machineTradeIcon;
        this.description = builder.description == null ? List.of() : List.copyOf(builder.description);
        this.visibility = builder.visibility == null ? MachineTradeHooks.ALWAYS_VISIBLE : builder.visibility;
        this.extraCheck = builder.extraCheck == null ? MachineTradeHooks.PASS : builder.extraCheck;
        this.afterSuccess = builder.afterSuccess == null ? MachineTradeHooks.NOOP : builder.afterSuccess;
    }

    /** 创建构建器。 */
    public static Builder builder() {
        return new Builder();
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 是否设置了非空图标。 */
    public boolean hasIcon() {
        return machineTradeIcon != null && machineTradeIcon != IGuiTexture.EMPTY;
    }

    /** 是否有描述。 */
    public boolean hasDescription() {
        return !description.isEmpty();
    }

    // ==============================================
    // TradeInfo
    // ==============================================

    /** 至少一侧 I/O 有效，且数值合法。 */
    @Override
    public boolean isValid() {
        if (energyExtract < 0 || energyInsert < 0 || energyExtract > Integer.MAX_VALUE || energyInsert > Integer.MAX_VALUE) {
            return false;
        }
        return itemInputs.stream().allMatch(ItemIO::isValid) && itemOutputs.stream().allMatch(ItemIO::isValid) && fluidInputs.stream().allMatch(FluidIO::isValid) && fluidOutputs.stream().allMatch(FluidIO::isValid) && currencyExtract.stream().allMatch(CurrencyIO::isValid) && currencyInsert.stream().allMatch(CurrencyIO::isValid) && hasAnyIo();
    }

    private boolean hasAnyIo() {
        return !itemInputs.isEmpty() || !itemOutputs.isEmpty() || !fluidInputs.isEmpty() || !fluidOutputs.isEmpty() || energyExtract > 0 || energyInsert > 0 || !currencyExtract.isEmpty() || !currencyInsert.isEmpty();
    }

    // ==============================================
    // Builder
    // ==============================================

    /**
     * 机器交易条目构建器。
     */
    @Setter
    @Accessors(fluent = true, chain = true)
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Builder {

        private final List<ItemIO> itemInputs = new ArrayList<>();
        private final List<ItemIO> itemOutputs = new ArrayList<>();
        private final List<FluidIO> fluidInputs = new ArrayList<>();
        private final List<FluidIO> fluidOutputs = new ArrayList<>();
        @Setter(AccessLevel.NONE)
        private long energyExtract;
        @Setter(AccessLevel.NONE)
        private long energyInsert;
        private final List<CurrencyIO> currencyExtract = new ArrayList<>();
        private final List<CurrencyIO> currencyInsert = new ArrayList<>();
        @Nullable
        private IGuiTexture machineTradeIcon;
        @Nullable
        private List<Component> description;
        private MachineTradeVisibilityCheck visibility;
        private MachineTradeCheckHook extraCheck;
        private MachineTradeSuccessHook afterSuccess;

        public Builder addItemInput(ItemIO io) {
            itemInputs.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        public Builder addItemOutput(ItemIO io) {
            itemOutputs.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        public Builder addFluidInput(FluidIO io) {
            fluidInputs.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        public Builder addFluidOutput(FluidIO io) {
            fluidOutputs.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        public Builder energyExtract(long amount) {
            if (amount < 0) throw new IllegalArgumentException("energyExtract must be non-negative");
            this.energyExtract = amount;
            return this;
        }

        public Builder energyInsert(long amount) {
            if (amount < 0) throw new IllegalArgumentException("energyInsert must be non-negative");
            this.energyInsert = amount;
            return this;
        }

        public Builder addCurrencyExtract(CurrencyIO io) {
            currencyExtract.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        public Builder addCurrencyInsert(CurrencyIO io) {
            currencyInsert.add(Objects.requireNonNull(io, "io"));
            return this;
        }

        /** 追加一行描述。 */
        public Builder addDescription(Component line) {
            Objects.requireNonNull(line, "line");
            if (this.description == null) {
                this.description = new ArrayList<>();
            } else if (!(this.description instanceof ArrayList)) {
                this.description = new ArrayList<>(this.description);
            }
            this.description.add(line);
            return this;
        }

        /** 构建条目；无效时抛异常。 */
        public MachineTrade build() {
            MachineTrade trade = new MachineTrade(this);
            if (!trade.isValid()) {
                throw new IllegalStateException("Invalid MachineTrade: empty or illegal I/O");
            }
            return trade;
        }
    }
}

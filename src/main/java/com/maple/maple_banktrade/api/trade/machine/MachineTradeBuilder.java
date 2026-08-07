package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public final class MachineTradeBuilder {

    private final List<MachineTradeIO.ItemIO> itemInputs = new ArrayList<>();
    private final List<MachineTradeIO.ItemIO> itemOutputs = new ArrayList<>();
    private final List<MachineTradeIO.FluidIO> fluidInputs = new ArrayList<>();
    private final List<MachineTradeIO.FluidIO> fluidOutputs = new ArrayList<>();
    private long energyExtract;
    private long energyInsert;
    private final List<MachineTradeIO.CurrencyIO> currencyExtract = new ArrayList<>();
    private final List<MachineTradeIO.CurrencyIO> currencyInsert = new ArrayList<>();
    @Nullable
    private IGuiTexture machineTradeIcon;
    @Nullable
    private List<Component> description;
    private boolean autoTrade;

    // 钩子 ID 与配置（CompoundTag）
    @Nullable
    private Identifier visibilityHookId;
    @Nullable
    private CompoundTag visibilityConfig;
    @Nullable
    private Identifier checkHookId = MachineTradeHookRegistry.DEFAULT_CHECK;
    @Nullable
    private CompoundTag checkConfig;
    @Nullable
    private Identifier successHookId = MachineTradeHookRegistry.DEFAULT_SUCCESS;
    @Nullable
    private CompoundTag successConfig;

    public MachineTradeBuilder addItemInput(MachineTradeIO.ItemIO io) {
        itemInputs.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder addItemOutput(MachineTradeIO.ItemIO io) {
        itemOutputs.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder addFluidInput(MachineTradeIO.FluidIO io) {
        fluidInputs.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder addFluidOutput(MachineTradeIO.FluidIO io) {
        fluidOutputs.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder energyExtract(long amount) {
        if (amount < 0) throw new IllegalArgumentException("energyExtract must be non-negative");
        this.energyExtract = amount;
        return this;
    }

    public MachineTradeBuilder energyInsert(long amount) {
        if (amount < 0) throw new IllegalArgumentException("energyInsert must be non-negative");
        this.energyInsert = amount;
        return this;
    }

    public MachineTradeBuilder addCurrencyExtract(MachineTradeIO.CurrencyIO io) {
        currencyExtract.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder addCurrencyInsert(MachineTradeIO.CurrencyIO io) {
        currencyInsert.add(Objects.requireNonNull(io, "io"));
        return this;
    }

    public MachineTradeBuilder addDescription(Component line) {
        Objects.requireNonNull(line, "line");
        if (this.description == null) {
            this.description = new ArrayList<>();
        } else if (!(this.description instanceof ArrayList)) {
            this.description = new ArrayList<>(this.description);
        }
        this.description.add(line);
        return this;
    }

    // ----- 钩子设置方法（使用 CompoundTag） -----
    public MachineTradeBuilder visibility(Identifier hookId, CompoundTag config) {
        this.visibilityHookId = Objects.requireNonNull(hookId, "hookId");
        this.visibilityConfig = config;
        return this;
    }

    public MachineTradeBuilder extraCheck(Identifier hookId, CompoundTag config) {
        this.checkHookId = Objects.requireNonNull(hookId, "hookId");
        this.checkConfig = config;
        return this;
    }

    public MachineTradeBuilder afterSuccess(Identifier hookId, CompoundTag config) {
        this.successHookId = Objects.requireNonNull(hookId, "hookId");
        this.successConfig = config;
        return this;
    }

    // 便捷方法：使用默认配置（空 CompoundTag）
    public MachineTradeBuilder visibility(Identifier hookId) {
        return visibility(hookId, null);
    }

    public MachineTradeBuilder extraCheck(Identifier hookId) {
        return extraCheck(hookId, null);
    }

    public MachineTradeBuilder afterSuccess(Identifier hookId) {
        return afterSuccess(hookId, null);
    }

    public MachineTrade build() {
        MachineTrade trade = new MachineTrade();
        trade.itemInputs(new ArrayList<>(itemInputs));
        trade.itemOutputs(new ArrayList<>(itemOutputs));
        trade.fluidInputs(new ArrayList<>(fluidInputs));
        trade.fluidOutputs(new ArrayList<>(fluidOutputs));
        trade.energyExtract(energyExtract);
        trade.energyInsert(energyInsert);
        trade.currencyExtract(new ArrayList<>(currencyExtract));
        trade.currencyInsert(new ArrayList<>(currencyInsert));
        trade.autoTrade(autoTrade);
        trade.machineTradeIcon(machineTradeIcon == null ? IGuiTexture.EMPTY : machineTradeIcon);
        trade.description(description == null ? new ArrayList<>() : new ArrayList<>(description));
        trade.visibilityHookId(visibilityHookId == null ? MachineTradeHookRegistry.DEFAULT_VISIBILITY : visibilityHookId);
        trade.visibilityConfig(visibilityConfig == null ? new CompoundTag() : visibilityConfig);
        trade.checkHookId(checkHookId == null ? MachineTradeHookRegistry.DEFAULT_CHECK : checkHookId);
        trade.checkConfig(checkConfig == null ? new CompoundTag() : checkConfig);
        trade.successHookId(successHookId == null ? MachineTradeHookRegistry.DEFAULT_SUCCESS : successHookId);
        trade.successConfig(successConfig == null ? new CompoundTag() : successConfig);

        if (trade.autoTrade() && !trade.hasValidAutoTradeInputs()) {
            throw new IllegalStateException(
                    "Invalid MachineTrade: autoTrade requires itemInputs.size() + fluidInputs.size() == 1" +
                            " (got items=" + trade.itemInputs().size() + ", fluids=" + trade.fluidInputs().size() + ")");
        }
        if (!trade.isValid()) {
            throw new IllegalStateException("Invalid MachineTrade: empty or illegal I/O");
        }
        return trade;
    }
}

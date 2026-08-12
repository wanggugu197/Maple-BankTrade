package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class MachineTradeBuilder {

    final Identifier id;

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
    private MachineTradeHooks.VisibilityHook visibilityHook;
    private MachineTradeHooks.CheckHook checkHook;
    private MachineTradeHooks.SuccessHook successHook;

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

    public MachineTrade build() {
        MachineTrade trade = new MachineTrade(
                id,
                itemInputs, itemOutputs, fluidInputs, fluidOutputs,
                energyExtract, energyInsert, currencyExtract, currencyInsert,
                autoTrade,
                machineTradeIcon == null ? IGuiTexture.EMPTY : machineTradeIcon,
                description == null ? new ArrayList<>() : new ArrayList<>(description),
                visibilityHook == null ? new MachineTradeHooks.AlwaysVisibleHook() : visibilityHook,
                checkHook == null ? new MachineTradeHooks.PassCheckHook() : checkHook,
                successHook == null ? new MachineTradeHooks.NoopSuccessHook() : successHook);

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

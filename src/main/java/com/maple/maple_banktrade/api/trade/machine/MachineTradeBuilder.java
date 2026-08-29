package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@Setter
@Getter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public final class MachineTradeBuilder {

    final ResourceLocation id;

    private final List<MachineTradeIO.ItemIO> itemInputs = new ArrayList<>();
    private final List<MachineTradeIO.ItemIO> itemOutputs = new ArrayList<>();
    private final List<MachineTradeIO.FluidIO> fluidInputs = new ArrayList<>();
    private final List<MachineTradeIO.FluidIO> fluidOutputs = new ArrayList<>();
    private long energyExtract;
    private long energyInsert;
    private final List<MachineTradeIO.CurrencyIO> currencyExtract = new ArrayList<>();
    private final List<MachineTradeIO.CurrencyIO> currencyInsert = new ArrayList<>();
    @Nullable
    private ResourceLocation machineTradeIcon;
    private List<List<Component>> description = new ArrayList<>();
    private boolean autoTrade;
    private MachineTradeHooks.StateHook stateHook = new MachineTradeHooks.AlwaysVisibleStateHook();
    private MachineTradeHooks.CheckHook checkHook = new MachineTradeHooks.PassCheckHook();
    private MachineTradeHooks.SuccessHook successHook = new MachineTradeHooks.NoopSuccessHook();

    public MachineTradeBuilder addItemInput(ItemStack stack) {
        itemInputs.add(MachineTradeIO.ItemIO.of(stack));
        return this;
    }

    public MachineTradeBuilder addItemInput(ItemLike item, int amount) {
        itemInputs.add(MachineTradeIO.ItemIO.of(item, amount));
        return this;
    }

    public MachineTradeBuilder addItemOutput(ItemStack stack) {
        itemOutputs.add(MachineTradeIO.ItemIO.of(stack));
        return this;
    }

    public MachineTradeBuilder addItemOutput(ItemLike item, int amount) {
        itemOutputs.add(MachineTradeIO.ItemIO.of(item, amount));
        return this;
    }

    public MachineTradeBuilder addFluidInput(FluidStack stack) {
        fluidInputs.add(MachineTradeIO.FluidIO.of(stack));
        return this;
    }

    public MachineTradeBuilder addFluidInput(Fluid fluid, int amount) {
        fluidInputs.add(MachineTradeIO.FluidIO.of(fluid, amount));
        return this;
    }

    public MachineTradeBuilder addFluidOutput(FluidStack stack) {
        fluidOutputs.add(MachineTradeIO.FluidIO.of(stack));
        return this;
    }

    public MachineTradeBuilder addFluidOutput(Fluid fluid, int amount) {
        fluidOutputs.add(MachineTradeIO.FluidIO.of(fluid, amount));
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

    public MachineTradeBuilder addCurrencyExtract(CurrencyResource resource, BigInteger amount) {
        currencyExtract.add(MachineTradeIO.CurrencyIO.of(resource, amount));
        return this;
    }

    public MachineTradeBuilder addCurrencyExtract(CurrencyResource resource, long amount) {
        currencyExtract.add(MachineTradeIO.CurrencyIO.of(resource, amount));
        return this;
    }

    public MachineTradeBuilder addCurrencyInsert(CurrencyResource resource, BigInteger amount) {
        currencyInsert.add(MachineTradeIO.CurrencyIO.of(resource, amount));
        return this;
    }

    public MachineTradeBuilder addCurrencyInsert(CurrencyResource resource, long amount) {
        currencyInsert.add(MachineTradeIO.CurrencyIO.of(resource, amount));
        return this;
    }

    public MachineTradeBuilder addDescriptionCommon(Component line) {
        return addDescription(0, line);
    }

    public MachineTradeBuilder addDescriptionCommon(List<Component> lines) {
        return addDescription(0, lines);
    }

    public MachineTradeBuilder addDescriptionVisible(Component line) {
        return addDescription(1, line);
    }

    public MachineTradeBuilder addDescriptionVisible(List<Component> lines) {
        return addDescription(1, lines);
    }

    public MachineTradeBuilder addDescriptionInvisible(Component line) {
        return addDescription(2, line);
    }

    public MachineTradeBuilder addDescriptionInvisible(List<Component> lines) {
        return addDescription(2, lines);
    }

    public MachineTradeBuilder addDescriptionIncomplete(Component line) {
        return addDescription(3, line);
    }

    public MachineTradeBuilder addDescriptionIncomplete(List<Component> lines) {
        return addDescription(3, lines);
    }

    public MachineTradeBuilder addDescriptionComplete(Component line) {
        return addDescription(4, line);
    }

    public MachineTradeBuilder addDescriptionComplete(List<Component> lines) {
        return addDescription(4, lines);
    }

    private void ensureIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Index must be >= 0");
        while (description.size() <= index) description.add(new ArrayList<>());
    }

    public MachineTradeBuilder addDescription(int index, Component line) {
        ensureIndex(index);
        description.get(index).add(line);
        return this;
    }

    public MachineTradeBuilder addDescription(int index, List<Component> lines) {
        ensureIndex(index);
        description.get(index).addAll(lines);
        return this;
    }

    public MachineTrade build() {
        MachineTrade trade = new MachineTrade(
                id,
                new ArrayList<>(itemInputs),
                new ArrayList<>(itemOutputs),
                new ArrayList<>(fluidInputs),
                new ArrayList<>(fluidOutputs),
                energyExtract,
                energyInsert,
                new ArrayList<>(currencyExtract),
                new ArrayList<>(currencyInsert),
                autoTrade,
                machineTradeIcon,
                new ArrayList<>(description),
                stateHook,
                checkHook,
                successHook);

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

package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeInfo;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器多资源交易条目：单次配方 I/O + 可选展示信息 + 三个自定义钩子（通过注册表 ID + 配置引用）。
 * <p>
 * 实现 {@link IPersistedSerializable} 以支持 LDLib2 持久化与同步。
 * </p>
 */
@Getter
@Setter(value = AccessLevel.PACKAGE)
@Accessors(fluent = true)
@EqualsAndHashCode
public final class MachineTrade implements TradeInfo {

    // ==============================================
    // 持久化字段
    // ==============================================

    @Persisted
    private List<ItemIO> itemInputs;

    @Persisted
    private List<ItemIO> itemOutputs;

    @Persisted
    private List<FluidIO> fluidInputs;

    @Persisted
    private List<FluidIO> fluidOutputs;

    @Persisted
    private long energyExtract;

    @Persisted
    private long energyInsert;

    @Persisted
    private List<CurrencyIO> currencyExtract;

    @Persisted
    private List<CurrencyIO> currencyInsert;

    @Persisted
    private boolean autoTrade;

    private IGuiTexture machineTradeIcon;

    @Persisted
    private List<Component> description;

    // 钩子 ID 与配置（配置为 CompoundTag，工厂自行解析）
    @Persisted
    private Identifier visibilityHookId;

    @Persisted
    private CompoundTag visibilityConfig;

    @Persisted
    private Identifier checkHookId;

    @Persisted
    private CompoundTag checkConfig;

    @Persisted
    private Identifier successHookId;

    @Persisted
    private CompoundTag successConfig;

    // ==============================================
    // 缓存
    // ==============================================

    @Setter(AccessLevel.NONE)
    private transient volatile MachineTradeHooks.MachineTradeVisibilityCheck cachedVisibility;
    @Setter(AccessLevel.NONE)
    private transient volatile MachineTradeHooks.MachineTradeCheckHook cachedCheck;
    @Setter(AccessLevel.NONE)
    private transient volatile MachineTradeHooks.MachineTradeSuccessHook cachedSuccess;

    // ==============================================
    // 构造器
    // ==============================================

    /** 无参构造器（供 LDLib2 反序列化使用） */
    public MachineTrade() {
        this.itemInputs = new ArrayList<>();
        this.itemOutputs = new ArrayList<>();
        this.fluidInputs = new ArrayList<>();
        this.fluidOutputs = new ArrayList<>();
        this.energyExtract = 0;
        this.energyInsert = 0;
        this.currencyExtract = new ArrayList<>();
        this.currencyInsert = new ArrayList<>();
        this.autoTrade = false;
        this.machineTradeIcon = IGuiTexture.EMPTY;
        this.description = new ArrayList<>();
        this.visibilityHookId = MachineTradeHookRegistry.DEFAULT_VISIBILITY;
        this.visibilityConfig = new CompoundTag();
        this.checkHookId = MachineTradeHookRegistry.DEFAULT_CHECK;
        this.checkConfig = new CompoundTag();
        this.successHookId = MachineTradeHookRegistry.DEFAULT_SUCCESS;
        this.successConfig = new CompoundTag();
    }

    // ==============================================
    // 业务方法
    // ==============================================

    public boolean hasIcon() {
        return machineTradeIcon != null && machineTradeIcon != IGuiTexture.EMPTY;
    }

    public boolean hasDescription() {
        return !description.isEmpty();
    }

    public boolean hasValidAutoTradeInputs() {
        return itemInputs.size() + fluidInputs.size() == 1;
    }

    @Override
    public boolean isValid() {
        if (energyExtract < 0 || energyInsert < 0 || energyExtract > Integer.MAX_VALUE || energyInsert > Integer.MAX_VALUE) {
            return false;
        }
        if (!itemInputs.stream().allMatch(ItemIO::isValid) ||
                !itemOutputs.stream().allMatch(ItemIO::isValid) ||
                !fluidInputs.stream().allMatch(FluidIO::isValid) ||
                !fluidOutputs.stream().allMatch(FluidIO::isValid) ||
                !currencyExtract.stream().allMatch(CurrencyIO::isValid) ||
                !currencyInsert.stream().allMatch(CurrencyIO::isValid) ||
                !hasAnyIo()) {
            return false;
        }
        return !autoTrade || hasValidAutoTradeInputs();
    }

    private boolean hasAnyIo() {
        return !itemInputs.isEmpty() || !itemOutputs.isEmpty() ||
                !fluidInputs.isEmpty() || !fluidOutputs.isEmpty() ||
                energyExtract > 0 || energyInsert > 0 ||
                !currencyExtract.isEmpty() || !currencyInsert.isEmpty();
    }

    // ==============================================
    // 钩子缓存
    // ==============================================

    public MachineTradeHooks.MachineTradeVisibilityCheck getVisibilityHook() {
        if (cachedVisibility == null) {
            synchronized (this) {
                if (cachedVisibility == null) {
                    cachedVisibility = MachineTradeHookRegistry.getVisibility(visibilityHookId, visibilityConfig);
                }
            }
        }
        return cachedVisibility;
    }

    public MachineTradeHooks.MachineTradeCheckHook getCheckHook() {
        if (cachedCheck == null) {
            synchronized (this) {
                if (cachedCheck == null) {
                    cachedCheck = MachineTradeHookRegistry.getCheck(checkHookId, checkConfig);
                }
            }
        }
        return cachedCheck;
    }

    public MachineTradeHooks.MachineTradeSuccessHook getSuccessHook() {
        if (cachedSuccess == null) {
            synchronized (this) {
                if (cachedSuccess == null) {
                    cachedSuccess = MachineTradeHookRegistry.getSuccess(successHookId, successConfig);
                }
            }
        }
        return cachedSuccess;
    }

    // ==============================================
    // UI 构造
    // ==============================================

    public static UIElement getMachineTradeIcon(MachineTrade trade) {
        var base = new UIElement()
                .layout(l -> l
                        .width(26).height(26)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER))
                .style(s -> s
                        .background(Sprites.PROGRESS_CONTAINER));

        var icon = new UIElement()
                .layout(l -> l.width(18).height(18));

        List<Component> tooltipLines = new ArrayList<>();

        if (trade.description != null) {
            tooltipLines.addAll(trade.description);
        }

        boolean hasInputs = !trade.itemInputs.isEmpty() || !trade.fluidInputs.isEmpty() || trade.energyExtract > 0 || !trade.currencyExtract.isEmpty();
        boolean hasOutputs = !trade.itemOutputs.isEmpty() || !trade.fluidOutputs.isEmpty() || trade.energyInsert > 0 || !trade.currencyInsert.isEmpty();

        if (!tooltipLines.isEmpty() && (hasInputs || hasOutputs)) {
            tooltipLines.add(Component.empty());
        }

        if (hasInputs) {
            tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.inputs")
                    .withStyle(ChatFormatting.YELLOW));

            for (ItemIO io : trade.itemInputs) {
                ItemStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (FluidIO io : trade.fluidInputs) {
                FluidStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (trade.energyExtract > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_extract", trade.energyExtract)
                        .withStyle(ChatFormatting.GOLD));
            }
            for (CurrencyIO io : trade.currencyExtract) {
                if (io.isValid()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.currency_entry", io.amount().toString())
                            .append(io.resource().type().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        if (hasOutputs) {
            if (hasInputs) {
                tooltipLines.add(Component.empty());
            }
            tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.outputs")
                    .withStyle(ChatFormatting.GREEN));

            for (ItemIO io : trade.itemOutputs) {
                ItemStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (FluidIO io : trade.fluidOutputs) {
                FluidStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (trade.energyInsert > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_insert", trade.energyInsert)
                        .withStyle(ChatFormatting.GOLD));
            }
            for (CurrencyIO io : trade.currencyInsert) {
                if (io.isValid()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.currency_entry", io.amount().toString())
                            .append(io.resource().type().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        icon.style(s -> s.tooltips(tooltipLines.toArray(new Component[0])));

        if (trade.machineTradeIcon != IGuiTexture.EMPTY) {
            icon.style(s -> s.background(trade.machineTradeIcon));
        } else {
            if (!trade.itemOutputs.isEmpty()) {
                ItemStack[] itemOuts = trade.itemOutputs.stream()
                        .map(ItemIO::toStack)
                        .filter(stack -> !stack.isEmpty())
                        .toArray(ItemStack[]::new);
                icon.style(s -> s.background(new ItemStackTexture(itemOuts)));
            } else if (!trade.itemInputs.isEmpty()) {
                ItemStack[] itemIns = trade.itemInputs.stream()
                        .map(ItemIO::toStack)
                        .filter(stack -> !stack.isEmpty())
                        .toArray(ItemStack[]::new);
                icon.style(s -> s.background(new ItemStackTexture(itemIns)));
            } else if (!trade.fluidOutputs.isEmpty()) {
                FluidStack[] fluidOuts = trade.fluidOutputs.stream()
                        .map(FluidIO::toStack)
                        .filter(stack -> !stack.isEmpty())
                        .toArray(FluidStack[]::new);
                icon.style(s -> s.background(new FluidStackTexture(fluidOuts)));
            } else if (!trade.fluidInputs.isEmpty()) {
                FluidStack[] fluidIns = trade.fluidInputs.stream()
                        .map(FluidIO::toStack)
                        .filter(stack -> !stack.isEmpty())
                        .toArray(FluidStack[]::new);
                icon.style(s -> s.background(new FluidStackTexture(fluidIns)));
            } else {
                icon.style(s -> s.background(SpriteTexture.of(MapleBankTrade.id("textures/item/leaf.png"))));
            }
        }

        base.addChild(icon);
        return base;
    }

    // ==============================================
    // Builder 工厂方法
    // ==============================================

    public static MachineTradeBuilder builder() {
        return new MachineTradeBuilder();
    }
}

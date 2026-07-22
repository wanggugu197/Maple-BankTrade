package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import com.lowdragmc.lowdraglib2.gui.texture.FluidStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeInfo;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.MachineTradeCheckHook;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.MachineTradeSuccessHook;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.MachineTradeVisibilityCheck;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
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
    /**
     * 是否参与自动交易匹配。
     * <p>
     * 为 true 时要求 {@code itemInputs.size() + fluidInputs.size() == 1}（恰好一种物或流输入），
     * 且所在 {@link MachineTradeType#allowAutoTrade()} 必须为 true。
     * </p>
     */
    private final boolean autoTrade;
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
        this.autoTrade = builder.autoTrade;
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

    /**
     * 自动交易输入是否合法：恰好一个物品输入或一个流体输入（二者数量之和为 1）。
     */
    public boolean hasValidAutoTradeInputs() {
        return itemInputs.size() + fluidInputs.size() == 1;
    }

    // ==============================================
    // TradeInfo
    // ==============================================

    /** 至少一侧 I/O 有效，且数值合法；autoTrade 时另要求单输入。 */
    @Override
    public boolean isValid() {
        if (energyExtract < 0 || energyInsert < 0 || energyExtract > Integer.MAX_VALUE || energyInsert > Integer.MAX_VALUE) {
            return false;
        }
        if (!itemInputs.stream().allMatch(ItemIO::isValid) || !itemOutputs.stream().allMatch(ItemIO::isValid) || !fluidInputs.stream().allMatch(FluidIO::isValid) || !fluidOutputs.stream().allMatch(FluidIO::isValid) || !currencyExtract.stream().allMatch(CurrencyIO::isValid) || !currencyInsert.stream().allMatch(CurrencyIO::isValid) || !hasAnyIo()) {
            return false;
        }
        if (autoTrade && !hasValidAutoTradeInputs()) {
            return false;
        }
        return true;
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
        /** 默认 false；为 true 时 build 要求 item+fluid 输入数量之和为 1。 */
        private boolean autoTrade;

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

        /** 构建条目；无效时抛异常。autoTrade 时强制单输入约束。 */
        public MachineTrade build() {
            MachineTrade trade = new MachineTrade(this);
            if (trade.autoTrade() && !trade.hasValidAutoTradeInputs()) {
                throw new IllegalStateException(
                        "Invalid MachineTrade: autoTrade requires itemInputs.size() + fluidInputs.size() == 1" + " (got items=" + trade.itemInputs().size() + ", fluids=" + trade.fluidInputs().size() + ")");
            }
            if (!trade.isValid()) {
                throw new IllegalStateException("Invalid MachineTrade: empty or illegal I/O");
            }
            return trade;
        }
    }

    // ==============================================
    // UI构造
    // ==============================================

    public UIElement getMachineTradeIcon() {
        var base = new UIElement()
                .layout(l -> l
                        .width(26).height(26)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER))
                .style(s -> s
                        .background(Sprites.PROGRESS_CONTAINER));

        var icon = new UIElement()
                .layout(l -> l.width(18).height(18));

        // ==============================================
        // 动态构建 Tooltip 链：Description -> 输入 -> 输出
        // ==============================================
        List<Component> tooltipLines = new ArrayList<>();

        // 1. 优先放入自定义描述
        if (description != null) {
            tooltipLines.addAll(description);
        }

        boolean hasInputs = !itemInputs.isEmpty() || !fluidInputs.isEmpty() || energyExtract > 0 || !currencyExtract.isEmpty();
        boolean hasOutputs = !itemOutputs.isEmpty() || !fluidOutputs.isEmpty() || energyInsert > 0 || !currencyInsert.isEmpty();

        // 如果存在描述，且后续有输入或输出，追加一个空行做视觉隔离
        if (!tooltipLines.isEmpty() && (hasInputs || hasOutputs)) {
            tooltipLines.add(Component.empty());
        }

        // 2. 整理并添加【输入】信息
        if (hasInputs) {
            tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.inputs")
                    .withStyle(ChatFormatting.YELLOW));

            for (ItemIO io : itemInputs) {
                ItemStack temp = io.resource().toStack(io.amount());
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (FluidIO io : fluidInputs) {
                FluidStack temp = io.resource().toStack(io.amount());
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (energyExtract > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_extract", energyExtract)
                        .withStyle(ChatFormatting.GOLD));
            }
            for (CurrencyIO io : currencyExtract) {
                if (io.isValid()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.currency_entry", io.amount().toString())
                            .append(io.resource().type().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        // 3. 整理并添加【输出】信息
        if (hasOutputs) {
            if (hasInputs) {
                tooltipLines.add(Component.empty());
            }
            tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.outputs")
                    .withStyle(ChatFormatting.GREEN));

            for (ItemIO io : itemOutputs) {
                ItemStack temp = io.resource().toStack(io.amount());
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (FluidIO io : fluidOutputs) {
                FluidStack temp = io.resource().toStack(io.amount());
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (energyInsert > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_insert", energyInsert)
                        .withStyle(ChatFormatting.GOLD));
            }
            for (CurrencyIO io : currencyInsert) {
                if (io.isValid()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.currency_entry", io.amount().toString())
                            .append(io.resource().type().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        // 应用最终生成的 Tooltip 数组
        icon.style(s -> s.tooltips(tooltipLines.toArray(new Component[0])));

        // ==============================================
        // 渲染背景图（保持你原有的动态材质匹配逻辑）
        // ==============================================
        if (machineTradeIcon != IGuiTexture.EMPTY) {
            icon.style(s -> s.background(machineTradeIcon));
        } else {
            if (!itemOutputs.isEmpty()) {
                ItemStack[] itemOuts = itemOutputs.stream()
                        .map(io -> io.resource().toStack(io.amount()))
                        .filter(stack -> !stack.isEmpty())
                        .toArray(ItemStack[]::new);
                icon.style(s -> s.background(new ItemStackTexture(itemOuts)));
            } else if (!itemInputs.isEmpty()) {
                ItemStack[] itemIns = itemInputs.stream()
                        .map(io -> io.resource().toStack(io.amount()))
                        .filter(stack -> !stack.isEmpty())
                        .toArray(ItemStack[]::new);
                icon.style(s -> s.background(new ItemStackTexture(itemIns)));
            } else if (!fluidOutputs.isEmpty()) {
                FluidStack[] fluidOuts = fluidOutputs.stream()
                        .map(io -> io.resource().toStack(io.amount()))
                        .filter(stack -> !stack.isEmpty())
                        .toArray(FluidStack[]::new);
                icon.style(s -> s.background(new FluidStackTexture(fluidOuts)));
            } else if (!fluidInputs.isEmpty()) {
                FluidStack[] fluidIns = fluidInputs.stream()
                        .map(io -> io.resource().toStack(io.amount()))
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
}

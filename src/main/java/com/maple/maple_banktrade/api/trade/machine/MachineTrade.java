package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.ChatFormatting;
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
import lombok.*;
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
@Accessors(fluent = true)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class MachineTrade implements TradeInfo {

    // ==============================================
    // 持久化字段
    // ==============================================

    @Persisted
    private Identifier id;

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

    @Persisted
    private Identifier machineTradeIcon;
    @Persisted
    private List<Component> descriptionVisible;
    @Persisted
    private List<Component> descriptionInvisible;

    @Persisted
    private MachineTradeHooks.VisibilityHook visibilityHook;
    @Persisted
    private MachineTradeHooks.CheckHook checkHook;
    @Persisted
    private MachineTradeHooks.SuccessHook successHook;

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
        this.machineTradeIcon = null;
        this.descriptionVisible = new ArrayList<>();
        this.descriptionInvisible = new ArrayList<>();
        this.visibilityHook = new MachineTradeHooks.AlwaysVisibleHook();
        this.checkHook = new MachineTradeHooks.PassCheckHook();
        this.successHook = new MachineTradeHooks.NoopSuccessHook();
    }

    // ==============================================
    // 业务方法
    // ==============================================
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
    // UI 构造
    // ==============================================

    public static UIElement getMachineTradeIcon(MachineTrade trade) {
        var base = new UIElement()
                .setId(trade.id().toString())
                .layout(l -> l
                        .width(26).height(26)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.CENTER))
                .style(s -> s
                        .background(Sprites.PROGRESS_CONTAINER));

        var icon = new UIElement()
                .layout(l -> l.width(18).height(18))
                .style(s -> s.background(determineIconTexture(trade)));

        base.addChild(icon);
        return base;
    }

    public static void setMachineTradeInvisible(MachineTrade trade, UIElement uiElement) {
        uiElement.style(s -> s.overlay(Sprites.RECT_RD));
        uiElement.getChildren().getFirst().style(s -> s.tooltips(buildTooltipLinesInvisible(trade).toArray(new Component[0])));
    }

    public static void setMachineTradeVisible(MachineTrade trade, UIElement uiElement) {
        uiElement.style(s -> s.overlay(IGuiTexture.EMPTY));
        uiElement.getChildren().getFirst().style(s -> s.tooltips(buildTooltipLinesVisible(trade).toArray(new Component[0])));
    }

    /** 根据交易信息生成可见时文本列表 */
    private static List<Component> buildTooltipLinesVisible(MachineTrade trade) {
        List<Component> tooltipLines = new ArrayList<>();

        if (trade.descriptionVisible != null) {
            tooltipLines.addAll(trade.descriptionVisible);
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

        return tooltipLines;
    }

    /** 根据交易信息生成不可见时文本列表 */
    private static List<Component> buildTooltipLinesInvisible(MachineTrade trade) {
        List<Component> tooltipLines = new ArrayList<>();
        tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.unlocked").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        if (trade.descriptionInvisible != null) {
            tooltipLines.addAll(trade.descriptionInvisible);
        }
        return tooltipLines;
    }

    /** 根据交易信息决定图标背景纹理 */
    private static IGuiTexture determineIconTexture(MachineTrade trade) {
        // 优先使用自定义纹理
        if (trade.machineTradeIcon != null) {
            return SpriteTexture.of(trade.machineTradeIcon);
        }

        // 按优先级：输出物品 → 输入物品 → 输出流体 → 输入流体 → 默认叶子图标
        if (!trade.itemOutputs.isEmpty()) {
            ItemStack[] itemOuts = trade.itemOutputs.stream()
                    .map(ItemIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(ItemStack[]::new);
            return new ItemStackTexture(itemOuts);
        } else if (!trade.itemInputs.isEmpty()) {
            ItemStack[] itemIns = trade.itemInputs.stream()
                    .map(ItemIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(ItemStack[]::new);
            return new ItemStackTexture(itemIns);
        } else if (!trade.fluidOutputs.isEmpty()) {
            FluidStack[] fluidOuts = trade.fluidOutputs.stream()
                    .map(FluidIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(FluidStack[]::new);
            return new FluidStackTexture(fluidOuts);
        } else if (!trade.fluidInputs.isEmpty()) {
            FluidStack[] fluidIns = trade.fluidInputs.stream()
                    .map(FluidIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(FluidStack[]::new);
            return new FluidStackTexture(fluidIns);
        } else {
            return SpriteTexture.of(MapleBankTrade.id("textures/item/leaf.png"));
        }
    }

    // ==============================================
    // Builder 工厂方法
    // ==============================================

    public static MachineTradeBuilder builder(Identifier id) {
        return new MachineTradeBuilder(id);
    }
}

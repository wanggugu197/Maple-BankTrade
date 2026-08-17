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
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.MapleBankTrade;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;

import java.util.ArrayList;
import java.util.List;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_COMPLETED;
import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

public class MachineTradeUI {

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
                        .background(Sprites.PROGRESS_CONTAINER)
                        .overlay(Sprites.RECT_RD));

        var icon = new UIElement()
                .layout(l -> l.width(18).height(18))
                .style(s -> s.background(determineIconTexture(trade)));

        base.addChild(icon);
        return base;
    }

    /** 根据交易信息生成可见时文本列表 */
    private static List<Component> buildTradeTooltipLines(MachineTrade trade) {
        List<Component> tooltipLines = new ArrayList<>();

        boolean hasInputs = !trade.itemInputs().isEmpty() || !trade.fluidInputs().isEmpty() || trade.energyExtract() > 0 || !trade.currencyExtract().isEmpty();
        boolean hasOutputs = !trade.itemOutputs().isEmpty() || !trade.fluidOutputs().isEmpty() || trade.energyInsert() > 0 || !trade.currencyInsert().isEmpty();

        if (hasInputs) {
            tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.inputs")
                    .withStyle(ChatFormatting.YELLOW));

            for (MachineTradeIO.ItemIO io : trade.itemInputs()) {
                ItemStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (MachineTradeIO.FluidIO io : trade.fluidInputs()) {
                FluidStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (trade.energyExtract() > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_extract", trade.energyExtract())
                        .withStyle(ChatFormatting.GOLD));
            }
            for (MachineTradeIO.CurrencyIO io : trade.currencyExtract()) {
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

            for (MachineTradeIO.ItemIO io : trade.itemOutputs()) {
                ItemStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.item_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            for (MachineTradeIO.FluidIO io : trade.fluidOutputs()) {
                FluidStack temp = io.toStack();
                if (!temp.isEmpty()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.fluid_entry", io.amount())
                            .append(temp.getHoverName())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
            if (trade.energyInsert() > 0) {
                tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.energy_insert", trade.energyInsert())
                        .withStyle(ChatFormatting.GOLD));
            }
            for (MachineTradeIO.CurrencyIO io : trade.currencyInsert()) {
                if (io.isValid()) {
                    tooltipLines.add(Component.translatable("trade.maple_banktrade.machine.tooltip.currency_entry", io.amount().toString())
                            .append(io.resource().type().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
                }
            }
        }

        return tooltipLines;
    }

    /** 根据交易信息决定图标背景纹理 */
    private static IGuiTexture determineIconTexture(MachineTrade trade) {
        // 优先使用自定义纹理
        if (trade.machineTradeIcon() != null) {
            return SpriteTexture.of(trade.machineTradeIcon());
        }

        // 按优先级：输出物品 → 输入物品 → 输出流体 → 输入流体 → 默认叶子图标
        if (!trade.itemOutputs().isEmpty()) {
            ItemStack[] itemOuts = trade.itemOutputs().stream()
                    .map(MachineTradeIO.ItemIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(ItemStack[]::new);
            return new ItemStackTexture(itemOuts);
        } else if (!trade.itemInputs().isEmpty()) {
            ItemStack[] itemIns = trade.itemInputs().stream()
                    .map(MachineTradeIO.ItemIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(ItemStack[]::new);
            return new ItemStackTexture(itemIns);
        } else if (!trade.fluidOutputs().isEmpty()) {
            FluidStack[] fluidOuts = trade.fluidOutputs().stream()
                    .map(MachineTradeIO.FluidIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(FluidStack[]::new);
            return new FluidStackTexture(fluidOuts);
        } else if (!trade.fluidInputs().isEmpty()) {
            FluidStack[] fluidIns = trade.fluidInputs().stream()
                    .map(MachineTradeIO.FluidIO::toStack)
                    .filter(stack -> !stack.isEmpty())
                    .toArray(FluidStack[]::new);
            return new FluidStackTexture(fluidIns);
        } else {
            return SpriteTexture.of(MapleBankTrade.id("textures/item/leaf.png"));
        }
    }

    public static void setMachineTradeState(MachineTrade trade, UIElement uiElement, int state) {
        if (uiElement.getChildren().isEmpty()) {
            MapleBankTrade.LOGGER.warn("UIElement for trade {} has no children", trade.id());
            return;
        }

        boolean visible = (state & FLAG_VISIBLE) != 0;
        boolean completed = (state & FLAG_COMPLETED) != 0;

        // 安全获取各描述列表
        List<List<Component>> desc = trade.description() != null ? trade.description() : List.of();
        List<Component> common = !desc.isEmpty() ? desc.get(0) : List.of();
        List<Component> visibleDesc = desc.size() > 1 ? desc.get(1) : List.of();
        List<Component> invisibleDesc = desc.size() > 2 ? desc.get(2) : List.of();
        List<Component> incompleteDesc = desc.size() > 3 ? desc.get(3) : List.of();
        List<Component> completeDesc = desc.size() > 4 ? desc.get(4) : List.of();

        IGuiTexture overlay = IGuiTexture.EMPTY;

        // 1. 公共描述（始终显示）
        List<Component> components = new ArrayList<>(common);

        // 2. 完成状态相关描述
        if (completed) {
            components.addAll(completeDesc);
            overlay = OreSprites.CHECK.setColor(0xff50913d);
        } else {
            components.addAll(incompleteDesc);
        }

        // 3. 根据状态添加特定描述
        if (visible) {
            components.addAll(visibleDesc);
            // 追加 I/O 信息
            components.addAll(buildTradeTooltipLines(trade));
        } else {
            components.addAll(invisibleDesc);
            overlay = completed ? OreSprites.CHECK.setColor(0xff50913d) : Sprites.RECT_RD;
        }

        // 4. 应用样式
        IGuiTexture finalOverlay = overlay;
        uiElement.style(s -> s.overlay(finalOverlay));
        uiElement.getChildren().getFirst().style(s -> s.tooltips(components.toArray(new Component[0])));
    }
}

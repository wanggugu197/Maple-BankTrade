package com.maple.maple_banktrade.api.machineTrade.itemAttachment;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.EmptyEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.gto.registrylib.composite.ComponentItem;
import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.machineTrade.ui.MachineTradeUIHelper;
import com.maple.maple_banktrade.api.machineTrade.ui.MachineTradeUiHost;
import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHandler;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;
import com.mapleutillib.api.composite.UIItemAttachment;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;

import java.util.*;
import java.util.stream.Collectors;

public class MachineTradeItemAttachment extends UIItemAttachment<ComponentItem> implements MachineTradeUiHost {

    private final Identifier tradeType;

    @DescSynced
    private Player player;

    public MachineTradeItemAttachment(Identifier tradeType) {
        this.tradeType = tradeType;
    }

    /** 空中使用时打开钱包 UI。 */
    @Override
    public InteractionResult use(ComponentItem item, Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            HeldItemUIMenuType.openUI(serverPlayer, hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ModularUI createUI(ComponentItem item, HeldItemUIMenuType.HeldItemUIHolder holder) {
        player = holder.player;
        var root = new UIElement()
                .style(s -> s.background(IGuiTexture.EMPTY))
                .layout(l -> l.gapAll(0).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));

        root.addChild(new UIElement()
                .layout(l -> l.paddingAll(5))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1))
                .addChild(MachineTradeUIHelper.buildTradesTab(this, tradeType)));
        root.addChild(
                new InventorySlots()
                        .layout(l -> l.paddingAll(5))
                        .style(s -> s.background(Sprites.BORDER_THICK_RT1)));

        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP_MERGED))),
                holder.player);
    }

    /**
     * 本站绑定的交易类型 ID（决定配方标签页顺序）。
     */
    @Override
    public List<Identifier> tradeTypeIds() {
        return List.of(tradeType);
    }

    /**
     * UI 用配方列表：优先可见项，可回退到已注册全部条目。
     * 实现可委托 {@code listVisibleTrades} / {@code listRegisteredTrades}。
     */
    @Override
    public List<Map.Entry<Identifier, MachineTrade>> listTradesForUi(Identifier tradeTypeId) {
        MachineTradeContext context = createTradeContext(tradeTypeId);
        if (context != null) {
            return MachineTradeHandler.listVisible(context);
        }
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        if (storage != null) {
            return List.copyOf(storage.entries().entrySet());
        }
        return List.of();
    }

    /**
     * UI 点击配方后在服务端执行。
     *
     * @param desiredCount 由修饰键解码的期望次数（≥1）
     */
    @Override
    public void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount) {
        Objects.requireNonNull(tradeTypeId, "tradeTypeId");
        Objects.requireNonNull(tradeId, "tradeId");
        if (desiredCount <= 0) return;
        MachineTradeContext context = createTradeContext(tradeTypeId);
        if (context == null) return;
        MachineTradeHandler.run(context, tradeId, desiredCount);
    }

    /**
     * 当前绑定的银行卡 UUID（库存页列表用，顺序尽量与插入顺序一致）。
     */
    @Override
    public Set<UUID> getBoundCardUuids() {
        return MBTBankStates.getBankCards(player.level().getServer())
                .getUsableCardsForPlayer(BankHelper.getUuid(player))
                .stream()
                .map(BankCard::getCardUuid)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean unbindCardFromUi(UUID cardUuid) {
        return false;
    }

    public MachineTradeStorage tradeStorage(Identifier tradeTypeId) {
        return TradeRegistry.requireStorage(tradeTypeId, MachineTradeStorage.class);
    }

    public MachineTradeContext createTradeContext(Identifier tradeTypeId) {
        if (player == null) return null;
        Level level = player.level();
        MinecraftServer server = level.getServer();
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        if (server == null || storage == null) return null;
        ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(player.getInventory().getNonEquipmentItems());
        FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(0, 0);
        return new MachineTradeContext(
                null, level, server,
                itemHandler, itemHandler,
                fluidHandler, fluidHandler, EmptyEnergyHandler.INSTANCE,
                new HashSet<>(MBTBankStates.getBankCards(player.level().getServer()).getUsableCardsForPlayer(BankHelper.getUuid(player))),
                storage);
    }
}

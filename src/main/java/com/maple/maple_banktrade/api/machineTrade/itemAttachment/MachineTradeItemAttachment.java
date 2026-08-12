package com.maple.maple_banktrade.api.machineTrade.itemAttachment;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

import com.gto.registrylib.composite.ComponentItem;
import com.lowdragmc.lowdraglib2.gui.factory.HeldItemUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.machineTrade.ui.MachineTradeUIHelper;
import com.maple.maple_banktrade.api.machineTrade.ui.MachineTradeUiHost;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeDetail;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHandler;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.utils.PlayerInventoryItemStacksResourceHandler;
import com.mapleutillib.api.composite.UIItemAttachment;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;

import java.util.*;
import java.util.stream.Collectors;

public class MachineTradeItemAttachment extends UIItemAttachment<ComponentItem> implements MachineTradeUiHost {

    private final Identifier tradeType;

    private ServerPlayer player;

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
        if (holder.player instanceof ServerPlayer serverPlayer) {
            player = serverPlayer;
        }
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
     * UI 点击配方后在服务端执行。
     */
    @Override
    public void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount) {
        Objects.requireNonNull(tradeTypeId, "tradeTypeId");
        Objects.requireNonNull(tradeId, "tradeId");
        if (desiredCount <= 0) return;
        MachineTradeContext context = createTradeContext(tradeTypeId);
        if (context == null) return;
        TradeExecuteResult<MachineTradeDetail> result = MachineTradeHandler.run(context, tradeId, desiredCount);
        if (result.success() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.broadcastChanges();
        }
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

    @Override
    public MachineTradeContext createTradeContext(Identifier tradeTypeId) {
        if (player == null) return null;
        Level level = player.level();
        MinecraftServer server = level.getServer();
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        PlayerInventoryItemStacksResourceHandler itemHandler = new PlayerInventoryItemStacksResourceHandler(player);
        FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(0, 0);
        return new MachineTradeContext(
                null, player, level, server,
                itemHandler, itemHandler, fluidHandler, fluidHandler,
                new SimpleEnergyHandler(0),
                new HashSet<>(MBTBankStates.getBankCards(player.level().getServer()).getUsableCardsForPlayer(BankHelper.getUuid(player))),
                storage);
    }
}

package com.maple.maple_banktrade.bank.ui.capability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.lowdragmc.lowdraglib2.gui.slot.ItemResourceHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.trade.registry.TradeRegistry;
import com.maple.maple_banktrade.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.bank.data.CurrencyType;
import com.maple.maple_banktrade.bank.data.TradableType;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTrade;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTradeHandler;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTradeStorage;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;

import java.util.Map;
import java.util.UUID;

import static com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration.BANK_CARD_DETAIL_CONTENT_WIDTH;
import static com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration.WALLET_PAGE_PADDING;

/**
 * 可交易银行卡的买卖面板 UI。
 */
public class TradableUI {

    // ==============================================
    // 常量
    // ==============================================

    private static final int BUY_BUTTON_SIZE = 16;
    private static final int SHIFT_TRADE_MODIFIER = 1;
    private static final int CTRL_TRADE_MODIFIER = 2;
    private static final int ALT_TRADE_MODIFIER = 4;

    // ==============================================
    // UI 构建
    // ==============================================

    /** 为可交易卡创建卖出槽与买入物品面板。 */
    public static void createTradePanel(UIElement ui, Player player, BankCard card) {
        if (!(card instanceof TradableWalletBankCard tradableBankCard)) return;

        Identifier tradeTypeId = tradableBankCard.getTradeTypeId();
        CurrencyItemTradeStorage storage = TradeRegistry.requireStorage(tradeTypeId, CurrencyItemTradeStorage.class);
        if (storage == null || storage.isEmpty()) return;

        TradableType tradableType = TradableType.requireById(tradeTypeId);
        if (tradableType == null) return;
        IGuiTexture panelBackground = tradableType.backgroundTexture();

        UIElement panel = new UIElement()
                .layout(l -> l
                        .width(BANK_CARD_DETAIL_CONTENT_WIDTH - 2 * WALLET_PAGE_PADDING))
                .style(s -> s.backgroundTexture(tradableType.backgroundTexture()));

        UIElement head = new UIElement()
                .style(s -> s
                        .backgroundTexture(panelBackground))
                .layout(l -> l
                        .width(BANK_CARD_DETAIL_CONTENT_WIDTH - 2 * WALLET_PAGE_PADDING)
                        .paddingHorizontal(4).paddingVertical(2)
                        .flexDirection(FlexDirection.ROW))
                .addChild(new UIElement()
                        .style(s -> s.background(tradableType.tradableIcon()))
                        .layout(l -> l.height(8).width(8)))
                .addChild(new TextElement()
                        .setText(TradableType.getTranslationKey(tradableType.id()))
                        .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(9))
                        .style(s -> s.tooltips(tradableType.description().toArray(new Component[0]))));

        UIElement content = new UIElement()
                .layout(l -> l
                        .width(BANK_CARD_DETAIL_CONTENT_WIDTH - 2 * WALLET_PAGE_PADDING)
                        .flexDirection(FlexDirection.ROW)
                        .flexWrap(FlexWrap.WRAP)
                        .paddingTop(0)
                        .paddingBottom(4)
                        .paddingHorizontal(6));

        if (storage.hasSellable()) {
            ObservableItemResourceHandler sellHandler = new ObservableItemResourceHandler(1);
            boolean[] trading = { false };
            sellHandler.setOnChanged((index, _) -> {
                if (index != 0 || trading[0]) return;
                trading[0] = true;
                try {
                    boolean success = canTrade(player, card.getCardUuid()) && CurrencyItemTradeHandler.sellAll(
                            sellHandler, 0, card.getCardUuid(), player.level().getServer(), storage)
                            .success();
                    ItemStack remaining = drainInput(sellHandler);
                    if (!remaining.isEmpty() && (!success || !player.getInventory().add(remaining))) {
                        player.drop(remaining, false);
                    }
                } finally {
                    trading[0] = false;
                }
            });
            head.addChild(new ItemSlot().bind(new ItemResourceHandlerSlot(sellHandler, 0))
                    .layout(l -> l.width(0).height(0))
                    .style(s -> s.backgroundTexture(IGuiTexture.EMPTY)));
        }

        for (Map.Entry<Identifier, CurrencyItemTrade> entry : storage.entries().entrySet()) {
            CurrencyItemTrade trade = entry.getValue();
            ItemSlot item = new ItemSlot().setItem(trade.item().copyWithCount(trade.itemAmountPerTrade()));
            item.layout(l -> l.width(BUY_BUTTON_SIZE).height(BUY_BUTTON_SIZE));
            item.style(style -> {
                style.backgroundTexture(IGuiTexture.EMPTY)
                        .appendTooltips(Component.translatable(
                                "currency.maple_banktrade.trade_price",
                                trade.pricePerTrade().toString(),
                                Component.translatable(CurrencyType.getTranslationKey(trade.currency().currencyTypeId()))));
                if (!trade.allowsSell()) {
                    style.appendTooltips(Component.translatable("currency.maple_banktrade.trade_buy_only"));
                }
            });
            item.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    event.modifiers = event.isAltDown() ? ALT_TRADE_MODIFIER : event.isCtrlDown() ? CTRL_TRADE_MODIFIER : event.isShiftDown() ? SHIFT_TRADE_MODIFIER : 0;
                    event.stopImmediatePropagation();
                }
            });
            item.addServerEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button != 0 || !canTrade(player, card.getCardUuid())) return;
                int count = (event.modifiers & ALT_TRADE_MODIFIER) != 0 ? 64 : (event.modifiers & CTRL_TRADE_MODIFIER) != 0 ? 8 : (event.modifiers & SHIFT_TRADE_MODIFIER) != 0 ? 4 : 1;
                CurrencyItemTradeHandler.buyToPlayerInventory(
                        player, card.getCardUuid(), player.level().getServer(), storage, entry.getKey(), count);
            });
            content.addChild(item);
        }

        panel.addChildren(head, content);
        ui.addChild(panel);
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 取出卖出槽残留物品。 */
    private static ItemStack drainInput(ObservableItemResourceHandler handler) {
        ItemResource resource = handler.getResource(0);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        int amount = handler.getAmountAsInt(0);
        if (amount <= 0) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(0, resource, amount, transaction);
            if (extracted <= 0) return ItemStack.EMPTY;
            transaction.commit();
            return resource.toStack(extracted);
        }
    }

    /** 校验玩家是否可用该可交易卡。 */
    private static boolean canTrade(Player player, UUID cardUuid) {
        if (player == null || player.level().isClientSide() || player.level().getServer() == null || cardUuid == null) {
            return false;
        }
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        return data.canUse(player.getUUID(), cardUuid) && data.getCard(cardUuid) instanceof TradableWalletBankCard;
    }
}

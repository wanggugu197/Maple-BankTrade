package com.maple.maple_banktrade.api.machineTrade.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.data.TradableType;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MachineTradeUIHelper {

    public static final int SHIFT_TRADE_COUNT = 4;
    public static final int CTRL_TRADE_COUNT = 8;
    public static final int ALT_TRADE_COUNT = 64;

    public static UIElement buildTradesTab(MachineTradeUiHost host, Identifier tradeTypeId) {
        var scroller = createScrollerView();
        TradableType type = TradableType.requireById(tradeTypeId);
        if (type != null) {
            scroller.addScrollViewChild(buildTradeTypeHeader(type));
        }
        var grid = new UIElement().layout(l -> l.width(260).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));

        var value = new BindableValue<Identifier[]>().setValue(new Identifier[] {}, false);
        value.registerValueListener(v -> {
            MapleBankTrade.LOGGER.info("{} value changed {}",tradeTypeId, Arrays.toString(v));
            grid.clearAllChildren();
            addTrades(host, tradeTypeId, grid, v);
        });
        value.bind(DataBindingBuilder.create(() ->
                        host.listTradesForUi(tradeTypeId).stream().map(Map.Entry::getKey).toArray(Identifier[]::new),
                _ -> {}).build());
        addTrades(host, tradeTypeId, grid, value.getValue());

        scroller.addScrollViewChildren(grid, value);
        return scroller;
    }

    public static void addTrades(MachineTradeUiHost host, Identifier tradeTypeId, UIElement grid, Identifier[] ids) {
        List<Map.Entry<Identifier, MachineTrade>> trades = host.listTradesForUi(tradeTypeId).stream()
                .filter(entry -> Arrays.stream(ids).toList().contains(entry.getKey()))
                .toList();

        if (trades.isEmpty()) {
            grid.addChild(new Label().setText(
                    Component.translatable("ui.maple_banktrade.trading_station.no_recipes")));
        } else {
            for (Map.Entry<Identifier, MachineTrade> entry : trades) {
                grid.addChild(buildTradeButton(host, tradeTypeId, entry));
            }
        }
    }

    public static UIElement buildTradeTypeHeader(TradableType type) {
        var header = new UIElement()
                .layout(l -> l.width(260).paddingAll(4))
                .style(s -> s.background(type.backgroundTexture()));

        var titleRow = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW));
        titleRow.addChild(new UIElement()
                .style(s -> s.background(type.tradableIcon()))
                .layout(l -> l.width(12).height(12)));
        titleRow.addChild(new TextElement()
                .setText(type.getDisplayName())
                .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(10))
                .layout(l -> l.width(252)));
        header.addChild(titleRow);

        for (Component line : type.description()) {
            header.addChild(new TextElement()
                    .setText(line)
                    .textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8))
                    .layout(l -> l.width(252)));
        }
        return header;
    }

    /**
     * 配方点击：客户端把批量次数写入 {@code modifiers}，服务端 RPC 执行。
     */
    public static @NonNull UIElement buildTradeButton(
                                                      MachineTradeUiHost host,
                                                      Identifier tradeTypeId,
                                                      Map.Entry<Identifier, MachineTrade> entry) {
        Identifier tradeId = entry.getKey();
        UIElement tradeUI = MachineTrade.getMachineTradeIcon(entry.getValue());

        tradeUI.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                e.modifiers = tradeCountFromKeyboard(e);
            }
        });
        tradeUI.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) {
                host.runTradeFromUi(tradeTypeId, tradeId, Math.max(1, e.modifiers));
            }
        });
        return tradeUI;
    }

    /** 客户端：Alt=64 / Ctrl=8 / Shift=4 / 默认 1。直接写入 {@link UIEvent#modifiers} 传给服务端。 */
    public static int tradeCountFromKeyboard(UIEvent event) {
        if (event.isAltDown()) {
            return ALT_TRADE_COUNT;
        }
        if (event.isCtrlDown()) {
            return CTRL_TRADE_COUNT;
        }
        if (event.isShiftDown()) {
            return SHIFT_TRADE_COUNT;
        }
        return 1;
    }

    public static ScrollerView createScrollerView() {
        ScrollerView list = new ScrollerView()
                .scrollerStyle(s -> s
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.NEVER)
                        .scrollerViewStyle(0)
                        .mode(ScrollerMode.VERTICAL));
        list.layout(l -> l.width(260).height(130)).style(s -> s.background(IGuiTexture.EMPTY));
        list.viewContainer.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY));
        list.viewPort.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY));
        return list;
    }
}

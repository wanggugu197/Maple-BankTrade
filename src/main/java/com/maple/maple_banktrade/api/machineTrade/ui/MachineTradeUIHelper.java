package com.maple.maple_banktrade.api.machineTrade.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.google.common.reflect.TypeToken;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
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
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeUI;
import com.mapleutillib.utils.RLUtils;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MachineTradeUIHelper {

    public static final int SHIFT_TRADE_COUNT = 4;
    public static final int CTRL_TRADE_COUNT = 8;
    public static final int ALT_TRADE_COUNT = 64;

    public static UIElement buildTradesTab(MachineTradeUiHost host, Identifier tradeTypeId) {
        var scroller = createScrollerView();
        TradableType type = TradableType.requireById(tradeTypeId);
        if (type != null) scroller.addScrollViewChild(buildTradeTypeHeader(type));

        var grid = new UIElement().layout(l -> l.width(260).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));

        // 获取全部交易条目（用于 UI 显示）
        Map<Identifier, MachineTrade> trades = host.listAllTrades(tradeTypeId);

        // 添加所有交易按钮（可见/不可见由后续状态控制）
        addTrades(host, trades, tradeTypeId, grid);

        // 状态映射：ID → 状态码 (int)
        var value = new BindableValue<Map<String, Integer>>();
        AtomicInteger counter = new AtomicInteger();
        Map<String, Integer> map = new LinkedHashMap<>();
        value.bind(DataBindingBuilder.create(
                () -> {
                    if (counter.incrementAndGet() % 20 == 1) {
                        MachineTradeContext context = host.createTradeContext(tradeTypeId);
                        if (context != null) {
                            map.clear();
                            for (Map.Entry<Identifier, MachineTrade> entry : trades.entrySet())
                                map.put(entry.getKey().toString(), entry.getValue().stateHook().getState(context, entry.getValue()));
                        }
                    }
                    return Map.copyOf(map);
                },
                _ -> {})
                .syncType(new TypeToken<Map<String, Integer>>() {}.getType())
                .initialValue(map)
                .c2sStrategy(SyncStrategy.NONE)
                .remoteSetter(stateMap -> {
                    grid.getChildren().forEach(child -> {
                        String id = child.getId();
                        Integer state = stateMap.get(id);
                        if (state == null) state = 0;
                        MachineTrade trade = trades.get(RLUtils.parse(id));
                        if (trade == null) return;
                        MachineTradeUI.setMachineTradeState(trade, child, state);
                    });
                })
                .build());

        scroller.addScrollViewChildren(grid, value);
        return scroller;
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

    /** 添加配方按钮：客户端把批量次数写入 {@code modifiers}，服务端执行 */
    public static void addTrades(MachineTradeUiHost host, Map<Identifier, MachineTrade> trades, Identifier tradeTypeId, UIElement grid) {
        if (trades.isEmpty()) {
            grid.addChild(new Label().setText(Component.translatable("ui.maple_banktrade.trading_station.no_recipes")));
        } else {
            trades.forEach((_, entry) -> {
                UIElement tradeUI = MachineTradeUI.getMachineTradeIcon(entry);
                tradeUI.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) e.modifiers = tradeCountFromKeyboard(e);
                });
                tradeUI.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (e.button == 0) host.runTradeFromUi(tradeTypeId, entry.id(), Math.max(1, e.modifiers));
                });
                grid.addChild(tradeUI);
            });
        }
    }

    /** 客户端：Alt=64 / Ctrl=8 / Shift=4 / 默认 1。直接写入 {@link UIEvent#modifiers} 传给服务端。 */
    public static int tradeCountFromKeyboard(UIEvent event) {
        if (event.isAltDown()) return ALT_TRADE_COUNT;
        if (event.isCtrlDown()) return CTRL_TRADE_COUNT;
        if (event.isShiftDown()) return SHIFT_TRADE_COUNT;
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

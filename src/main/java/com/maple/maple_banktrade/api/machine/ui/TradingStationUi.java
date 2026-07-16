package com.maple.maple_banktrade.api.machine.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TabView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.OreSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;
import com.maple.maple_banktrade.bank.data.TradableType;
import com.maple.maple_banktrade.trade.machine.MachineTrade;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * 贸易站 UI 构建 API：主壳、库存页、配方页、槽网格与修饰键批量。
 * <p>
 * 宿主实现 {@link TradingStationUiHost}；库存页可用 {@link #fullIoInventoryTab}。
 * </p>
 */
public final class TradingStationUi {

    /** 槽位网格单行像素宽度（6 × 18）。 */
    public static final int SLOT_ROW_WIDTH = 18 * 6;

    public static final int SHIFT_TRADE_COUNT = 4;
    public static final int CTRL_TRADE_COUNT = 8;
    public static final int ALT_TRADE_COUNT = 64;

    /** 写入 UIEvent.modifiers 的自定义标志（非 GLFW）。 */
    public static final int SHIFT_TRADE_MODIFIER = 1;
    public static final int CTRL_TRADE_MODIFIER = 2;
    public static final int ALT_TRADE_MODIFIER = 4;

    private TradingStationUi() {}

    // ╔══════════════════════════════════════════════╗
    // ║ 主界面壳 ║
    // ╚══════════════════════════════════════════════╝

    /**
     * 构建标准贸易站 UI：库存页 + 每个 trade type 一页配方 + 玩家背包。
     *
     * @param inventoryTab 库存标签页内容（由站类型决定是否含流体/能量）
     */
    public static ModularUI create(
                                   BlockUIMenuType.BlockUIHolder holder,
                                   TradingStationUiHost host,
                                   UIElement inventoryTab) {
        Objects.requireNonNull(holder, "holder");
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(inventoryTab, "inventoryTab");

        var root = new UIElement()
                .style(s -> s.background(IGuiTexture.EMPTY))
                .layout(l -> l.gapAll(0).alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER));

        var tabView = new TabView();
        tabView.tabHeaderContainer.layout(l -> l.width(270).height(14));
        tabView.tabContentContainer.layout(l -> l.width(270).height(140));

        tabView.addTab(
                new Tab().setText(Component.translatable("ui.maple_banktrade.trading_station.tab.inventory")),
                inventoryTab);
        for (Identifier typeId : host.tradeTypeIds()) {
            tabView.addTab(buildTradeTypeTab(typeId), buildTradesTab(host, typeId));
        }
        root.addChild(tabView);

        var lowerPart = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW));
        var additional = new UIElement()
                .layout(l -> l.width(98).height(87).paddingAll(5))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1));
        var inventory = new InventorySlots()
                .layout(l -> l.paddingAll(5))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1));
        lowerPart.addChildren(additional, inventory);
        root.addChild(lowerPart);

        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP_MERGED))),
                holder.player);
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 库存页工厂 ║
    // ╚══════════════════════════════════════════════╝

    /**
     * 物品+流体 I/O；能量条与绑定卡列表可选。
     *
     * @param boundCardUuids 绑定卡 UUID 供应（通常 {@code station::getCardUuids}）；null 不显示列表
     */
    public static UIElement fullIoInventoryTab(
                                               ItemStacksResourceHandler itemInput,
                                               ItemStacksResourceHandler itemOutput,
                                               FluidStacksResourceHandler fluidInput,
                                               FluidStacksResourceHandler fluidOutput,
                                               @Nullable EnergyHandler energy,
                                               @Nullable Supplier<Set<UUID>> boundCardUuids) {
        Objects.requireNonNull(itemInput, "itemInput");
        Objects.requireNonNull(itemOutput, "itemOutput");
        Objects.requireNonNull(fluidInput, "fluidInput");
        Objects.requireNonNull(fluidOutput, "fluidOutput");
        var scroller = createScrollerView();
        var panel = new UIElement().layout(l -> l.width(260).gapAll(2).alignItems(AlignItems.CENTER));
        var columns = new UIElement().layout(l -> l.width(260).flexDirection(FlexDirection.ROW));
        columns.addChild(ioColumn(
                Component.translatable("ui.maple_banktrade.trading_station.input"),
                itemInput, fluidInput));
        columns.addChild(ioColumn(
                Component.translatable("ui.maple_banktrade.trading_station.output"),
                itemOutput, fluidOutput));
        panel.addChild(columns);
        if (energy != null) {
            panel.addChild(buildEnergyBar(energy));
        }
        if (boundCardUuids != null) {
            panel.addChild(buildBoundCardsPanel(boundCardUuids));
        }
        scroller.addScrollViewChild(panel);
        return scroller;
    }

    /**
     * 绑定银行卡列表：标题（数量）+ 多行卡信息（S2C 绑定）。
     */
    public static UIElement buildBoundCardsPanel(Supplier<Set<UUID>> boundCardUuids) {
        Objects.requireNonNull(boundCardUuids, "boundCardUuids");
        var panel = new UIElement()
                .layout(l -> l.width(250).gapAll(1).paddingAll(4).alignItems(AlignItems.FLEX_START))
                .style(s -> s.background(Sprites.RECT_RD_T));

        Label title = new Label();
        title.setValue(Component.translatable("ui.maple_banktrade.trading_station.bound_cards.title", 0));
        title.textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(9));
        title.bind(DataBindingBuilder.componentS2C(() -> {
            Set<UUID> set = boundCardUuids.get();
            int n = set == null ? 0 : set.size();
            return Component.translatable("ui.maple_banktrade.trading_station.bound_cards.title", n);
        }).build());
        panel.addChild(title);

        Label list = new Label();
        list.setValue(Component.empty());
        list.textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8));
        list.layout(l -> l.width(246));
        list.bind(DataBindingBuilder.componentS2C(() -> boundCardsBody(boundCardUuids.get())).build());
        panel.addChild(list);
        return panel;
    }

    /** 列表正文：空提示或「银行 · 卡名 (短UUID)」多行。 */
    public static Component boundCardsBody(@Nullable Set<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Component.translatable("ui.maple_banktrade.trading_station.bound_cards.empty");
        }
        BankCardsWorldData data = tryWorldData();
        MutableComponent root = Component.empty();
        boolean first = true;
        for (UUID uuid : new ArrayList<>(uuids)) {
            if (uuid == null) continue;
            if (!first) {
                root.append(Component.literal("\n"));
            }
            first = false;
            root.append(boundCardLine(uuid, data));
        }
        return first ? Component.translatable("ui.maple_banktrade.trading_station.bound_cards.empty") : root;
    }

    private static Component boundCardLine(UUID uuid, @Nullable BankCardsWorldData data) {
        BankCard card = data == null ? null : data.getCard(uuid);
        if (card == null) {
            return Component.translatable(
                    "ui.maple_banktrade.trading_station.bound_cards.entry_unknown",
                    shortUuid(uuid));
        }
        return Component.translatable(
                "ui.maple_banktrade.trading_station.bound_cards.entry",
                bankDisplayName(card),
                cardDisplayName(card),
                shortUuid(uuid));
    }

    private static Component cardDisplayName(BankCard card) {
        return Component.translatable(BankCardFactory.getTranslationKey(card.getNameIndex()));
    }

    private static Component bankDisplayName(BankCard card) {
        BankInfo info = BankInfo.of(BankType.requireById(card.getBankTypeId()));
        return info == null ? Component.literal(card.getBankTypeId().toString()) : info.name();
    }

    private static String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.length() <= 8 ? s : s.substring(0, 8);
    }

    @Nullable
    private static BankCardsWorldData tryWorldData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return MBTBankStates.getBankCards(server);
    }

    // ╔══════════════════════════════════════════════╗
    // ║ 槽位 / 能量 / 滚动容器 ║
    // ╚══════════════════════════════════════════════╝

    /** 单侧：标题 + 物品槽。 */
    public static UIElement itemColumn(Component title, ItemStacksResourceHandler items) {
        return new UIElement()
                .layout(l -> l.width(130).alignItems(AlignItems.CENTER))
                .addChildren(
                        new TextElement().setText(title).textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true)),
                        slotGrid(items.size(), i -> new ItemSlot().bind(items, i)));
    }

    /** 单侧：标题 + 物品槽 + 流体槽。 */
    public static UIElement ioColumn(
                                     Component title,
                                     ItemStacksResourceHandler items,
                                     FluidStacksResourceHandler fluids) {
        return new UIElement()
                .layout(l -> l.width(130).alignItems(AlignItems.CENTER))
                .addChildren(
                        new TextElement().setText(title).textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true)),
                        slotGrid(items.size(), i -> new ItemSlot().bind(items, i)),
                        slotGrid(fluids.size(), i -> new FluidSlot().bind(fluids, i)));
    }

    /** 按槽位数生成换行网格。 */
    public static UIElement slotGrid(int size, IntFunction<UIElement> factory) {
        var grid = new UIElement()
                .layout(l -> l.width(SLOT_ROW_WIDTH).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));
        for (int i = 0; i < size; i++) {
            grid.addChild(factory.apply(i));
        }
        return grid;
    }

    /**
     * 能量进度条（0–1 比例绑定，避免 MAX_VALUE range 溢出）。
     */
    public static ProgressBar buildEnergyBar(EnergyHandler energy) {
        Objects.requireNonNull(energy, "energy");
        ProgressBar bar = new ProgressBar()
                .setRange(0, 1)
                .bar(b -> b.getStyle().backgroundTexture(OreSprites.BTN_PRESSED_RED));
        bar.layout(l -> l.width(250).height(20)).style(s -> s.background(Sprites.RECT_RD_T));
        bar.bind(DataBindingBuilder.floatValS2C(() -> {
            long cap = energy.getCapacityAsLong();
            return cap <= 0 ? 0f : (float) ((double) energy.getAmountAsLong() / (double) cap);
        }).build());
        bar.label(label -> label.bind(DataBindingBuilder.componentS2C(() -> energyLabel(energy)).build()));
        return bar;
    }

    public static Component energyLabel(EnergyHandler energy) {
        long amount = energy.getAmountAsLong();
        long capacity = energy.getCapacityAsLong();
        double ratio = capacity <= 0 ? 0 : ((double) amount / (double) capacity) * 100;
        String pct = String.format("%.4f%%", ratio);
        return Component.translatable("ui.maple_banktrade.trading_station.energy", amount, capacity, pct);
    }

    /** 无滚动条、固定尺寸的垂直 ScrollerView。 */
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

    // ╔══════════════════════════════════════════════╗
    // ║ 配方页 / 按钮 / 修饰键 ║
    // ╚══════════════════════════════════════════════╝

    /** 交易类型 Tab 头：名称 + 描述 tooltip。 */
    public static Tab buildTradeTypeTab(Identifier tradeTypeId) {
        TradableType type = TradableType.requireById(tradeTypeId);
        Component title = type != null ? type.getDisplayName() : TradableType.getDisplayName(tradeTypeId);
        Tab tab = new Tab().setText(title);
        if (type != null && !type.description().isEmpty()) {
            tab.style(s -> s.tooltips(type.description().toArray(new Component[0])));
        }
        return tab;
    }

    /** 某一交易类型的配方标签页（类型头 + 配方网格）。 */
    public static UIElement buildTradesTab(TradingStationUiHost host, Identifier tradeTypeId) {
        var scroller = createScrollerView();
        TradableType type = TradableType.requireById(tradeTypeId);
        if (type != null) {
            scroller.addScrollViewChild(buildTradeTypeHeader(type));
        }

        List<Map.Entry<Identifier, MachineTrade>> trades = host.listTradesForUi(tradeTypeId);
        var grid = new UIElement()
                .layout(l -> l.width(260).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));
        if (trades.isEmpty()) {
            grid.addChild(new Label().setText(Component.translatable("ui.maple_banktrade.trading_station.no_recipes")));
        } else {
            for (Map.Entry<Identifier, MachineTrade> entry : trades) {
                grid.addChild(buildTradeButton(host, tradeTypeId, entry));
            }
        }
        scroller.addScrollViewChild(grid);
        return scroller;
    }

    /** 交易类型页眉：图标 + 名称 + 描述。 */
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
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(10)));
        header.addChild(titleRow);

        for (Component line : type.description()) {
            header.addChild(new TextElement()
                    .setText(line)
                    .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8)));
        }
        return header;
    }

    /**
     * 可点击配方图标。
     * <p>
     * 客户端编码 Alt/Ctrl/Shift → modifiers；服务端解码后 {@link TradingStationUiHost#runTradeFromUi}。
     * 勿 stopPropagation，以免干扰 LDLib2 服务端事件。
     * </p>
     */
    public static @NonNull UIElement buildTradeButton(
                                                      TradingStationUiHost host,
                                                      Identifier tradeTypeId,
                                                      Map.Entry<Identifier, MachineTrade> entry) {
        Identifier tradeId = entry.getKey();
        UIElement tradeUI = entry.getValue().getMachineTradeIcon();

        tradeUI.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) return;
            event.modifiers = 0;
            if (event.isAltDown()) event.modifiers |= ALT_TRADE_MODIFIER;
            else if (event.isCtrlDown()) event.modifiers |= CTRL_TRADE_MODIFIER;
            else if (event.isShiftDown()) event.modifiers |= SHIFT_TRADE_MODIFIER;
        });

        tradeUI.addServerEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) return;
            host.runTradeFromUi(tradeTypeId, tradeId, desiredCountFromModifiers(event.modifiers));
        });
        return tradeUI;
    }

    /** 将自定义 modifiers 解码为执行次数（默认 1）。 */
    public static int desiredCountFromModifiers(int modifiers) {
        if ((modifiers & ALT_TRADE_MODIFIER) != 0) return ALT_TRADE_COUNT;
        if ((modifiers & CTRL_TRADE_MODIFIER) != 0) return CTRL_TRADE_COUNT;
        if ((modifiers & SHIFT_TRADE_MODIFIER) != 0) return SHIFT_TRADE_COUNT;
        return 1;
    }

    /**
     * 可见列表优先，空则回退已注册列表（供 host 默认实现复用）。
     */
    public static List<Map.Entry<Identifier, MachineTrade>> preferVisibleOrRegistered(
                                                                                      Supplier<List<Map.Entry<Identifier, MachineTrade>>> visible,
                                                                                      Supplier<List<Map.Entry<Identifier, MachineTrade>>> registered) {
        List<Map.Entry<Identifier, MachineTrade>> trades = visible.get();
        if (trades == null || trades.isEmpty()) {
            trades = registered.get();
        }
        return trades == null ? List.of() : trades;
    }
}

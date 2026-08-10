package com.maple.maple_banktrade.api.machineTrade.ui;

import net.minecraft.network.chat.Component;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
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
import com.maple.maple_banktrade.api.bank.data.TradableType;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;

import java.util.*;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

/**
 * 贸易站 UI 构建。
 * <p>
 * 数据通道（对齐 LDLib2 Sync）：
 * <ul>
 * <li><b>S2C</b>：{@link DataBindingBuilder}（{@code *S2C}）— 标题、能量、卡名等只读展示。</li>
 * <li><b>C2S 动作</b>：{@link UIElement#addServerEventListener} /
 * {@link Button#setOnServerClick} — 解绑 / 执行配方（RPC 事件，无需 message）。</li>
 * <li><b>本地 UI</b>：{@link Button#setOnClick} — 仅客户端表现（如隐藏已解绑行）。</li>
 * </ul>
 * 批量次数：客户端写入 {@link UIEvent#modifiers}（直接存次数 1/4/8/64），服务端读取。
 * </p>
 */
public final class TradingStationUi {

    public static final int SLOT_ROW_WIDTH = 18 * 6;

    private TradingStationUi() {}

    // ── 主壳 ──────────────────────────────────────────

    public static ModularUI create(
                                   BlockUIMenuType.BlockUIHolder holder,
                                   MachineTradeUiHost host,
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
            tabView.addTab(buildTradeTypeTab(typeId), MachineTradeUIHelper.buildTradesTab(host, typeId));
        }
        root.addChild(tabView);

        var lower = new UIElement().layout(l -> l.flexDirection(FlexDirection.ROW));
        lower.addChildren(
                buildAdditionalPanel(host),
                new InventorySlots()
                        .layout(l -> l.paddingAll(5))
                        .style(s -> s.background(Sprites.BORDER_THICK_RT1)));
        root.addChild(lower);

        return new ModularUI(
                UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.GDP_MERGED))),
                holder.player);
    }

    /**
     * 左下额外小面板：可滚动；仅当 {@link MachineTradeUiHost#supportsAutoTrade()} 时显示自动交易 Switch。
     */
    public static UIElement buildAdditionalPanel(MachineTradeUiHost host) {
        Objects.requireNonNull(host, "host");
        var panel = new UIElement()
                .layout(l -> l.width(98).height(87).paddingAll(3))
                .style(s -> s.background(Sprites.BORDER_THICK_RT1));

        ScrollerView scroller = new ScrollerView()
                .scrollerStyle(s -> s
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .scrollerViewStyle(0)
                        .mode(ScrollerMode.VERTICAL));
        scroller.layout(l -> l.width(92).height(81));
        scroller.viewContainer.layout(l -> l.paddingAll(1)).style(s -> s.background(IGuiTexture.EMPTY));
        scroller.viewPort.layout(l -> l.paddingAll(0)).style(s -> s.background(IGuiTexture.EMPTY));

        if (host.supportsAutoTrade()) {
            scroller.addScrollViewChild(buildAutoTradeSwitchRow(host));
        }

        panel.addChild(scroller);
        return panel;
    }

    /**
     * 自动交易开关行：标签 + {@link Switch}。
     * <p>
     * S2C 绑定显示 BE 状态；服务端 {@link UIEvents#MOUSE_DOWN} 翻转配置
     * （Switch 客户端本地切换，服务端改 {@link MachineTradeUiHost#setAutoTradeEnabled}）。
     * </p>
     */
    public static UIElement buildAutoTradeSwitchRow(MachineTradeUiHost host) {
        var row = new UIElement()
                .layout(l -> l
                        .width(88)
                        .gapAll(2)
                        .flexDirection(FlexDirection.COLUMN)
                        .alignItems(AlignItems.FLEX_START)
                        .wrap(FlexWrap.WRAP)
                        .paddingAll(2));

        Label title = new Label();
        title.setValue(Component.translatable("ui.maple_banktrade.trading_station.auto_trade"));
        title.textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).fontSize(8).textWrap(TextWrap.WRAP));
        title.style(s -> s.tooltips(
                Component.translatable("ui.maple_banktrade.trading_station.auto_trade.tooltip")));

        Switch sw = new Switch();
        sw.setOn(host.isAutoTradeEnabled(), false);
        // 服务端 → 客户端同步开关外观（LDLib2 IBinding / Sync）
        sw.bind(DataBindingBuilder.boolS2C(host::isAutoTradeEnabled).build());
        // 服务端：点击时翻转配置（与客户端 Switch 本地切换同向）
        sw.addServerEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (event.button != 0) {
                return;
            }
            host.setAutoTradeEnabled(!host.isAutoTradeEnabled());
        });

        var switchRow = new UIElement()
                .layout(l -> l
                        .width(84)
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.SPACE_BETWEEN)
                        .gapAll(2));
        switchRow.addChildren(title, sw);
        row.addChild(switchRow);
        return row;
    }

    // ── 库存页 ────────────────────────────────────────

    public static UIElement fullIoInventoryTab(
                                               ItemStacksResourceHandler itemInput,
                                               ItemStacksResourceHandler itemOutput,
                                               FluidStacksResourceHandler fluidInput,
                                               FluidStacksResourceHandler fluidOutput,
                                               @Nullable EnergyHandler energy,
                                               @Nullable MachineTradeUiHost host) {
        Objects.requireNonNull(itemInput, "itemInput");
        Objects.requireNonNull(itemOutput, "itemOutput");
        Objects.requireNonNull(fluidInput, "fluidInput");
        Objects.requireNonNull(fluidOutput, "fluidOutput");

        var panel = new UIElement().layout(l -> l.width(260).gapAll(2).alignItems(AlignItems.CENTER));
        var columns = new UIElement().layout(l -> l.width(260).flexDirection(FlexDirection.ROW));
        columns.addChild(ioColumn(
                Component.translatable("ui.maple_banktrade.trading_station.input"), itemInput, fluidInput));
        columns.addChild(ioColumn(
                Component.translatable("ui.maple_banktrade.trading_station.output"), itemOutput, fluidOutput));
        panel.addChild(columns);
        if (energy != null) {
            panel.addChild(buildEnergyBar(energy));
        }
        if (host != null) {
            panel.addChild(buildBoundCardsPanel(host));
        }

        var scroller = MachineTradeUIHelper.createScrollerView();
        scroller.addScrollViewChild(panel);
        return scroller;
    }

    /**
     * 绑定卡列表。数量/卡名走 S2C；解绑走 {@link Button#setOnServerClick}。
     */
    public static UIElement buildBoundCardsPanel(MachineTradeUiHost host) {
        Objects.requireNonNull(host, "host");
        var panel = new UIElement()
                .layout(l -> l.width(250).gapAll(1).paddingAll(4).alignItems(AlignItems.FLEX_START))
                .style(s -> s.background(Sprites.RECT_RD_T));

        Label title = label(9);
        title.bind(DataBindingBuilder.componentS2C(() -> {
            Set<UUID> set = host.getBoundCardUuids();
            int n = set == null ? 0 : set.size();
            return Component.translatable("ui.maple_banktrade.trading_station.bound_cards.title", n);
        }).build());
        panel.addChild(title);

        var rows = new UIElement()
                .layout(l -> l.width(246).gapAll(1).flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.STRETCH));
        Set<UUID> uuids = host.getBoundCardUuids();
        if (uuids == null || uuids.isEmpty()) {
            Label empty = label(8);
            empty.setValue(Component.translatable("ui.maple_banktrade.trading_station.bound_cards.empty"));
            empty.layout(l -> l.width(246));
            rows.addChild(empty);
        } else {
            for (UUID uuid : new ArrayList<>(uuids)) {
                if (uuid != null) {
                    rows.addChild(buildBoundCardRow(host, uuid));
                }
            }
        }
        panel.addChild(rows);
        return panel;
    }

    private static UIElement buildBoundCardRow(MachineTradeUiHost host, UUID uuid) {
        var row = new UIElement()
                .layout(l -> l
                        .width(246)
                        .flexDirection(FlexDirection.ROW)
                        .alignItems(AlignItems.CENTER)
                        .justifyContent(AlignContent.SPACE_BETWEEN)
                        .wrap(FlexWrap.WRAP)
                        .gapAll(2));

        // S2C：服务端解析卡名，客户端只收 Component
        Label info = new Label();
        info.textStyle(s -> s.adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(8));
        info.layout(l -> l.width(190).flexGrow(1f));
        info.bind(DataBindingBuilder.componentS2C(() -> boundCardLine(uuid)).build());

        Button unbind = new Button()
                .setText(Component.translatable("ui.maple_banktrade.trading_station.bound_cards.unbind"));
        unbind.textStyle(s -> s.adaptiveWidth(true));
        unbind.layout(l -> l.height(12));
        // C2S：服务端解绑
        unbind.setOnServerClick(e -> {
            if (e.button == 0) {
                host.unbindCardFromUi(uuid);
            }
        });
        // 本地：隐藏行（标题数量仍由 S2C 更新）
        unbind.setOnClick(e -> {
            if (e.button == 0) {
                row.setDisplay(false);
            }
        });

        row.addChildren(info, unbind);
        return row;
    }

    private static Component boundCardLine(UUID uuid) {
        BankCardsWorldData data = tryWorldData();
        BankCard card = data == null ? null : data.getCard(uuid);
        if (card == null) {
            return Component.translatable(
                    "ui.maple_banktrade.trading_station.bound_cards.entry_unknown", shortUuid(uuid));
        }
        BankInfo bank = BankInfo.of(BankType.requireById(card.getBankTypeId()));
        Component bankName = bank == null ? Component.literal(card.getBankTypeId().toString()) : Component.translatable(BankInfo.getBankInfoTranslationKey(bank.type()));
        Component cardName = Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(card.getNameIndex()));
        return Component.translatable(
                "ui.maple_banktrade.trading_station.bound_cards.entry", bankName, cardName, shortUuid(uuid));
    }

    private static String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.length() <= 8 ? s : s.substring(0, 8);
    }

    @Nullable
    private static BankCardsWorldData tryWorldData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : MBTBankStates.getBankCards(server);
    }

    // ── 槽位 / 能量 ───────────────────────────────────
    public static UIElement ioColumn(
                                     Component title,
                                     ItemStacksResourceHandler items,
                                     FluidStacksResourceHandler fluids) {
        return new UIElement()
                .layout(l -> l.width(130).alignItems(AlignItems.CENTER))
                .addChildren(
                        titleText(title),
                        slotGrid(items.size(), i -> new ItemSlot().bind(items, i)),
                        slotGrid(fluids.size(), i -> new FluidSlot().bind(fluids, i)));
    }

    public static UIElement slotGrid(int size, IntFunction<UIElement> factory) {
        var grid = new UIElement()
                .layout(l -> l.width(SLOT_ROW_WIDTH).flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP));
        for (int i = 0; i < size; i++) {
            grid.addChild(factory.apply(i));
        }
        return grid;
    }

    /** 能量条：比例 + 文案均 S2C。 */
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
        return Component.translatable(
                "ui.maple_banktrade.trading_station.energy",
                amount, capacity, String.format("%.4f%%", ratio));
    }

    // ── 配方页 ────────────────────────────────────────

    public static Tab buildTradeTypeTab(Identifier tradeTypeId) {
        TradableType type = TradableType.requireById(tradeTypeId);
        Component title = type != null ? type.getDisplayName() : TradableType.getDisplayName(tradeTypeId);
        Tab tab = new Tab().setText(title);
        if (type != null && !type.description().isEmpty()) {
            tab.style(s -> s.tooltips(type.description().toArray(new Component[0])));
        }
        return tab;
    }

    // ── 小工具 ────────────────────────────────────────

    private static Label label(int fontSize) {
        Label label = new Label();
        label.textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true).textWrap(TextWrap.WRAP).fontSize(fontSize));
        return label;
    }

    private static TextElement titleText(Component title) {
        return new TextElement()
                .setText(title)
                .textStyle(s -> s.adaptiveWidth(true).adaptiveHeight(true));
    }
}

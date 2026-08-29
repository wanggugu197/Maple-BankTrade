package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;
import com.maple.maple_banktrade.api.bank.data.CardInfo;
import dev.vfyjxf.taffy.style.AlignContent;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * 钱包手持 UI 注册：左右页、银行槽位、卡列表与开启动画。
 */
public class WalletUIRegistration extends PlayerUIMenuType {

    // ==============================================
    // 常量
    // ==============================================

    /** 钱包 UI 标识。 */
    public static final ResourceLocation WALLET_UI = MapleBankTrade.id("wallet_ui");

    /** 页宽；变换动画 translate 依赖此常量，布局与 wallet.lss 对齐。 */
    public static final int WALLET_PAGE_WIDTH = 135;
    /** 页内边距。 */
    public static final int WALLET_PAGE_PADDING = 4;
    /** 右页银行槽位数量。 */
    private static final int BANK_UI_SLOT_COUNT = 6;
    /** 主鼠标键。 */
    private static final int PRIMARY_MOUSE_BUTTON = 0;
    /** 卡详情内容区宽度。 */
    public static final int BANK_CARD_DETAIL_CONTENT_WIDTH = 260;
    /** 银行槽位同步字符串分隔符。 */
    private static final String BANK_SLOT_SYNC_SEPARATOR = "|";
    /** 列表同步 Tag 字段键。 */
    private static final String BANK_CARD_LIST_KEY = "cards";
    private static final String BANK_CARD_LIST_BANK_KEY = "bank_type";
    private static final String BANK_CARD_LIST_COUNT_KEY = "card_count";
    private static final String BANK_CARD_SYNC_REVISION_KEY = "sync_revision";
    /** 钱包打开总时长。 */
    private static final float WALLET_OPEN_DURATION = 0.56f;
    /** 单侧翻页动画时长。 */
    private static final float WALLET_FLIP_HALF_DURATION = WALLET_OPEN_DURATION / 2f;
    /** 封面提示呼吸动画时长。 */
    private static final float WALLET_PROMPT_BREATH_DURATION = 0.9f;
    /** 封面提示最大不透明度。 */
    private static final float WALLET_PROMPT_MAX_OPACITY = 0.5f;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册钱包手持 UI：组装左右页、封面与开启动画。 */
    public static void init() {
        PlayerUIMenuType.register(WALLET_UI, player -> {

            // 静态尺寸/对齐见 wallet.lss；此处只组装结构与动画初始态
            var all = new UIElement().addClass("mbt-wallet-root");
            var pages = new UIElement().addClass("mbt-wallet-pages");
            var cover = createCover();
            var bankCardListPage = createLeftPage(player);
            var leftPage = bankCardListPage.page();
            var rightPage = createRightPage(player, bankCardListPage);

            pages.addChildren(leftPage, rightPage);
            all.addChildren(pages, cover);

            AtomicBoolean opened = new AtomicBoolean(false);
            cover.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button != PRIMARY_MOUSE_BUTTON || !opened.compareAndSet(false, true)) return;
                event.stopPropagation();
                openWallet(cover, leftPage, rightPage);
            });

            var ui = UI.of(all, WalletUIStylesheets.createWalletStylesheets());

            return p -> ModularUI.of(ui, p);
        });
    }

    // ==============================================
    // UI 构建
    // ==============================================

    /** 创建钱包单页；transform 为折叠初态，须 INLINE。 */
    private static UIElement createPage(Transform2D transform) {
        return new UIElement()
                .addClass("mbt-wallet-page")
                .style(s -> s.transform2D(transform))
                .setActive(false);
    }

    /** 创建钱包封面与打开提示文字。 */
    private static UIElement createCover() {
        var prompt = new TextElement()
                .setText(Component.translatable("ui.maple_banktrade.wallet.open"))
                .addClass("mbt-wallet-cover-prompt");
        // startCoverPromptBreathing(prompt, true);

        return new UIElement()
                .addClass("mbt-wallet-cover")
                .style(s -> s.transform2D(closedCoverTransform()))
                .addChild(prompt);
    }

    /** 创建左页银行卡列表，内容由服务端 Tag 同步后在客户端重建。 */
    private static BankCardListPage createLeftPage(Player player) {
        UIElement page = createPage(closedLeftPageTransform());
        ScrollerView list = new ScrollerView();
        list.addClass("mbt-wallet-card-list");
        list.viewContainer(container -> container.addClass("mbt-wallet-card-list-container"));

        BankType initialBankType = getBankTypeForIndex(player, 0);
        BankCardListPage bankCardListPage = new BankCardListPage(
                page,
                list,
                new ResourceLocation[] { initialBankType == null ? null : initialBankType.id() },
                new int[] { 0 });
        list.onMessage("select_card", payload -> selectCard(player, bankCardListPage, payload));
        page.addChild(list);
        showBankCardList(initialBankType, bankCardListPage);
        page.addChild(createBankCardListSync(player, bankCardListPage));
        return bankCardListPage;
    }

    /** 创建右页银行列表与分页控制。 */
    private static UIElement createRightPage(Player player, BankCardListPage bankCardListPage) {
        var rightPage = createPage(closedRightPageTransform());
        final int[] currentPage = { 0 };
        rightPage.addEventListener(UIEvents.MOUSE_WHEEL, event -> {
            if (event.deltaY != 0) {
                event.stopPropagation();
            }
        });
        rightPage.addServerEventListener(UIEvents.MOUSE_WHEEL, event -> {
            if (event.deltaY == 0) return;
            updatePage(event.deltaY < 0 ? 1 : -1, player, currentPage);
        });

        var bankInfos = new UIElement().addClass("mbt-bank-list");
        bankInfos.layout(l -> l.alignContent(AlignContent.SPACE_BETWEEN).justifyContent(AlignContent.SPACE_AROUND));

        IntSupplier startIndexSupplier = () -> {
            currentPage[0] = clampPage(currentPage[0], getPageCount(player));
            return currentPage[0] * BANK_UI_SLOT_COUNT;
        };
        for (int i = 0; i < BANK_UI_SLOT_COUNT; i++) {
            bankInfos.addChild(createBankSlotUI(player, startIndexSupplier, i, bankCardListPage));
        }

        TextElement pageNumber = new TextElement();
        pageNumber.addClass("mbt-bank-page-label");
        pageNumber.setText(formatPageNumber(currentPage[0], getPageCount(player)), false);
        UIElement turnPage = new UIElement().addClass("mbt-bank-turn-page");
        turnPage.addChildren(
                createPageButton("←", -1, player, currentPage),
                pageNumber,
                createPageButton("→", 1, player, currentPage));

        rightPage.addChildren(bankInfos, turnPage, createPageNumberSync(player, currentPage, pageNumber));
        return rightPage;
    }

    /** 创建银行槽位并绑定银行类型与持卡数量。 */
    private static UIElement createBankSlotUI(Player player, IntSupplier startIndexSupplier, int slotIndex, BankCardListPage bankCardListPage) {
        SlotInfo initialSlotInfo = getSlotInfo(player, startIndexSupplier.getAsInt() + slotIndex);
        UIElement bank = new UIElement().addClass("mbt-bank-slot");
        applyBankSlotState(bank, initialSlotInfo);
        bank.addChild(BankInfo.createTransparentButton(
                button -> button.setOnServerClick(ignored -> showBankCardList(getSlotInfo(player, startIndexSupplier.getAsInt() + slotIndex).bankType(), bankCardListPage))));

        BindableValue<String> slotSync = new BindableValue<>(initialSlotInfo.toSyncValue());
        slotSync.addClass("mbt-ui-sync-hidden");
        slotSync.setDisplay(false);
        slotSync.bind(DataBindingBuilder.stringS2C(() -> getSlotInfo(player, startIndexSupplier.getAsInt() + slotIndex).toSyncValue())
                .name("bank_slot_" + slotIndex)
                .initialValue(initialSlotInfo.toSyncValue())
                .remoteSetter(syncValue -> applyBankSlotState(bank, SlotInfo.fromSyncValue(syncValue)))
                .build());

        return bank.addChild(slotSync);
    }

    /** 根据槽位同步数据刷新样式。 */
    private static void applyBankSlotState(UIElement bank, SlotInfo slotInfo) {
        bank.style(style -> applyBankSlotStyle(style, slotInfo));
    }

    /** 应用银行背景、数量提示或空槽位样式。 */
    private static void applyBankSlotStyle(com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle style, SlotInfo slotInfo) {
        if (slotInfo.hasBankCards()) {
            BankInfo.of(slotInfo.bankType()).applyBankStyle(style, slotInfo.cardCount());
        } else {
            BankInfo.applyEmptyBankStyle(style);
        }
    }

    // ==============================================
    // 状态
    // ==============================================

    /** 切换左页选中银行，并递增修订号触发列表刷新。 */
    private static void showBankCardList(BankType bankType, BankCardListPage bankCardListPage) {
        bankCardListPage.selectedBankTypeId()[0] = bankType == null ? null : bankType.id();
        bankCardListPage.listRevision()[0]++;
    }

    // ==============================================
    // 同步绑定
    // ==============================================

    /** 绑定当前银行下的银行卡列表快照。 */
    private static BindableValue<Tag> createBankCardListSync(Player player, BankCardListPage bankCardListPage) {
        Tag initialValue = getBankCardListTag(player, bankCardListPage.selectedBankTypeId()[0], bankCardListPage.listRevision()[0]);
        BindableValue<Tag> sync = new BindableValue<>(initialValue);
        sync.addClass("mbt-ui-sync-hidden");
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.tagS2C(() -> getBankCardListTag(player, bankCardListPage.selectedBankTypeId()[0], bankCardListPage.listRevision()[0]))
                .name("bank_card_list")
                .initialValue(initialValue)
                .remoteSetter(tag -> rebuildBankCardList(player, bankCardListPage, tag))
                .build());
        return sync;
    }

    /** 绑定右页页码文本。 */
    private static BindableValue<String> createPageNumberSync(Player player, int[] currentPage, TextElement pageNumber) {
        String initialValue = formatPageNumber(currentPage[0], getPageCount(player));
        BindableValue<String> pageNumberSync = new BindableValue<>(initialValue);
        pageNumberSync.addClass("mbt-ui-sync-hidden");
        pageNumberSync.setDisplay(false);
        pageNumberSync.bind(DataBindingBuilder.stringS2C(() -> {
            int pageCount = getPageCount(player);
            currentPage[0] = clampPage(currentPage[0], pageCount);
            return formatPageNumber(currentPage[0], pageCount);
        })
                .name("bank_page_number")
                .initialValue(initialValue)
                .remoteSetter(text -> pageNumber.setText(text == null ? "" : text, false))
                .build());
        return pageNumberSync;
    }

    // ==============================================
    // 同步数据
    // ==============================================

    /** 在服务端构建指定银行下可用银行卡的客户端只读快照。 */
    private static Tag getBankCardListTag(Player player, ResourceLocation bankTypeId, int revision) {
        CompoundTag root = new CompoundTag();
        ListTag cards = new ListTag();
        root.put(BANK_CARD_LIST_KEY, cards);
        root.putString(BANK_CARD_LIST_BANK_KEY, bankTypeId == null ? "" : bankTypeId.toString());
        root.putInt(BANK_CARD_LIST_COUNT_KEY, 0);
        root.putInt(BANK_CARD_SYNC_REVISION_KEY, revision);
        BankCardsWorldData data = getServerBankCards(player);
        if (bankTypeId == null || data == null) return root;
        BankType bankType = BankType.requireById(bankTypeId);
        if (bankType == null) return root;
        // getCardsForPlayerInBank 已按 canUse 过滤
        data.getCardsForPlayerInBank(BankHelper.getUuid(player), bankType).stream()
                .sorted()
                .forEach(cardUuid -> {
                    BankCard card = data.getCard(cardUuid);
                    if (cardInfoOf(card) != null) {
                        cards.add(card.toClientTag(data.getPermission(BankHelper.getUuid(player), cardUuid)));
                    }
                });
        root.putInt(BANK_CARD_LIST_COUNT_KEY, cards.size());
        return root;
    }

    // ==============================================
    // 客户端重建
    // ==============================================

    /** 客户端收到列表快照后清空并重建左页列表。 */
    private static void rebuildBankCardList(Player player, BankCardListPage bankCardListPage, Tag tag) {
        ScrollerView list = bankCardListPage.list();
        list.clearAllScrollViewChildren();
        list.verticalScroller.setValue(0f);
        if (!(tag instanceof CompoundTag root) || !(root.get(BANK_CARD_LIST_KEY) instanceof ListTag cards)) return;
        for (Tag cardTag : cards) {
            BankCard card = BankCard.fromClientTag(cardTag);
            // 服务端只下发 canUse 卡；此处再滤损坏/无 UI 注册的快照
            if (card == null || !card.canClientUse() || cardInfoOf(card) == null) continue;
            list.addScrollViewChild(createClientCardListButton(player, bankCardListPage, card));
        }
    }

    /** 创建左页列表中的单张银行卡按钮。 */
    private static Button createClientCardListButton(Player player, BankCardListPage bankCardListPage, BankCard card) {
        CardInfo info = cardInfoOf(card);
        Button button = new Button().setText("", false);
        button.addClass("mbt-bank-card-list-button");
        // 列表项尺寸见 bank_card.lss；贴图按卡类型动态注入
        button.buttonStyle(style -> {
            IGuiTexture background = info.simplifiedButtonBackground();
            style.baseTexture(background).hoverTexture(background).pressedTexture(background);
        });
        button.noText();
        button.addChild(info.simplifiedUIFactory().create(player, card));
        button.setOnClick(ignored -> {
            CompoundTag payload = new CompoundTag();
            payload.putString("card_uuid", card.getCardUuid().toString());
            bankCardListPage.list().sendMessage("select_card", payload);
        });
        return button;
    }

    // ==============================================
    // 事件
    // ==============================================

    /** 处理点击银行卡；权限由 {@link BankCardDetailUIRegistration#openUI} 统一校验。 */
    private static void selectCard(Player player, BankCardListPage bankCardListPage, CompoundTag payload) {
        String cardUuid = (payload.contains("card_uuid") ? payload.getString("card_uuid") : "");
        try {
            UUID uuid = UUID.fromString(cardUuid);
            BankCardsWorldData data = getServerBankCards(player);
            if (data == null || !(player instanceof ServerPlayer serverPlayer)) return;
            BankCard card = data.getCard(uuid);
            ResourceLocation selectedBankTypeId = bankCardListPage.selectedBankTypeId()[0];
            if (card == null || selectedBankTypeId == null || !selectedBankTypeId.equals(card.getBankTypeId())) {
                return;
            }
            BankCardDetailUIRegistration.openUI(serverPlayer, uuid);
        } catch (IllegalArgumentException ignored) {}
    }

    // ==============================================
    // 分页与查询
    // ==============================================

    /** 创建右页底部翻页按钮。 */
    private static Button createPageButton(String text, int delta, Player player, int[] currentPage) {
        Button button = new Button().setText(text, false);
        button.addClass("mbt-bank-page-button");
        button.setOnServerClick(ignored -> updatePage(delta, player, currentPage));
        return button;
    }

    /** 根据滚轮或按钮更新银行分页页码（首尾循环）。 */
    private static void updatePage(int delta, Player player, int[] currentPage) {
        int pageCount = getPageCount(player);
        currentPage[0] = (currentPage[0] + delta) % pageCount;
        if (currentPage[0] < 0) currentPage[0] += pageCount;
    }

    /** 计算右页银行列表总页数，至少保留一页。 */
    private static int getPageCount(Player player) {
        BankCardsWorldData data = getServerBankCards(player);
        int bankCount = data == null ? 0 : data.getBankTypesForPlayer(BankHelper.getUuid(player)).size();
        return Math.max(1, (bankCount + BANK_UI_SLOT_COUNT - 1) / BANK_UI_SLOT_COUNT);
    }

    /** 查询指定银行槽位的银行类型与可用卡数量。 */
    private static SlotInfo getSlotInfo(Player player, int bankIndex) {
        BankType bankType = getBankTypeForIndex(player, bankIndex);
        BankCardsWorldData data = getServerBankCards(player);
        if (bankType == null || data == null) {
            return new SlotInfo(bankType, 0);
        }
        Set<UUID> cardUUIDs = data.getCardsForPlayerInBank(BankHelper.getUuid(player), bankType);
        return new SlotInfo(bankType, cardUUIDs.size());
    }

    /** 按注册顺序查询玩家拥有银行卡的银行类型。 */
    private static BankType getBankTypeForIndex(Player player, int bankIndex) {
        BankCardsWorldData data = getServerBankCards(player);
        if (bankIndex < 0 || data == null) return null;
        List<ResourceLocation> bankTypeIds = data.getBankTypesForPlayer(BankHelper.getUuid(player));
        return bankIndex < bankTypeIds.size() ? BankType.requireById(bankTypeIds.get(bankIndex)) : null;
    }

    /** 判断玩家是否在服务端环境可访问银行数据。 */
    private static boolean hasServer(Player player) {
        return player != null && !player.level().isClientSide() && player.level().getServer() != null;
    }

    /** 安全获取服务端银行世界数据。 */
    private static BankCardsWorldData getServerBankCards(Player player) {
        return hasServer(player) ? MBTBankStates.getBankCards(player.level().getServer()) : null;
    }

    /** 已注册的 CardInfo；空卡或未注册返回 null。 */
    private static CardInfo cardInfoOf(BankCard card) {
        return card == null ? null : CardInfo.of(card.getNameIndex());
    }

    /** 把页码限制在合法范围内。 */
    private static int clampPage(int page, int pageCount) {
        if (page < 0) return 0;
        return Math.min(page, pageCount - 1);
    }

    /** 格式化右页底部页码文本。 */
    private static String formatPageNumber(int currentPage, int pageCount) {
        return (clampPage(currentPage, pageCount) + 1) + "/" + pageCount;
    }

    // ==============================================
    // 内部类型
    // ==============================================

    /** 左页银行卡列表运行状态；数组字段用于在 lambda 中持有可变值。 */
    private record BankCardListPage(UIElement page, ScrollerView list,
                                    ResourceLocation[] selectedBankTypeId, int[] listRevision) {}

    /** 右页银行槽位的轻量同步数据。 */
    private record SlotInfo(BankType bankType, int cardCount) {

        /** 是否有可用银行卡。 */
        private boolean hasBankCards() {
            return bankType != null && cardCount > 0;
        }

        /** 编码为同步字符串。 */
        private String toSyncValue() {
            return (bankType == null ? "" : bankType.id().toString()) + BANK_SLOT_SYNC_SEPARATOR + cardCount;
        }

        /** 从同步字符串解码。 */
        private static SlotInfo fromSyncValue(String syncValue) {
            if (syncValue == null || syncValue.isEmpty()) return new SlotInfo(null, 0);
            int separatorIndex = syncValue.lastIndexOf(BANK_SLOT_SYNC_SEPARATOR);
            if (separatorIndex < 0) return new SlotInfo(resolveBankType(syncValue), 0);
            return new SlotInfo(resolveBankType(syncValue.substring(0, separatorIndex)),
                    parseCardCount(syncValue.substring(separatorIndex + BANK_SLOT_SYNC_SEPARATOR.length())));
        }

        /** 解析银行类型 ID。 */
        private static BankType resolveBankType(String bankTypeId) {
            try {
                return bankTypeId == null || bankTypeId.isEmpty() ? null : BankType.requireById(ResourceLocation.parse(bankTypeId));
            } catch (RuntimeException ignored) {
                return null;
            }
        }

        /** 解析持卡数量。 */
        private static int parseCardCount(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
    }

    // ==============================================
    // 动画
    // ==============================================

    /** 播放钱包打开动画：右页先展开，封面翻转后左页展开。 */
    private static void openWallet(UIElement cover, UIElement leftPage, UIElement rightPage) {
        leftPage.setActive(true);
        rightPage.setActive(true);
        cover.setActive(false);

        rightPage.animation()
                .duration(WALLET_FLIP_HALF_DURATION)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, new Transform2D())
                .style(PropertyRegistry.OPACITY, 1f)
                .start();

        cover.animation()
                .duration(WALLET_FLIP_HALF_DURATION)
                .ease(Eases.SINE_IN_OUT)
                .style(PropertyRegistry.TRANSFORM_2D, FloatObjectPair.of(1f, turningCoverTransform()))
                .onFinished(ignored -> {
                    cover.setDisplay(false);
                    leftPage.animation()
                            .duration(WALLET_FLIP_HALF_DURATION)
                            .ease(Eases.SINE_IN_OUT)
                            .style(PropertyRegistry.TRANSFORM_2D, openedLeftPageTransform())
                            .style(PropertyRegistry.OPACITY, 1f)
                            .start();
                })
                .start();
    }

    /** 循环播放封面提示文字的淡入淡出。 */
    private static void startCoverPromptBreathing(UIElement prompt, boolean fadeIn) {
        prompt.animation(animation -> {
            if (!isActiveAndDisplayedInTree(prompt)) return;
            animation.duration(WALLET_PROMPT_BREATH_DURATION)
                    .ease(Eases.SINE_IN_OUT)
                    .style(PropertyRegistry.OPACITY, fadeIn ? WALLET_PROMPT_MAX_OPACITY : 0f)
                    .onFinished(element -> startCoverPromptBreathing(element, !fadeIn))
                    .start();
        });
    }

    /** 检查元素及其父级是否仍可显示，避免隐藏后继续排动画。 */
    private static boolean isActiveAndDisplayedInTree(UIElement element) {
        if (!element.isActive() || !element.isDisplayed()) return false;
        var parent = element.getParent();
        return parent == null || isActiveAndDisplayedInTree(parent);
    }

    // ==============================================
    // 变换
    // ==============================================

    /** 左页折叠初态。 */
    private static Transform2D closedLeftPageTransform() {
        return new Transform2D()
                .pivot(1f, 0.5f)
                .scale(0.04f, 1.04f);
    }

    /** 左页展开态。 */
    private static Transform2D openedLeftPageTransform() {
        return new Transform2D()
                .pivot(1f, 0.5f);
    }

    /** 右页折叠初态。 */
    private static Transform2D closedRightPageTransform() {
        return new Transform2D()
                .translate(-WALLET_PAGE_WIDTH / 2f, 0)
                .scale(0.98f, 1f);
    }

    /** 封面初始态。 */
    private static Transform2D closedCoverTransform() {
        return new Transform2D()
                .pivot(0.5f, 0.5f);
    }

    /** 封面翻转中间态。 */
    private static Transform2D turningCoverTransform() {
        return new Transform2D()
                .pivot(0.5f, 0.5f)
                .scale(0.04f, 1.04f);
    }
}

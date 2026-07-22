package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardPermission;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 银行卡详情权限管理浮层；布局见 bank_card.lss（.mbt-card-perm-*）。
 */
public final class BankCardPermissionPanel {

    // ==============================================
    // 常量
    // ==============================================

    /** 删除卡所需确认点击次数。 */
    private static final int DELETE_CLICKS = 3;

    /** 同步 Tag 字段键。 */
    private static final String K_REV = "revision", K_ACTOR = "actor", K_HOLDERS = "holders", K_GRANTS = "grants";
    private static final String K_UUID = "uuid", K_NAME = "name", K_PERM = "perm";
    /** 面板消息名。 */
    private static final String M_GRANT = "grant", M_REVOKE = "revoke", M_DELETE = "delete", M_REFRESH = "refresh";

    /** 禁止实例化。 */
    private BankCardPermissionPanel() {}

    // ==============================================
    // 挂载
    // ==============================================

    /** 若玩家可管理该卡，则在详情页挂载权限管理浮层。 */
    public static void attachIfManager(UIElement detail, Player player, BankCard card) {
        if (detail == null || card == null || !access(player, card, BankCardPermission::canManage, BankCardsWorldData::canManage)) {
            return;
        }

        UUID cardId = card.getCardUuid();
        int[] rev = { 0 }, delClicks = { 0 };
        Button[] delBtn = { null };

        UIElement panel = new UIElement().addClass("mbt-card-perm-panel");
        panel.setDisplay(false);
        ScrollerView grants = scrollList(), holders = scrollList();

        // 消息挂 panel，避免列表重建丢 handler
        panel.onMessage(M_GRANT, p -> mutate(player, p, rev, (data, target) -> data.grantPermission(
                BankHelper.getUuid(player), target, cardId,
                BankCardPermission.bySerializedName(p.getStringOr(K_PERM, "usable")))));
        panel.onMessage(M_REVOKE, p -> mutate(player, p, rev,
                (data, target) -> data.revokeManagedPermission(BankHelper.getUuid(player), target, cardId)));
        // 删除：服务端计数并执行；文案在客户端 setOnClick 中逐步更新
        panel.onMessage(M_DELETE, _ -> handleDelete(player, cardId, delClicks));
        panel.onMessage(M_REFRESH, _ -> {
            rev[0]++;
            delClicks[0] = 0; // 重开面板时重置服务端确认进度
        });

        // 关闭钮放 header 行；按钮视觉由 LSS 控制
        Button close = button(Component.literal("×"), "mbt-card-perm-close");
        close.setOnClick(_ -> {
            panel.setDisplay(false);
            resetDelete(delBtn, delClicks);
        });

        UIElement header = new UIElement().addClass("mbt-card-perm-header");
        header.addChild(label(Component.translatable("ui.bank.card.manage.title"), "mbt-card-perm-title"));

        UIElement headerActions = new UIElement().addClass("mbt-card-perm-header-actions");
        if (access(player, card, BankCardPermission::isOwner, BankCardsWorldData::isOwner)) {
            delBtn[0] = button(Component.translatable("ui.bank.card.manage.delete"), "mbt-card-perm-manage-button");
            // 客户端逐步改文案；每次点击仍发消息给服务端累计确认
            delBtn[0].setOnClick(_ -> {
                delClicks[0]++;
                applyDeleteButtonLabel(delBtn[0], delClicks[0]);
                panel.sendMessage(M_DELETE, new CompoundTag());
            });
            headerActions.addChild(delBtn[0]);
        }
        header.addChild(headerActions);

        // close 最后加入 + LSS absolute/z-index，保证盖住 header 可点
        panel.addChildren(
                header,
                label(Component.translatable("ui.bank.card.manage.grant"), "mbt-card-perm-section"),
                grants,
                label(Component.translatable("ui.bank.card.manage.holders"), "mbt-card-perm-section"),
                holders,
                close);

        // 同步挂 detail，避免 panel display:false 时绑定不更新
        Tag initial = buildSync(player, cardId, rev[0]);
        BindableValue<Tag> sync = new BindableValue<>(initial);
        sync.addClass("mbt-ui-sync-hidden");
        sync.setDisplay(false);
        sync.bind(DataBindingBuilder.tagS2C(() -> buildSync(player, cardId, rev[0]))
                .name("card_perm_" + cardId.toString().replace('-', '_'))
                .initialValue(initial)
                .remoteSetter(tag -> applySync(panel, grants, holders, tag))
                .build());
        applySync(panel, grants, holders, initial);

        Button manage = button(Component.translatable("ui.bank.card.manage.open"), "mbt-card-perm-open-button");
        manage.setOnClick(_ -> {
            boolean open = !panel.isDisplayed();
            panel.setDisplay(open);
            if (open) {
                resetDelete(delBtn, delClicks);
                panel.sendMessage(M_REFRESH, new CompoundTag());
            }
        });
        // panel 后于 manage 添加，叠层在上
        detail.addChildren(manage, panel, sync);
    }

    // ==============================================
    // 同步
    // ==============================================

    /** 构建权限同步 Tag：grants = 权限表 ∪ 在线；无记录 → UNUSABLE。 */
    private static Tag buildSync(Player player, UUID cardId, int revision) {
        CompoundTag root = new CompoundTag();
        root.putInt(K_REV, revision);
        root.putString(K_ACTOR, BankCardPermission.UNUSABLE.getSerializedName());
        root.put(K_HOLDERS, new ListTag());
        root.put(K_GRANTS, new ListTag());
        if (!serverPlayer(player) || cardId == null) return root;

        MinecraftServer server = player.level().getServer();
        BankCardsWorldData data = MBTBankStates.getBankCards(server);
        root.putString(K_ACTOR, data.getPermission(BankHelper.getUuid(player), cardId).getSerializedName());
        root.put(K_HOLDERS, entryList(server, data.getUsablePermissionsForCard(cardId).entrySet().stream()));

        Set<UUID> targets = new LinkedHashSet<>(data.getAllPlayersInPermissionTable());
        targets.addAll(data.getPermissionsForCard(cardId).keySet());
        if (server != null) server.getPlayerList().getPlayers().forEach(p -> targets.add(BankHelper.getUuid(p)));
        root.put(K_GRANTS, entryList(server, targets.stream()
                .map(id -> Map.entry(id, orUnusable(data.getPermission(id, cardId))))));
        return root;
    }

    /** 将玩家-权限条目排序后写入 ListTag。 */
    private static ListTag entryList(MinecraftServer server, Stream<Map.Entry<UUID, BankCardPermission>> stream) {
        ListTag list = new ListTag();
        stream.sorted(Map.Entry.<UUID, BankCardPermission>comparingByValue(BankCardPermission::compareRankDesc)
                .thenComparing(e -> playerLabel(server, e.getKey()), String.CASE_INSENSITIVE_ORDER))
                .forEach(e -> {
                    CompoundTag t = new CompoundTag();
                    t.putString(K_UUID, e.getKey().toString());
                    t.putString(K_NAME, playerLabel(server, e.getKey()));
                    t.putString(K_PERM, orUnusable(e.getValue()).getSerializedName());
                    list.add(t);
                });
        return list;
    }

    /** 根据同步 Tag 重建授予列表与持有者列表。 */
    private static void applySync(UIElement host, ScrollerView grants, ScrollerView holders, Tag tag) {
        grants.clearAllScrollViewChildren();
        holders.clearAllScrollViewChildren();
        if (!(tag instanceof CompoundTag root)) return;
        BankCardPermission actor = BankCardPermission.bySerializedName(
                root.getStringOr(K_ACTOR, BankCardPermission.UNUSABLE.getSerializedName()));
        fill(grants, root, K_GRANTS, e -> row(host, actor, e, true));
        fill(holders, root, K_HOLDERS, e -> row(host, actor, e, false));
    }

    /** 将 ListTag 中的条目填入滚动列表。 */
    private static void fill(ScrollerView list, CompoundTag root, String key,
                             java.util.function.Function<CompoundTag, UIElement> factory) {
        Tag raw = root.get(key);
        ListTag entries = raw instanceof ListTag l ? l : root.getListOrEmpty(key);
        for (Tag t : entries) {
            if (t instanceof CompoundTag e) list.addScrollViewChild(factory.apply(e));
        }
    }

    // ==============================================
    // 行 UI
    // ==============================================

    /** 构建授予/持有者列表中的单行。 */
    private static UIElement row(UIElement host, BankCardPermission actor, CompoundTag entry, boolean grant) {
        String uuid = entry.getStringOr(K_UUID, "");
        String name = entry.getStringOr(K_NAME, "?");
        BankCardPermission perm = BankCardPermission.bySerializedName(
                entry.getStringOr(K_PERM, BankCardPermission.UNUSABLE.getSerializedName()));

        UIElement actions = new UIElement().addClass("mbt-card-perm-actions");
        if (grant) {
            if (!perm.isOwner()) {
                if (actor.isOwner()) {
                    actions.addChild(grantBtn(host, uuid, BankCardPermission.USABLE));
                    actions.addChild(grantBtn(host, uuid, BankCardPermission.ADMIN));
                } else if (actor == BankCardPermission.ADMIN && !perm.canManage()) {
                    actions.addChild(grantBtn(host, uuid, BankCardPermission.USABLE));
                }
            }
        } else if (canRevoke(actor, perm)) {
            Button revoke = button(
                    Component.translatable("ui.bank.card.manage.revoke"),
                    "mbt-card-perm-action-button");
            revoke.setOnClick(_ -> host.sendMessage(M_REVOKE, uuidTag(uuid)));
            actions.addChild(revoke);
        }

        UIElement shell = new UIElement().addClass("mbt-card-perm-row");
        shell.addChildren(
                label(name, "mbt-card-perm-name"),
                label(Component.translatable(orUnusable(perm).getTranslationKey()).getString(), "mbt-card-perm-perm"),
                actions);
        return shell;
    }

    /** 创建授予指定权限的按钮。 */
    private static Button grantBtn(UIElement host, String targetUuid, BankCardPermission grant) {
        boolean admin = grant == BankCardPermission.ADMIN;
        Button b = button(
                Component.translatable(admin ? "ui.bank.card.manage.grant_admin" : "ui.bank.card.manage.grant_usable"),
                "mbt-card-perm-action-button");
        b.setOnClick(_ -> {
            CompoundTag p = uuidTag(targetUuid);
            p.putString(K_PERM, grant.getSerializedName());
            host.sendMessage(M_GRANT, p);
        });
        return b;
    }

    /** 判断 actor 是否可撤销 target 的权限。 */
    private static boolean canRevoke(BankCardPermission actor, BankCardPermission target) {
        if (target == null || !target.canUse() || target.isOwner()) return false;
        return actor != null && (actor.isOwner() || (actor == BankCardPermission.ADMIN && target == BankCardPermission.USABLE));
    }

    // ==============================================
    // 服务端操作
    // ==============================================

    /** 解析目标 UUID 并在服务端修改权限，成功后递增 revision。 */
    private static void mutate(Player player, CompoundTag payload, int[] rev,
                               BiConsumer<BankCardsWorldData, UUID> action) {
        if (!serverPlayer(player) || payload == null) return;
        UUID target = parseUuid(payload.getStringOr(K_UUID, ""));
        if (target == null) return;
        MBTBankStates.modifyBankCards(player.level().getServer(), data -> action.accept(data, target));
        rev[0]++;
    }

    /**
     * 服务端：累计删除确认次数，达 {@link #DELETE_CLICKS} 后删卡。
     * 按钮文案由客户端 {@link #applyDeleteButtonLabel} 更新，勿在服务端 setText。
     */
    private static void handleDelete(Player player, UUID cardId, int[] clicks) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!MBTBankStates.getBankCards(player.level().getServer()).isOwner(BankHelper.getUuid(player), cardId)) return;

        // 服务端独立计数（与客户端 UI 计数同步：每点一次发一条 M_DELETE）
        if (++clicks[0] < DELETE_CLICKS) {
            return;
        }
        boolean[] ok = { false };
        MBTBankStates.modifyBankCards(sp.level().getServer(),
                data -> ok[0] = data.deleteCardAsOwner(BankHelper.getUuid(sp), cardId));
        if (ok[0]) {
            sp.closeContainer();
        } else {
            // 删卡失败时重置服务端计数；客户端文案靠下次打开面板 resetDelete
            clicks[0] = 0;
        }
    }

    /**
     * 按已点击次数更新删除按钮文案（须在客户端调用）。
     * 1→确认删除；2→再次确认；3→执行删除中。
     */
    private static void applyDeleteButtonLabel(Button deleteButton, int clicks) {
        if (deleteButton == null) return;
        int step = Math.clamp(clicks, 0, DELETE_CLICKS);
        if (step <= 0) {
            deleteButton.setText(Component.translatable("ui.bank.card.manage.delete"));
            return;
        }
        // 第 1/2/3 次各用独立文案，并带进度 (n/3)
        String key = switch (step) {
            case 1 -> "ui.bank.card.manage.delete_confirm_1";
            case 2 -> "ui.bank.card.manage.delete_confirm_2";
            default -> "ui.bank.card.manage.delete_confirm_3";
        };
        deleteButton.setText(Component.translatable(key, step, DELETE_CLICKS));
    }

    /** 重置删除确认计数与按钮文案（客户端 UI）。 */
    private static void resetDelete(Button[] delBtn, int[] clicks) {
        clicks[0] = 0;
        applyDeleteButtonLabel(delBtn[0], 0);
    }

    // ==============================================
    // UI 结构
    // ==============================================

    /** 创建权限列表面板滚动列表。 */
    private static ScrollerView scrollList() {
        ScrollerView list = new ScrollerView();
        list.addClass("mbt-card-perm-scroll");
        list.viewContainer(c -> c.addClass("mbt-card-perm-scroll-container"));
        return list;
    }

    /** 创建纯文本标签。 */
    private static TextElement label(String value, String cssClass) {
        return label(Component.literal(value == null ? "" : value), cssClass);
    }

    /** 创建带 CSS 类的文本元素。 */
    private static TextElement label(Component value, String cssClass) {
        TextElement e = new TextElement().setText(value);
        e.addClass(cssClass);
        return e;
    }

    /** 创建仅挂 class 的按钮（样式在 bank_card.lss）。 */
    private static Button button(Component label, String cssClass) {
        Button b = new Button().setText(label);
        if (cssClass != null) b.addClass(cssClass);
        return b;
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 构造仅含目标 UUID 的消息 Tag。 */
    private static CompoundTag uuidTag(String uuid) {
        CompoundTag t = new CompoundTag();
        t.putString(K_UUID, uuid);
        return t;
    }

    /** null 权限归一为 UNUSABLE。 */
    private static BankCardPermission orUnusable(BankCardPermission p) {
        return p == null ? BankCardPermission.UNUSABLE : p;
    }

    /** 是否为服务端玩家上下文。 */
    private static boolean serverPlayer(Player p) {
        return p instanceof ServerPlayer && !p.level().isClientSide();
    }

    /** 解析 UUID 字符串，失败返回 null。 */
    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 在线名优先，否则缓存名，再否则 UUID 前缀。 */
    private static String playerLabel(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return "?";
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getPlainTextName();
        return server.services().nameToIdCache().get(uuid).map(NameAndId::name).orElseGet(() -> {
            String s = uuid.toString();
            return s.length() > 8 ? s.substring(0, 8) : s;
        });
    }

    // ==============================================
    // 内部类型
    // ==============================================

    /** 服务端权限访问函数。 */
    @FunctionalInterface
    private interface CardAccess {

        boolean test(BankCardsWorldData data, UUID player, UUID card);
    }

    /** 客户端用快照、服务端用世界数据判断访问权。 */
    private static boolean access(Player player, BankCard card,
                                  Predicate<BankCardPermission> client,
                                  CardAccess server) {
        if (player == null || card == null) return false;
        if (player.level().isClientSide() || player.level().getServer() == null) {
            BankCardPermission p = card.getClientPermission();
            return p != null && client.test(p);
        }
        return server.test(MBTBankStates.getBankCards(player.level().getServer()),
                BankHelper.getUuid(player), card.getCardUuid());
    }
}

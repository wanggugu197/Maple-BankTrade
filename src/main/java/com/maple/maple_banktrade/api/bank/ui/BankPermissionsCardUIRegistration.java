package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.WalletApiRegistration;
import com.maple.maple_banktrade.api.bank.base.*;
import com.maple.maple_banktrade.api.bank.data.BankInfo;
import com.maple.maple_banktrade.api.bank.item.BankDataComponent;

import java.util.*;

/**
 * 权限卡构造器 UI：仅列出玩家为 OWNER / ADMIN 的卡；
 * 行文案为卡名，悬停显示银行名 / 权限 / UUID 等基础信息。
 * <p>
 * 交互走 root {@code onMessage}（勿对动态行用 setOnServerClick，避免 RPC id 错位）。
 * </p>
 */
public final class BankPermissionsCardUIRegistration {

    public static final Identifier BANK_PERMISSIONS_CARD_UI = MapleBankTrade.id("bank_permissions_card_ui");

    private static final String K_LEFT = "left", K_RIGHT = "right";
    private static final String K_UUID = "uuid", K_NAME = "name", K_BANK = "bank", K_PERM = "perm";
    private static final String M_ADD = "add", M_REMOVE = "remove", M_CLEAR = "clear", M_EXPORT = "export";

    private BankPermissionsCardUIRegistration() {}

    public static void init() {
        PlayerUIMenuType.register(BANK_PERMISSIONS_CARD_UI, player -> {
            LinkedHashSet<UUID> selected = new LinkedHashSet<>();
            if (!player.level().isClientSide()) {
                prefill(player, selected);
            }
            return p -> ModularUI.of(createUI(p, selected), p);
        });
    }

    private static UI createUI(Player player, LinkedHashSet<UUID> selected) {
        UIElement root = new UIElement().addClass("mbt-perm-card-page");
        ScrollerView left = scroll("mbt-perm-card-left");
        ScrollerView right = scroll("mbt-perm-card-right");
        @SuppressWarnings("unchecked")
        BindableValue<Tag>[] sync = new BindableValue[1];

        Runnable push = () -> {
            if (player.level().isClientSide() || sync[0] == null) return;
            Tag tag = buildSync(player, selected);
            sync[0].setValue(tag, true);
            paint(root, left, right, tag);
        };

        root.onMessage(M_ADD, tag -> {
            if (player.level().isClientSide()) return;
            UUID id = parseUuid(tag);
            // 仅 OWNER / ADMIN 可加入右侧
            if (id != null && isOwnerOrAdmin(player, id) && selected.add(id)) push.run();
        });
        root.onMessage(M_REMOVE, tag -> {
            if (player.level().isClientSide()) return;
            UUID id = parseUuid(tag);
            if (id != null && selected.remove(id)) push.run();
        });
        root.onMessage(M_CLEAR, _ -> {
            if (player.level().isClientSide() || selected.isEmpty()) return;
            selected.clear();
            push.run();
        });
        root.onMessage(M_EXPORT, _ -> exportCard(player, selected));

        Button clear = btn("ui.maple_banktrade.perm_card.clear", "mbt-perm-card-button");
        clear.setOnClick(_ -> root.sendMessage(M_CLEAR));
        Button export = btn("ui.maple_banktrade.perm_card.export", "mbt-perm-card-button");
        export.setOnClick(_ -> root.sendMessage(M_EXPORT));

        root.addChildren(
                label("ui.maple_banktrade.perm_card.title", "mbt-perm-card-title"),
                new UIElement().addClass("mbt-perm-card-columns").addChildren(
                        col("ui.maple_banktrade.perm_card.manageable_list", left),
                        col("ui.maple_banktrade.perm_card.selected_list", right)),
                new UIElement().addClass("mbt-perm-card-footer").addChildren(clear, export));

        Tag initial = buildSync(player, selected);
        BindableValue<Tag> bind = new BindableValue<>(initial);
        sync[0] = bind;
        bind.setDisplay(false);
        bind.bind(DataBindingBuilder.tagS2C(() -> buildSync(player, selected))
                .name("bank_perm_card_sync")
                .initialValue(initial)
                .remoteSetter(tag -> paint(root, left, right, tag))
                .build());
        root.addChild(bind);
        paint(root, left, right, initial);

        return UI.of(root, WalletUIStylesheets.createPermCardStylesheets());
    }

    // ---- layout helpers ----

    private static UIElement col(String sectionKey, ScrollerView list) {
        return new UIElement().addClass("mbt-perm-card-col")
                .addChildren(label(sectionKey, "mbt-perm-card-section"), list);
    }

    private static ScrollerView scroll(String clazz) {
        ScrollerView v = new ScrollerView();
        v.addClass(clazz);
        v.viewContainer(c -> c.addClass("mbt-perm-card-list-container"));
        return v;
    }

    private static Button btn(String langKey, String clazz) {
        Button b = new Button().setText(Component.translatable(langKey));
        b.addClass(clazz);
        return b;
    }

    private static TextElement label(String langKey, String clazz) {
        TextElement t = new TextElement().setText(Component.translatable(langKey));
        t.addClass(clazz);
        return t;
    }

    private static TextElement label(Component text) {
        TextElement t = new TextElement().setText(text);
        t.addClass("mbt-perm-card-name");
        return t;
    }

    // ---- sync / paint ----

    /**
     * 仅同步玩家为 OWNER / ADMIN 的卡。
     * 左：全部可管理且尚未选中；右：已选且仍可管理。
     */
    private static Tag buildSync(Player player, LinkedHashSet<UUID> selected) {
        CompoundTag root = new CompoundTag();
        root.put(K_LEFT, new ListTag());
        root.put(K_RIGHT, new ListTag());
        if (player.level().isClientSide() || player.level().getServer() == null) return root;

        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        UUID pid = BankHelper.getUuid(player);
        Map<UUID, BankCardPermission> perms = data.getPermissionsForPlayer(pid);

        ListTag left = new ListTag();
        // 左侧：OWNER/ADMIN 且尚未选中；按 uuid 排序保证稳定
        perms.entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().canManage())
                .filter(e -> !selected.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .forEach(e -> {
                    BankCard card = data.getCard(e.getKey());
                    if (card != null) left.add(entry(card, e.getValue()));
                });
        root.put(K_LEFT, left);

        // 右侧：保持 selected 顺序，且仍须为 OWNER/ADMIN
        ListTag right = new ListTag();
        for (UUID id : selected) {
            BankCardPermission perm = perms.get(id);
            if (perm == null || !perm.canManage()) continue;
            BankCard card = data.getCard(id);
            if (card != null) right.add(entry(card, perm));
        }
        root.put(K_RIGHT, right);
        return root;
    }

    private static CompoundTag entry(BankCard card, BankCardPermission perm) {
        CompoundTag t = new CompoundTag();
        t.putString(K_UUID, card.getCardUuid().toString());
        Identifier name = card.getNameIndex();
        t.putString(K_NAME, name == null ? "" : name.toString());
        Identifier bank = card.getBankTypeId();
        t.putString(K_BANK, bank == null ? "" : bank.toString());
        t.putString(K_PERM, perm == null ? BankCardPermission.UNUSABLE.getSerializedName() : perm.getSerializedName());
        return t;
    }

    private static void paint(UIElement host, ScrollerView left, ScrollerView right, Tag tag) {
        left.clearAllScrollViewChildren();
        right.clearAllScrollViewChildren();
        if (!(tag instanceof CompoundTag root)) return;
        fill(left, root.getListOrEmpty(K_LEFT), "ui.maple_banktrade.perm_card.empty_manageable",
                host, M_ADD, "ui.maple_banktrade.perm_card.add");
        fill(right, root.getListOrEmpty(K_RIGHT), "ui.maple_banktrade.perm_card.empty_selected",
                host, M_REMOVE, "ui.maple_banktrade.perm_card.remove");
    }

    private static void fill(
                             ScrollerView list,
                             ListTag entries,
                             String emptyKey,
                             UIElement host,
                             String msg,
                             String actionKey) {
        if (entries.isEmpty()) {
            list.addScrollViewChild(label(emptyKey, "mbt-perm-card-empty"));
            return;
        }
        for (Tag t : entries) {
            if (!(t instanceof CompoundTag e)) continue;
            String uuid = e.getStringOr(K_UUID, "");
            UIElement row = new UIElement().addClass("mbt-perm-card-row");
            TextElement name = label(cardName(e));
            name.style(s -> s.tooltips(buildTooltips(e)));
            Button action = btn(actionKey, "mbt-perm-card-row-button");
            CompoundTag payload = new CompoundTag();
            payload.putString(K_UUID, uuid);
            action.setOnClick(_ -> host.sendMessage(msg, payload));
            row.addChildren(name, action);
            list.addScrollViewChild(row);
        }
    }

    /** 主行：卡显示名。 */
    private static Component cardName(CompoundTag entry) {
        String raw = entry.getStringOr(K_NAME, "");
        if (raw.isEmpty()) return Component.translatable("ui.maple_banktrade.perm_card.unknown_card");
        Identifier id = Identifier.tryParse(raw);
        return id == null ? Component.literal(raw) : Component.translatable(BankCardFactory.getTranslationKey(id));
    }

    /** 悬停：银行名、卡名、权限、UUID。 */
    private static Component[] buildTooltips(CompoundTag entry) {
        return new Component[] {
                Component.translatable("ui.maple_banktrade.perm_card.tip.bank", bankName(entry)),
                Component.translatable("ui.maple_banktrade.perm_card.tip.card", cardName(entry)),
                Component.translatable("ui.maple_banktrade.perm_card.tip.perm",
                        Component.translatable(BankCardPermission.bySerializedName(
                                entry.getStringOr(K_PERM, "unusable")).getTranslationKey())),
                Component.translatable("ui.bank.card.detail.uuid", entry.getStringOr(K_UUID, "")),
        };
    }

    private static Component bankName(CompoundTag entry) {
        String raw = entry.getStringOr(K_BANK, "");
        if (raw.isEmpty()) return Component.translatable("ui.maple_banktrade.perm_card.unknown_bank");
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return Component.literal(raw);
        BankInfo info = BankInfo.of(BankType.requireById(id));
        return info == null ? Component.literal(id.toString()) : Component.translatable(BankInfo.getTranslationKey(info.type()));
    }

    // ---- server ----

    private static void exportCard(Player player, LinkedHashSet<UUID> selected) {
        if (!(player instanceof ServerPlayer sp)) return;
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        LinkedHashSet<UUID> valid = new LinkedHashSet<>();
        for (UUID id : selected) {
            if (data.canManage(BankHelper.getUuid(player), id) && data.getCard(id) != null) {
                valid.add(id);
            }
        }
        ItemStack stack = new ItemStack(WalletApiRegistration.BANK_PERMISSIONS_CARD.get());
        BankDataComponent.CARD_PERMISSIONS.set(stack, valid);
        sp.getInventory().placeItemBackInInventory(stack);
    }

    /** 仅 OWNER / ADMIN。 */
    private static boolean isOwnerOrAdmin(Player player, UUID cardId) {
        if (player.level().getServer() == null) return false;
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        return data.getCard(cardId) != null && data.canManage(BankHelper.getUuid(player), cardId);
    }

    /** 手持成品权限卡时预填（仅仍为 OWNER / ADMIN 的 UUID）。 */
    private static void prefill(Player player, LinkedHashSet<UUID> selected) {
        if (player.level().getServer() == null) return;
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        for (ItemStack stack : new ItemStack[] { player.getMainHandItem(), player.getOffhandItem() }) {
            if (!WalletApiRegistration.BANK_PERMISSIONS_CARD.is(stack)) continue;
            Set<UUID> from = BankDataComponent.CARD_PERMISSIONS.getOrDefault(stack, Set.of());
            if (from == null || from.isEmpty()) continue;
            for (UUID id : from) {
                if (data.canManage(BankHelper.getUuid(player), id) && data.getCard(id) != null) {
                    selected.add(id);
                }
            }
            return;
        }
    }

    private static UUID parseUuid(CompoundTag tag) {
        if (tag == null) return null;
        String s = tag.getStringOr(K_UUID, "");
        if (s.isEmpty()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.data.CardInfo;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * 独立银行卡详情 UI 注册；布局见 wallet.lss，背景由 {@link CardInfo} 注入。
 */
public final class BankCardDetailUIRegistration {

    // ==============================================
    // 常量
    // ==============================================

    /** 银行卡详情 UI 标识。 */
    public static final ResourceLocation BANK_CARD_DETAIL_UI = MapleBankTrade.id("bank_card_detail_ui");
    /** 详情打开参数的网络编解码器。 */
    private static final StreamCodec<RegistryFriendlyByteBuf, DetailPayload> DETAIL_PAYLOAD_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.cardUuid());
                buffer.writeNbt(payload.cardTag());
            },
            buffer -> new DetailPayload(buffer.readUUID(), readCardTag(buffer)));

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册参数化详情菜单。 */
    public static void init() {
        ParameterizedPlayerUIMenuType.register(BANK_CARD_DETAIL_UI, DETAIL_PAYLOAD_CODEC, BankCardDetailUIRegistration::createHolder);
    }

    // ==============================================
    // 打开
    // ==============================================

    /** 打开详情 UI；服务端校验 canUse 且已注册 CardInfo。 */
    public static boolean openUI(ServerPlayer player, UUID cardUuid) {
        BankCard card = resolveUsableCard(player, cardUuid);
        if (card == null) return false;
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        CompoundTag cardTag = card.toClientTag(data.getPermission(BankHelper.getUuid(player), cardUuid));
        return ParameterizedPlayerUIMenuType.openUI(player, BANK_CARD_DETAIL_UI, new DetailPayload(cardUuid, cardTag));
    }

    // ==============================================
    // UI 构建
    // ==============================================

    /** 组装详情页：卡面内容、物品栏与关闭按钮。 */
    private static UI createDetailUI(Player player, UUID cardUuid, CompoundTag cardTag) {
        BankCard card = resolveVisibleCard(player, cardUuid, cardTag);
        CardInfo info = card == null ? null : CardInfo.of(card.getNameIndex());

        ScrollerView scroller = new ScrollerView();
        scroller.addClass("mbt-wallet-detail-scroller");
        scroller.viewContainer(container -> container.addClass("mbt-wallet-detail-scroller-container"));
        UIElement inventory = new InventorySlots().addClass("mbt-wallet-detail-inventory");
        UIElement root = new UIElement().addClass("mbt-wallet-detail-page");

        if (info != null) {
            // 每张卡背景不同 → 必须 INLINE，不能进 LSS
            scroller.style(style -> style.backgroundTexture(info.detailBackground()));
            inventory.style(style -> style.backgroundTexture(info.detailBackground()));
            scroller.addScrollViewChild(info.detailsUIFactory().create(player, card));
        }

        Button closeButton = new Button().setText("×", false);
        closeButton.addClass("mbt-wallet-close-button");
        closeButton.setOnClick(ignored -> player.closeContainer());
        closeButton.setOnServerClick(ignored -> player.closeContainer());

        root.addChildren(scroller, inventory, closeButton);
        return UI.of(root, WalletUIStylesheets.createWalletStylesheets());
    }

    // ==============================================
    // 权限解析
    // ==============================================

    /** 服务端：canUse 且卡存在且有 CardInfo 时返回卡实体。 */
    private static BankCard resolveUsableCard(Player player, UUID cardUuid) {
        if (player == null || cardUuid == null || player.level().isClientSide() || player.level().getServer() == null) {
            return null;
        }
        BankCardsWorldData data = MBTBankStates.getBankCards(player.level().getServer());
        if (!data.canUse(BankHelper.getUuid(player), cardUuid)) return null;
        BankCard card = data.getCard(cardUuid);
        return card != null && CardInfo.of(card.getNameIndex()) != null ? card : null;
    }

    /** 服务端用 canUse；客户端用快照 canClientUse。 */
    private static BankCard resolveVisibleCard(Player player, UUID cardUuid, CompoundTag cardTag) {
        if (player == null || cardUuid == null) return null;
        if (!player.level().isClientSide() && player.level().getServer() != null) {
            return resolveUsableCard(player, cardUuid);
        }
        BankCard card = BankCard.fromClientTag(cardTag);
        return card != null && cardUuid.equals(card.getCardUuid()) && card.canClientUse() ? card : null;
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 创建详情菜单 Holder。 */
    private static ParameterizedPlayerUIMenuType.PlayerUIHolder createHolder(Player player, DetailPayload payload) {
        return new BankCardDetailUIHolder(payload.cardUuid(), payload.cardTag());
    }

    /** 读取卡快照 Tag，空则返回空 CompoundTag。 */
    private static CompoundTag readCardTag(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? new CompoundTag() : tag;
    }

    /** 禁止实例化。 */
    private BankCardDetailUIRegistration() {}

    // ==============================================
    // 内部类型
    // ==============================================

    /** 打开详情时携带的卡 UUID 与客户端快照。 */
    private record DetailPayload(UUID cardUuid, CompoundTag cardTag) {}

    /** 详情菜单 Holder：创建 UI、校验可用性、关闭后重开钱包。 */
    private record BankCardDetailUIHolder(UUID cardUuid, CompoundTag cardTag) implements ParameterizedPlayerUIMenuType.PlayerUIHolder {

        @Override
        public @NonNull ModularUI createUI(@NonNull Player player) {
            return ModularUI.of(createDetailUI(player, cardUuid, cardTag), player);
        }

        @Override
        public boolean isStillValid(@NonNull Player player) {
            return player.level().isClientSide() || resolveUsableCard(player, cardUuid) != null;
        }

        @Override
        public void onClosed(Player player) {
            if (player instanceof ServerPlayer serverPlayer && player.level().getServer() instanceof MinecraftServer server) {
                server.execute(() -> {
                    if (!serverPlayer.hasDisconnected()) {
                        PlayerUIMenuType.openUI(serverPlayer, WalletUIRegistration.WALLET_UI);
                    }
                });
            }
        }
    }
}

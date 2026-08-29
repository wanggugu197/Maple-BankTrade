package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardPermission;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;

import java.util.function.Consumer;

import static com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration.*;

/**
 * 卡面 UI 拼装；权限由列表同步 / openUI 把关，此处只负责展示。
 */
public final class BankCardInfoUI {

    // ==============================================
    // 常量
    // ==============================================

    /** 简化卡面文本缩放。 */
    private static final float TEXT_FONT_SCALE = 0.66f;

    // ==============================================
    // 卡面构建
    // ==============================================

    /** 创建简化卡面容器，并可选追加额外行。 */
    public static UIElement createSimplifiedCardUI(BankCard card, Consumer<UIElement> extraLines) {
        UIElement simplified = new UIElement()
                .layout(l -> l
                        .width(WALLET_PAGE_WIDTH - 2 * WALLET_PAGE_PADDING)
                        .heightAuto()
                        .paddingAll(WALLET_PAGE_PADDING));
        if (card == null) return simplified;
        if (extraLines != null) extraLines.accept(simplified);
        return simplified;
    }

    /** 创建详情卡面容器，并可选追加额外行。 */
    public static UIElement createDetailsCardUI(BankCard card, Consumer<UIElement> extraLines) {
        UIElement detail = new UIElement()
                .layout(l -> l
                        .width(BANK_CARD_DETAIL_CONTENT_WIDTH)
                        .paddingAll(WALLET_PAGE_PADDING));
        if (card == null) return detail;
        if (extraLines != null) extraLines.accept(detail);
        return detail;
    }

    /** 向简化卡面追加基础信息行（卡名）。 */
    public static void addSimplifiedBase(UIElement simplified, BankCard card) {
        simplified.addChild(addSimplifiedLine(getNameIndexName(card)));
    }

    /** 向详情卡面追加 UUID、银行/卡名/权限，以及管理者权限面板。 */
    public static void addDetailBase(Player player, UIElement detail, BankCard card) {
        detail.addChild(addDetailLine(Component.translatable("ui.bank.card.detail.uuid", card.getCardUuid().toString())));
        detail.addChild(addDetailLine(Component.empty()
                .append(getBankName(card.getBankTypeId())).append(" • ")
                .append(getNameIndexName(card)).append(" • ")
                .append(permissionName(player, card))));
        // 拥有者 / 管理员：权限管理子面板
        BankCardPermissionPanel.attachIfManager(detail, player, card);
    }

    // ==============================================
    // 文本行
    // ==============================================

    /** 创建简化卡面文本行。 */
    public static UIElement addSimplifiedLine(Component text) {
        return addLine(text, WALLET_PAGE_WIDTH - 4 * WALLET_PAGE_PADDING, TEXT_FONT_SCALE);
    }

    /** 创建详情卡面文本行。 */
    public static UIElement addDetailLine(Component text) {
        return addLine(text, BANK_CARD_DETAIL_CONTENT_WIDTH - 2 * WALLET_PAGE_PADDING, 1);
    }

    /** 创建指定宽度与缩放的文本行。 */
    public static UIElement addLine(Component text, int width, float scale) {
        return new TextElement()
                .setText(text)
                .textStyle(style -> style.fontSize(9 * scale)
                        .textAlignVertical(Vertical.CENTER)
                        .adaptiveHeight(true)
                        .textWrap(TextWrap.WRAP))
                .layout(l -> l.heightAuto().flexGrow(1));
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 获取卡名显示文本。 */
    private static Component getNameIndexName(BankCard card) {
        return Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(card.getNameIndex()));
    }

    /** 获取银行显示名称。 */
    private static Component getBankName(ResourceLocation bankTypeId) {
        BankInfo info = BankInfo.of(BankType.requireById(bankTypeId));
        return info == null ? Component.literal(bankTypeId.toString()) : Component.translatable(BankInfo.getBankInfoTranslationKey(info.type()));
    }

    /** 获取权限显示名称。 */
    private static Component permissionName(Player player, BankCard card) {
        return Component.translatable(resolvePermission(player, card).getTranslationKey());
    }

    /** 服务端读权威权限，客户端读快照权限。 */
    private static BankCardPermission resolvePermission(Player player, BankCard card) {
        if (card == null) return BankCardPermission.UNUSABLE;
        if (player != null && !player.level().isClientSide() && player.level().getServer() != null) {
            return MBTBankStates.getBankCards(player.level().getServer()).getPermission(BankHelper.getUuid(player), card.getCardUuid());
        }
        return card.getClientPermission() == null ? BankCardPermission.UNUSABLE : card.getClientPermission();
    }

    /** 禁止实例化。 */
    private BankCardInfoUI() {}
}

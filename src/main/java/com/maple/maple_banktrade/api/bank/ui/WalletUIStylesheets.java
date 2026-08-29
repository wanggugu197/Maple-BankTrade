package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.maple.maple_banktrade.MapleBankTrade;

import java.util.List;
import java.util.Objects;

/**
 * 钱包相关 UI 样式表加载工具。
 */
public final class WalletUIStylesheets {

    // ==============================================
    // 常量
    // ==============================================

    /** 钱包布局样式表。 */
    private static final ResourceLocation WALLET_STYLESHEET = MapleBankTrade.id("lss/wallet.lss");
    /** 银行槽位样式表。 */
    private static final ResourceLocation BANK_STYLESHEET = MapleBankTrade.id("lss/bank.lss");
    /** 银行卡样式表。 */
    private static final ResourceLocation BANK_CARD_STYLESHEET = MapleBankTrade.id("lss/bank_card.lss");
    /** 银行权限卡编辑 UI 样式表。 */
    private static final ResourceLocation BANK_PERMISSIONS_CARD_STYLESHEET = MapleBankTrade.id("lss/bank_permissions_card.lss");

    /** 钱包 UI 使用的样式表资源列表（含 MC 合并表）。 */
    private static final List<ResourceLocation> WALLET_STYLESHEET_LOCATIONS = List.of(
            StylesheetManager.MC_MERGED,
            WALLET_STYLESHEET,
            BANK_STYLESHEET,
            BANK_CARD_STYLESHEET,
            BANK_PERMISSIONS_CARD_STYLESHEET);

    /** 权限卡构造器 UI：仅合并表 + 本 UI LSS。 */
    private static final List<ResourceLocation> PERM_CARD_STYLESHEET_LOCATIONS = List.of(
            StylesheetManager.MC_MERGED,
            BANK_PERMISSIONS_CARD_STYLESHEET);

    // ==============================================
    // 查询
    // ==============================================

    /** 加载并返回钱包 UI 可用的样式表实例列表。 */
    public static List<Stylesheet> createWalletStylesheets() {
        return load(WALLET_STYLESHEET_LOCATIONS);
    }

    /** 权限卡构造器 UI 样式表。 */
    public static List<Stylesheet> createPermCardStylesheets() {
        return load(PERM_CARD_STYLESHEET_LOCATIONS);
    }

    private static List<Stylesheet> load(List<ResourceLocation> locations) {
        return locations.stream()
                .map(StylesheetManager.INSTANCE::getStylesheet)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 禁止实例化。 */
    private WalletUIStylesheets() {}
}

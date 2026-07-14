package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.resources.Identifier;

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
    private static final Identifier WALLET_STYLESHEET = MapleBankTrade.id("lss/wallet.lss");
    /** 银行槽位样式表。 */
    private static final Identifier BANK_STYLESHEET = MapleBankTrade.id("lss/bank.lss");
    /** 银行卡样式表。 */
    private static final Identifier BANK_CARD_STYLESHEET = MapleBankTrade.id("lss/bank_card.lss");

    /** 钱包 UI 使用的样式表资源列表（含 MC 合并表）。 */
    private static final List<Identifier> WALLET_STYLESHEET_LOCATIONS = List.of(
            StylesheetManager.MC_MERGED,
            WALLET_STYLESHEET,
            BANK_STYLESHEET,
            BANK_CARD_STYLESHEET);

    // ==============================================
    // 查询
    // ==============================================

    /** 加载并返回钱包 UI 可用的样式表实例列表。 */
    public static List<Stylesheet> createWalletStylesheets() {
        return WalletUIStylesheets.WALLET_STYLESHEET_LOCATIONS.stream()
                .map(StylesheetManager.INSTANCE::getStylesheet)
                .filter(Objects::nonNull)
                .toList();
    }

    /** 禁止实例化。 */
    private WalletUIStylesheets() {}
}

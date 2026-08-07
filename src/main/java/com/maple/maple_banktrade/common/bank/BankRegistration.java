package com.maple.maple_banktrade.common.bank;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import java.util.List;

import static com.maple.maple_banktrade.api.bank.data.BankInfo.getBankInfoTranslationKey;

/**
 * 内置银行类型与显示信息注册。
 */
public class BankRegistration {

    // ==============================================
    // 银行类型
    // ==============================================

    public static final BankType CENTRAL = BankType.register(MapleBankTrade.id("central"));
    public static final BankType AGRICULTURAL = BankType.register(MapleBankTrade.id("agricultural"));
    public static final BankType STARRY_SKY = BankType.register(MapleBankTrade.id("starry_sky"));
    public static final BankType NATURE = BankType.register(MapleBankTrade.id("nature"));
    public static final BankType MAGIC = BankType.register(MapleBankTrade.id("magic"));

    // ==============================================
    // 注册
    // ==============================================

    /** 注册内置银行显示信息。 */
    public static void init() {
        registerBankInfos();
    }

    /** 注册各银行的本地化显示信息。 */
    private static void registerBankInfos() {
        BankInfo.register(CENTRAL,
                List.of(MBTLangHandler.addLang("bank.maple_banktrade.central.desc.1", "面向自动化账户和标准银行卡的高效银行服务。", "Efficient banking services for automated accounts and standard bank cards.")),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/technology.png"));
        MBTLangHandler.addLang(getBankInfoTranslationKey(CENTRAL), "科技银行", "Technology Bank");

        BankInfo.register(AGRICULTURAL,
                List.of(MBTLangHandler.addLang("bank.maple_banktrade.agricultural.desc.1", "面向农业贸易与日常储蓄的银行。", "Banking for agricultural trade and everyday savings.")),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/agricultural.png"));
        MBTLangHandler.addLang(getBankInfoTranslationKey(AGRICULTURAL), "农业银行", "Agricultural Bank");

        BankInfo.register(STARRY_SKY,
                List.of(MBTLangHandler.addLang("bank.maple_banktrade.starry_sky.desc.1", "面向远途商贸流通与多货币交易的银行。", "Banking for long-range commerce, circulation, and multi-currency trade.")),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/starry_sky.png"));
        MBTLangHandler.addLang(getBankInfoTranslationKey(STARRY_SKY), "星空银行", "Starry Sky Bank");

        BankInfo.register(NATURE,
                List.of(MBTLangHandler.addLang("bank.maple_banktrade.nature.desc.1", "面向自然资源、生态贸易与基础储蓄的银行。", "Banking for natural resources, ecological trade, and basic savings.")),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/nature.png"));
        MBTLangHandler.addLang(getBankInfoTranslationKey(NATURE), "自然银行", "Nature Bank");

        BankInfo.register(MAGIC,
                List.of(MBTLangHandler.addLang("bank.maple_banktrade.magic.desc.1", "面向特殊资产、标记账户与稀有交易的银行。", "Banking for special assets, tagged accounts, and rare trades.")),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/magic.png"));
        MBTLangHandler.addLang(getBankInfoTranslationKey(MAGIC), "魔法银行", "Magic Bank");
    }
}

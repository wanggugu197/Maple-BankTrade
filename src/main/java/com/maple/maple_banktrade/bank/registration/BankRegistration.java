package com.maple.maple_banktrade.bank.registration;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;

/**
 * 内置银行类型与显示信息注册。
 */
public class BankRegistration {

    // ==============================================
    // 银行类型
    // ==============================================

    public static final BankType CENTRAL = BankType.BankTypeRegister(MapleBankTrade.id("central"));
    public static final BankType FARMERS = BankType.BankTypeRegister(MapleBankTrade.id("farmers"));
    public static final BankType MERCHANT = BankType.BankTypeRegister(MapleBankTrade.id("merchant"));
    public static final BankType NATURE = BankType.BankTypeRegister(MapleBankTrade.id("nature"));
    public static final BankType MAGIC = BankType.BankTypeRegister(MapleBankTrade.id("magic"));

    private static boolean bankInfosRegistered;

    // ==============================================
    // 注册
    // ==============================================

    /** 注册内置银行显示信息。 */
    public static void init() {
        registerBankInfos();
    }

    /** 注册各银行的本地化显示信息。 */
    private static void registerBankInfos() {
        if (bankInfosRegistered) return;
        bankInfosRegistered = true;

        BankInfo.registerBankInfo(CENTRAL, "central", "technology",
                "科技银行", "Technology Bank",
                "面向自动化账户和标准银行卡的高效银行服务。", "Efficient banking services for automated accounts and standard bank cards.");
        BankInfo.registerBankInfo(FARMERS, "farmers", "agricultural",
                "农业银行", "Agricultural Bank",
                "面向农业贸易与日常储蓄的银行。", "Banking for agricultural trade and everyday savings.");
        BankInfo.registerBankInfo(MERCHANT, "merchant", "starry_sky",
                "星空银行", "Starry Sky Bank",
                "面向远途商贸流通与多货币交易的银行。", "Banking for long-range commerce, circulation, and multi-currency trade.");
        BankInfo.registerBankInfo(NATURE, "nature", "nature",
                "自然银行", "Nature Bank",
                "面向自然资源、生态贸易与基础储蓄的银行。", "Banking for natural resources, ecological trade, and basic savings.");
        BankInfo.registerBankInfo(MAGIC, "magic", "magic",
                "魔法银行", "Magic Bank",
                "面向特殊资产、标记账户与稀有交易的银行。", "Banking for special assets, tagged accounts, and rare trades.");
    }
}

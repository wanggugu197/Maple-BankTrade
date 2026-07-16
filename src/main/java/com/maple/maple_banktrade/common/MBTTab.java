package com.maple.maple_banktrade.common;

import net.minecraft.world.item.CreativeModeTab;

import com.gto.registrylib.util.entry.RegistryEntry;

import java.util.Map;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.api.bank.WalletApiRegistration.WALLET;

/**
 * 创造模式物品栏页签。
 */
public class MBTTab {

    // ==============================================
    // 页签
    // ==============================================

    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB_BANK = REGISTRY
            .creativeTab("maple_banktrade", "Maple BankTrade", Map.of("zh_cn", "枫糖银贸"),
                    builder -> builder.icon(WALLET::asStack));

    // ==============================================
    // 初始化
    // ==============================================

    /** 预留初始化入口（静态字段完成注册）。 */
    public static void init() {}
}

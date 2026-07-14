package com.maple.maple_banktrade.api.bank;

import net.neoforged.bus.api.IEventBus;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.api.bank.item.WalletAttachment;
import com.maple.maple_banktrade.api.bank.ui.BankCardDetailUIRegistration;
import com.maple.maple_banktrade.api.bank.ui.ParameterizedPlayerUIMenuType;
import com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;

/**
 * 钱包物品与相关 UI 的 API 注册入口。
 */
public class WalletApiRegistration {

    // ==============================================
    // 注册
    // ==============================================

    /** 钱包物品。 */
    public static final ItemEntry<ComponentItem> WALLET = REGISTRY
            .componentItem("wallet")
            .langCn("钱包")
            .attach(new WalletAttachment())
            .addTab(TAB_BANK.getKey())
            .register();

    /** 注册参数化菜单、卡详情 UI 与钱包 UI。 */
    public static void init(IEventBus modBus) {
        ParameterizedPlayerUIMenuType.init(modBus);
        BankCardDetailUIRegistration.init();
        WalletUIRegistration.init();
    }
}

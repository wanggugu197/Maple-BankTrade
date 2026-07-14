package com.maple.maple_banktrade.common;

import net.neoforged.bus.api.IEventBus;

import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.WalletApiRegistration;
import com.maple.maple_banktrade.bank.WalletRegistration;
import com.maple.maple_banktrade.bank.command.MBTBankCommands;
import com.maple.maple_banktrade.config.MBTModConfig;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;
import com.maple.maple_banktrade.trade.registration.CurrencyItemTradeRegistration;
import com.maple.maple_banktrade.trade.registration.MachineTradeRegistration;

/**
 * 通用初始化。
 */
public class CommonInit {

    public static void init(IEventBus modBus) {
        MBTModConfig.init();
        MBTTab.init();
        MBTLangHandler.init();

        // 初始化银行基础UI
        WalletApiRegistration.init(modBus);

        // 初始化银行注册内容
        WalletRegistration.init();
        // 初始化银行系统的世界数据注册入口
        MBTBankStates.init();
        // 初始化银行命令
        if (MBTModConfig.enableBankCommands()) {
            MBTBankCommands.init();
        }
        // 初始化交易项目（惰性 Item/Fluid 引用，不在此处解析 components）
        if (MBTModConfig.enableBuiltInTrades()) {
            CurrencyItemTradeRegistration.init();
            MachineTradeRegistration.init();
        }
    }
}

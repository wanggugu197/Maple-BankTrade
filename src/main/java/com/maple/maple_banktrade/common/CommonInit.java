package com.maple.maple_banktrade.common;

import net.neoforged.bus.api.IEventBus;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.WalletApiRegistration;
import com.maple.maple_banktrade.common.bank.BankRegistration;
import com.maple.maple_banktrade.common.bank.CardRegistration;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.common.bank.TradableTypeRegistration;
import com.maple.maple_banktrade.common.trade.CurrencyItemTradeRegistration;
import com.maple.maple_banktrade.common.trade.MachineTradeRegistration;
import com.maple.maple_banktrade.config.MBTModConfig;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

/**
 * 通用初始化。
 * <p>
 * 始终注册 API 层；内置内容（银行/卡/货币/交易站/价目）由
 * {@link MBTModConfig#enableModContent()} 统一开关。
 * </p>
 */
public class CommonInit {

    public static void init(IEventBus modBus) {
        MBTModConfig.init();
        MBTDataComponent.init();
        MBTLangHandler.init();
        MBTTab.init();

        // API：钱包物品、UI、银行世界数据、命令
        WalletApiRegistration.init(modBus);

        // 内置内容：银行/卡/货币/交易站/价目（可整体关闭，仅保留 API）
        if (MBTModConfig.enableModContent()) {
            contentInit(modBus);
        } else {
            MapleBankTrade.LOGGER.info(
                    "[{}] enableModContent=false: skipped built-in content (bankInit / trades / MBTRegistration); API only",
                    MapleBankTrade.MODID);
        }
    }

    /**
     * 注册模组内置内容（非 API）。
     */
    public static void contentInit(IEventBus modBus) {
        // 初始化内置货币、交易类型显示、银行与银行卡
        CurrencyRegistration.init();
        TradableTypeRegistration.init();
        BankRegistration.init();
        CardRegistration.init();

        // 内置价目（惰性 Item/Fluid 引用；不在此处解析 components）
        if (MBTModConfig.enableBuiltInTrades()) {
            CurrencyItemTradeRegistration.init();
            MachineTradeRegistration.init();
        }

        // 触发交易站等 DeferredRegister 静态登记
        MBTRegistration.init();
        // 各贸易站：物品 / 流体 / 能量能力（基类统一注册）
        modBus.addListener(MBTRegistration::registerTradingStationCapabilities);
    }
}

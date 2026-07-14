package com.maple.maple_banktrade.bank;

import com.maple.maple_banktrade.bank.registration.BankRegistration;
import com.maple.maple_banktrade.bank.registration.CardRegistration;
import com.maple.maple_banktrade.bank.registration.CurrencyRegistration;
import com.maple.maple_banktrade.bank.registration.TradableTypeRegistration;

/**
 * 银行内置内容总注册入口。
 */
public class WalletRegistration {

    /** 初始化货币、交易类型、银行与银行卡。 */
    public static void init() {
        CurrencyRegistration.init();
        TradableTypeRegistration.init();

        BankRegistration.init();
        CardRegistration.init();
    }
}

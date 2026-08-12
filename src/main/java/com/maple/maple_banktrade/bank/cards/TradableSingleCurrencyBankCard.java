package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;

public class TradableSingleCurrencyBankCard extends SingleCurrencyBankCard implements TradableWalletBankCard {

    /** 可交易单货币银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("tradable_single_currency");
    private static final Identifier NO_TRADE_TYPE_ID = MapleBankTrade.id("trade_type/none");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<TradableSingleCurrencyBankCard> CODEC = PersistedParser.createCodec(TradableSingleCurrencyBankCard::new);
    public static final MapCodec<TradableSingleCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(TradableSingleCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private Identifier tradeTypeId;

    // ==============================================
    // 构造
    // ==============================================

    public TradableSingleCurrencyBankCard() {
        this.tradeTypeId = NO_TRADE_TYPE_ID;
    }

    public TradableSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, Identifier tradeTypeId) {
        this(identity, currencyType, 0L, tradeTypeId);
    }

    public TradableSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, long balance, Identifier tradeTypeId) {
        this(identity, CARD_TYPE_ID, currencyType, balance, tradeTypeId);
    }

    protected TradableSingleCurrencyBankCard(BankCardIdentity identity, Identifier cardTypeId, CurrencyType currencyType, long balance, Identifier tradeTypeId) {
        super(identity, cardTypeId, currencyType, balance);
        this.tradeTypeId = tradeTypeId == null ? NO_TRADE_TYPE_ID : tradeTypeId;
    }
}

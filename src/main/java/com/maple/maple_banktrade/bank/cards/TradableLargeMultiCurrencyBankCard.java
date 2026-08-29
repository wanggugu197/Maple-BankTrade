package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public class TradableLargeMultiCurrencyBankCard extends LargeMultiCurrencyBankCard
                                                implements TradableWalletBankCard {

    public static final ResourceLocation CARD_TYPE_ID = MapleBankTrade.id("tradable_large_multi_currency");
    private static final ResourceLocation NO_TRADE_TYPE_ID = MapleBankTrade.id("none");

    // ==============================================
    // Codec
    // ==============================================

    public static final Codec<TradableLargeMultiCurrencyBankCard> CODEC = PersistedParser.createCodec(TradableLargeMultiCurrencyBankCard::new);
    public static final MapCodec<TradableLargeMultiCurrencyBankCard> MAP_CODEC = PersistedParser.createMapCodec(TradableLargeMultiCurrencyBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private ResourceLocation tradeTypeId;

    // ==============================================
    // 构造
    // ==============================================

    public TradableLargeMultiCurrencyBankCard() {
        this.tradeTypeId = NO_TRADE_TYPE_ID;
    }

    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              Collection<CurrencyType> currencyTypes,
                                              ResourceLocation tradeTypeId) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, BigInteger.ZERO), tradeTypeId);
    }

    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              ResourceLocation tradeTypeId,
                                              CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(BigInteger.ZERO, currencyTypes), tradeTypeId);
    }

    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              Map<ResourceLocation, BigInteger> balances,
                                              ResourceLocation tradeTypeId) {
        this(identity, CARD_TYPE_ID, balances, tradeTypeId);
    }

    protected TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                                 ResourceLocation cardTypeId,
                                                 Map<ResourceLocation, BigInteger> balances,
                                                 ResourceLocation tradeTypeId) {
        super(identity, cardTypeId, balances);
        this.tradeTypeId = tradeTypeId == null ? NO_TRADE_TYPE_ID : tradeTypeId;
    }
}

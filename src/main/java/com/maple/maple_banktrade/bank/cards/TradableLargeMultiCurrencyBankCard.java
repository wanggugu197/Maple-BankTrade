package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

/**
 * 可在钱包 UI 中执行货币-物品交易的大额多货币银行卡。
 * <p>
 * 支持多种货币的 BigInteger 余额，同时绑定一个交易类型 ID，
 * 用于在钱包详情页中展示对应的交易面板。
 * </p>
 */
public class TradableLargeMultiCurrencyBankCard extends LargeMultiCurrencyBankCard
                                                implements TradableWalletBankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 可交易大额多货币银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("tradable_large_multi_currency");

    /** 缺省交易类型 ID（表示无交易通道）。 */
    private static final Identifier NO_TRADE_TYPE_ID = MapleBankTrade.id("trade_type/none");

    // ==============================================
    // Codec
    // ==============================================

    /**
     * 可交易大额多货币银行卡序列化编解码器。
     * <p>
     * 复用父类的余额表编解码，额外存储 tradeTypeId。
     * </p>
     */
    public static final MapCodec<TradableLargeMultiCurrencyBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            BALANCES_CODEC.fieldOf("balances").forGetter(TradableLargeMultiCurrencyBankCard::getBalances),
            Identifier.CODEC.fieldOf("trade_type").forGetter(TradableLargeMultiCurrencyBankCard::getTradeTypeId))
            .apply(instance, TradableLargeMultiCurrencyBankCard::new));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Identifier tradeTypeId;

    // ==============================================
    // 构造方法
    // ==============================================

    /**
     * 创建指定身份、货币集合和交易类型的可交易大额多货币卡。
     *
     * @param identity      银行卡身份（UUID、银行类型、名称索引）
     * @param currencyTypes 支持的货币类型（至少一种）
     * @param tradeTypeId   交易通道 ID
     */
    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              Collection<CurrencyType> currencyTypes,
                                              Identifier tradeTypeId) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, BigInteger.ZERO), tradeTypeId);
    }

    /**
     * 创建指定身份、货币数组和交易类型的可交易大额多货币卡。
     *
     * @param identity      银行卡身份
     * @param tradeTypeId   交易通道 ID
     * @param currencyTypes 支持的货币类型（可变参数）
     */
    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              Identifier tradeTypeId,
                                              CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(BigInteger.ZERO, currencyTypes), tradeTypeId);
    }

    public TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                              Map<Identifier, BigInteger> balances,
                                              Identifier tradeTypeId) {
        this(identity, CARD_TYPE_ID, balances, tradeTypeId);
    }

    /**
     * 从存档字段恢复可交易大额多货币卡（供 Codec 使用）。
     */
    protected TradableLargeMultiCurrencyBankCard(BankCardIdentity identity,
                                                 Identifier cardTypeId,
                                                 Map<Identifier, BigInteger> balances,
                                                 Identifier tradeTypeId) {
        super(identity, cardTypeId, balances);
        this.tradeTypeId = tradeTypeId == null ? NO_TRADE_TYPE_ID : tradeTypeId;
    }
}

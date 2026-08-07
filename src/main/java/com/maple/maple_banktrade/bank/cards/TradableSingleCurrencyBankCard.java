package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

/**
 * 可在钱包 UI 中执行货币-物品交易的单货币银行卡。
 */
public class TradableSingleCurrencyBankCard extends SingleCurrencyBankCard implements TradableWalletBankCard {

    // ==============================================
    // 常量 / Codec
    // ==============================================

    /** 可交易单货币银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("tradable_single_currency");
    private static final Identifier NO_TRADE_TYPE_ID = MapleBankTrade.id("trade_type/none");

    /** 可交易单货币银行卡序列化编解码器。 */
    public static final MapCodec<TradableSingleCurrencyBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            CurrencyType.CODEC.fieldOf("currency").forGetter(TradableSingleCurrencyBankCard::getCurrencyType),
            Codec.LONG.fieldOf("balance").forGetter(TradableSingleCurrencyBankCard::getBalance),
            Identifier.CODEC.fieldOf("trade_type").forGetter(TradableSingleCurrencyBankCard::getTradeTypeId))
            .apply(instance, TradableSingleCurrencyBankCard::new));

    // ==============================================
    // 字段 / 构造
    // ==============================================

    @Getter
    private final Identifier tradeTypeId;

    /** 创建指定银行、名称索引、货币和交易类型的可交易单货币卡。 */
    public TradableSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType,
                                          Identifier tradeTypeId) {
        this(identity, currencyType, 0L, tradeTypeId);
    }

    public TradableSingleCurrencyBankCard(BankCardIdentity identity, CurrencyType currencyType, long balance, Identifier tradeTypeId) {
        this(identity, CARD_TYPE_ID, currencyType, balance, tradeTypeId);
    }

    /** 从存档字段恢复可交易单货币卡。 */
    protected TradableSingleCurrencyBankCard(BankCardIdentity identity, Identifier cardTypeId, CurrencyType currencyType, long balance, Identifier tradeTypeId) {
        super(identity, cardTypeId, currencyType, balance);
        this.tradeTypeId = tradeTypeId == null ? NO_TRADE_TYPE_ID : tradeTypeId;
    }
}

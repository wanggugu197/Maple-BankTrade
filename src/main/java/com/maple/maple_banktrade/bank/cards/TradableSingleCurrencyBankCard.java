package com.maple.maple_banktrade.bank.cards;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.capability.TradableWalletBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.UUID;

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
    public static final Codec<TradableSingleCurrencyBankCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("card_uuid").forGetter(TradableSingleCurrencyBankCard::getCardUuid),
            Identifier.CODEC.fieldOf("bank_type").forGetter(TradableSingleCurrencyBankCard::getBankTypeId),
            Identifier.CODEC.fieldOf("card_type").forGetter(TradableSingleCurrencyBankCard::getCardTypeId),
            Identifier.CODEC.fieldOf("name_index").forGetter(TradableSingleCurrencyBankCard::getNameIndex),
            CurrencyType.ID_CODEC.fieldOf("currency").forGetter(TradableSingleCurrencyBankCard::getCurrencyTypeId),
            Codec.LONG.fieldOf("balance").forGetter(TradableSingleCurrencyBankCard::getBalance),
            Identifier.CODEC.fieldOf("trade_type").forGetter(TradableSingleCurrencyBankCard::getTradeTypeId))
            .apply(instance, (cardUuid, bankTypeId, ignoredCardTypeId, nameIndex, currencyTypeId, balance, tradeTypeId) -> new TradableSingleCurrencyBankCard(cardUuid, bankTypeId, nameIndex, currencyTypeId, balance, tradeTypeId)));

    // ==============================================
    // 字段 / 构造
    // ==============================================

    @Getter
    private final Identifier tradeTypeId;

    /** 创建指定银行、名称索引、货币和交易类型的可交易单货币卡。 */
    public TradableSingleCurrencyBankCard(UUID cardUuid, BankType bankType, Identifier nameIndex, CurrencyType currencyType,
                                          Identifier tradeTypeId) {
        this(cardUuid, bankType.id(), nameIndex, currencyType.id(), 0L, tradeTypeId);
    }

    /** 从存档字段恢复可交易单货币卡。 */
    protected TradableSingleCurrencyBankCard(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex, Identifier currencyTypeId, long balance,
                                             Identifier tradeTypeId) {
        super(cardUuid, bankTypeId, CARD_TYPE_ID, nameIndex, currencyTypeId, balance);
        this.tradeTypeId = tradeTypeId == null ? NO_TRADE_TYPE_ID : tradeTypeId;
    }
}

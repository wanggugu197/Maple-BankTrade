package com.maple.maple_banktrade.common.bank;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardType;
import com.maple.maple_banktrade.api.bank.data.CardInfo;
import com.maple.maple_banktrade.bank.cards.*;
import com.maple.maple_banktrade.bank.ui.cardInfos.*;
import com.maple.maple_banktrade.common.trade.CurrencyItemTradeRegistration;

import static com.maple.maple_banktrade.common.bank.BankRegistration.*;
import static com.maple.maple_banktrade.common.bank.CurrencyRegistration.*;

/**
 * 内置银行卡类型、创建工厂与 UI 信息注册。
 */
public class CardRegistration {

    // ==============================================
    // 状态 / 默认样式
    // ==============================================

    private static boolean bankCardTypesRegistered;
    private static boolean cardInfosRegistered;

    private static final IGuiTexture DEFAULT_DETAIL_BACKGROUND = SpriteTexture.of("maple_banktrade:textures/gui/wallet_lining.png").setBorder(4, 4, 4, 4);

    // ==============================================
    // 卡创建工厂
    // ==============================================

    /** 石料与矿石交易卡（中央银行）。 */
    public static final BankCardFactory CENTRAL_STONE_ORES_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_stone_ores_card"), "中央银行石料矿石卡", "Central Stone & Ores Card",
            CENTRAL,
            (cardUuid, bankType, nameIndex) -> new TradableSingleCurrencyBankCard(
                    cardUuid, bankType, nameIndex, COINS, CurrencyItemTradeRegistration.STONE_AND_ORES.id()));

    /** 植物与食物交易卡（农业银行）。 */
    public static final BankCardFactory FARMERS_PLANTS_FOOD_CARD = BankCardFactory.register(
            MapleBankTrade.id("farmers_plants_food_card"), "农业银行植物食物卡", "Farmers Plants & Food Card",
            FARMERS,
            (cardUuid, bankType, nameIndex) -> new TradableSingleCurrencyBankCard(
                    cardUuid, bankType, nameIndex, COINS, CurrencyItemTradeRegistration.PLANTS_AND_FOOD.id()));

    /** 生物掉落交易卡（星空银行）。 */
    public static final BankCardFactory MERCHANT_MOB_DROPS_CARD = BankCardFactory.register(
            MapleBankTrade.id("merchant_mob_drops_card"), "星空银行生物掉落卡", "Merchant Mob Drops Card",
            MERCHANT,
            (cardUuid, bankType, nameIndex) -> new TradableSingleCurrencyBankCard(
                    cardUuid, bankType, nameIndex, COINS, CurrencyItemTradeRegistration.MOB_DROPS.id()));

    /** 兼容旧 ID：中央单货币卡 → 石料矿石交易。 */
    public static final BankCardFactory CENTRAL_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_single_currency_card"), "中央银行单货币卡", "Central Single Currency Card",
            CENTRAL,
            (cardUuid, bankType, nameIndex) -> new TradableSingleCurrencyBankCard(
                    cardUuid, bankType, nameIndex, COINS, CurrencyItemTradeRegistration.STONE_AND_ORES.id()));

    /** 中央大额单货币卡。 */
    public static final BankCardFactory CENTRAL_LARGE_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_large_single_currency_card"), "中央银行大额单货币卡", "Central Large Single Currency Card",
            FARMERS,
            (cardUuid, bankType, nameIndex) -> new LargeSingleCurrencyBankCard(cardUuid, bankType, nameIndex, COINS));

    /** 中央多货币卡。 */
    public static final BankCardFactory CENTRAL_MULTI_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_multi_currency_card"), "中央银行多货币卡", "Central Multi Currency Card",
            MERCHANT,
            (cardUuid, bankType, nameIndex) -> new MultiCurrencyBankCard(cardUuid, bankType, nameIndex, COINS, GOLD, DIAMONDS));

    /** 中央标记卡。 */
    public static final BankCardFactory CENTRAL_TAGGED_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_tagged_card"), "中央银行标记卡", "Central Tagged Card",
            MERCHANT,
            TaggedBankCard::new);

    /** 自然银行单货币卡。 */
    public static final BankCardFactory NATURE_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("nature_single_currency_card"), "自然银行单货币卡", "Nature Single Currency Card",
            NATURE,
            (cardUuid, bankType, nameIndex) -> new SingleCurrencyBankCard(cardUuid, bankType, nameIndex, COINS));

    /** 魔法银行标记卡。 */
    public static final BankCardFactory MAGIC_TAGGED_CARD = BankCardFactory.register(
            MapleBankTrade.id("magic_tagged_card"), "魔法银行标记卡", "Magic Tagged Card",
            MAGIC,
            TaggedBankCard::new);

    // ==============================================
    // 注册
    // ==============================================

    /** 注册银行卡类型与显示信息。 */
    public static void init() {
        registerBankCards();
        registerCardInfos();
    }

    /** 注册内置银行卡类型 Codec。 */
    private static void registerBankCards() {
        if (bankCardTypesRegistered) return;
        bankCardTypesRegistered = true;
        BankCardType.register(SingleCurrencyBankCard.CARD_TYPE_ID, SingleCurrencyBankCard.class, SingleCurrencyBankCard.CODEC);
        BankCardType.register(TradableSingleCurrencyBankCard.CARD_TYPE_ID, TradableSingleCurrencyBankCard.class, TradableSingleCurrencyBankCard.CODEC);
        BankCardType.register(LargeSingleCurrencyBankCard.CARD_TYPE_ID, LargeSingleCurrencyBankCard.class, LargeSingleCurrencyBankCard.CODEC);
        BankCardType.register(MultiCurrencyBankCard.CARD_TYPE_ID, MultiCurrencyBankCard.class, MultiCurrencyBankCard.CODEC);
        BankCardType.register(TaggedBankCard.CARD_TYPE_ID, TaggedBankCard.class, TaggedBankCard.CODEC);
    }

    /** 注册内置银行卡 UI 信息。 */
    private static void registerCardInfos() {
        if (cardInfosRegistered) return;
        cardInfosRegistered = true;

        registerTradableCardInfo(CENTRAL_STONE_ORES_CARD, Sprites.RECT_RD_T);
        registerTradableCardInfo(FARMERS_PLANTS_FOOD_CARD, Sprites.RECT_RD_LIGHT);
        registerTradableCardInfo(MERCHANT_MOB_DROPS_CARD, Sprites.RECT_RD);
        registerTradableCardInfo(CENTRAL_SINGLE_CURRENCY_CARD, Sprites.RECT_RD_T);

        CardInfo.register(CENTRAL_LARGE_SINGLE_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD_T_DARK, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> LargeSingleCurrencyBankCardInfo.createSimplifiedUI((LargeSingleCurrencyBankCard) card),
                (player, card) -> LargeSingleCurrencyBankCardInfo.createDetailsUI(player, (LargeSingleCurrencyBankCard) card));
        CardInfo.register(CENTRAL_MULTI_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> MultiCurrencyBankCardInfo.createSimplifiedUI((MultiCurrencyBankCard) card),
                (player, card) -> MultiCurrencyBankCardInfo.createDetailsUI(player, (MultiCurrencyBankCard) card));
        CardInfo.register(CENTRAL_TAGGED_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, Sprites.BORDER1_RT0_TRANSLATE,
                (_, card) -> TaggedBankCardInfo.createSimplifiedUI((TaggedBankCard) card),
                (player, card) -> TaggedBankCardInfo.createDetailsUI(player, (TaggedBankCard) card));
        CardInfo.register(NATURE_SINGLE_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> SingleCurrencyBankCardInfo.createSimplifiedUI((SingleCurrencyBankCard) card),
                (player, card) -> SingleCurrencyBankCardInfo.createDetailsUI(player, (SingleCurrencyBankCard) card));
        CardInfo.register(MAGIC_TAGGED_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, Sprites.BORDER1_RT0_TRANSLATE,
                (_, card) -> TaggedBankCardInfo.createSimplifiedUI((TaggedBankCard) card),
                (player, card) -> TaggedBankCardInfo.createDetailsUI(player, (TaggedBankCard) card));
    }

    /** 注册可交易单货币卡的 UI 信息。 */
    private static void registerTradableCardInfo(BankCardFactory factory, IGuiTexture simplifiedBackground) {
        CardInfo.register(factory.nameIndex(),
                simplifiedBackground, DEFAULT_DETAIL_BACKGROUND,
                (player, card) -> TradableSingleCurrencyBankCardInfo.createSimplifiedUI((TradableSingleCurrencyBankCard) card),
                (player, card) -> card instanceof TradableSingleCurrencyBankCard tradableCard ? TradableSingleCurrencyBankCardInfo.createDetailsUI(player, tradableCard) : SingleCurrencyBankCardInfo.createDetailsUI(player, (SingleCurrencyBankCard) card));
    }
}

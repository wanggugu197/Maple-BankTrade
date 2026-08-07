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
import com.maple.maple_banktrade.common.MBTTab;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import static com.maple.maple_banktrade.api.bank.base.BankCardFactory.getBankCardFactoryTranslationKey;
import static com.maple.maple_banktrade.common.bank.BankRegistration.*;
import static com.maple.maple_banktrade.common.bank.CurrencyRegistration.*;

/**
 * 内置银行卡类型、创建工厂与 UI 信息注册。
 */
public class CardRegistration {

    // ==============================================
    // 状态 / 默认样式
    // ==============================================

    private static final IGuiTexture DEFAULT_DETAIL_BACKGROUND = SpriteTexture.of("maple_banktrade:textures/gui/wallet_lining.png").setBorder(4, 4, 4, 4);

    // ==============================================
    // 卡创建工厂
    // ==============================================

    /** 石料与矿石交易卡（中央银行）。 */
    public static final BankCardFactory CENTRAL_STONE_ORES_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_stone_ores_card"),
            CENTRAL,
            (identity) -> new TradableSingleCurrencyBankCard(identity, COINS, TradeTypeRegistration.STONE_AND_ORES.id()),
            MBTTab.TAB_BANK.getKey());

    /** 植物与食物交易卡（农业银行）。 */
    public static final BankCardFactory FARMERS_PLANTS_FOOD_CARD = BankCardFactory.register(
            MapleBankTrade.id("farmers_plants_food_card"),
            AGRICULTURAL,
            (identity) -> new TradableSingleCurrencyBankCard(identity, COINS, TradeTypeRegistration.PLANTS_AND_FOOD.id()),
            MBTTab.TAB_BANK.getKey());

    /** 生物掉落交易卡（星空银行）。 */
    public static final BankCardFactory MERCHANT_MOB_DROPS_CARD = BankCardFactory.register(
            MapleBankTrade.id("merchant_mob_drops_card"),
            STARRY_SKY,
            (identity) -> new TradableSingleCurrencyBankCard(identity, COINS, TradeTypeRegistration.MOB_DROPS.id()),
            MBTTab.TAB_BANK.getKey());

    /** 兼容旧 ID：中央单货币卡 → 石料矿石交易。 */
    public static final BankCardFactory CENTRAL_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_single_currency_card"),
            CENTRAL,
            (identity) -> new TradableSingleCurrencyBankCard(identity, COINS, TradeTypeRegistration.STONE_AND_ORES.id()),
            MBTTab.TAB_BANK.getKey());

    /** 中央大额单货币卡。 */
    public static final BankCardFactory CENTRAL_LARGE_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_large_single_currency_card"),
            AGRICULTURAL,
            (identity) -> new LargeSingleCurrencyBankCard(identity, COINS),
            MBTTab.TAB_BANK.getKey());

    /** 中央多货币卡。 */
    public static final BankCardFactory CENTRAL_MULTI_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_multi_currency_card"),
            STARRY_SKY,
            (identity) -> new MultiCurrencyBankCard(identity, COINS, GOLD, DIAMONDS),
            MBTTab.TAB_BANK.getKey());

    /** 中央标记卡。 */
    public static final BankCardFactory CENTRAL_TAGGED_CARD = BankCardFactory.register(
            MapleBankTrade.id("central_tagged_card"),
            STARRY_SKY,
            TaggedBankCard::new,
            MBTTab.TAB_BANK.getKey());

    /** 自然银行单货币卡。 */
    public static final BankCardFactory NATURE_SINGLE_CURRENCY_CARD = BankCardFactory.register(
            MapleBankTrade.id("nature_single_currency_card"),
            NATURE,
            (identity) -> new SingleCurrencyBankCard(identity, COINS),
            MBTTab.TAB_BANK.getKey());

    /** 魔法银行标记卡。 */
    public static final BankCardFactory MAGIC_TAGGED_CARD = BankCardFactory.register(
            MapleBankTrade.id("magic_tagged_card"),
            MAGIC,
            TaggedBankCard::new,
            MBTTab.TAB_BANK.getKey());

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
        BankCardType.register(SingleCurrencyBankCard.CARD_TYPE_ID, SingleCurrencyBankCard.class, SingleCurrencyBankCard.MAP_CODEC);
        BankCardType.register(TradableSingleCurrencyBankCard.CARD_TYPE_ID, TradableSingleCurrencyBankCard.class, TradableSingleCurrencyBankCard.MAP_CODEC);
        BankCardType.register(LargeSingleCurrencyBankCard.CARD_TYPE_ID, LargeSingleCurrencyBankCard.class, LargeSingleCurrencyBankCard.MAP_CODEC);
        BankCardType.register(MultiCurrencyBankCard.CARD_TYPE_ID, MultiCurrencyBankCard.class, MultiCurrencyBankCard.MAP_CODEC);
        BankCardType.register(LargeMultiCurrencyBankCard.CARD_TYPE_ID, LargeMultiCurrencyBankCard.class, LargeMultiCurrencyBankCard.MAP_CODEC);
        BankCardType.register(TradableLargeMultiCurrencyBankCard.CARD_TYPE_ID, TradableLargeMultiCurrencyBankCard.class, TradableLargeMultiCurrencyBankCard.MAP_CODEC);
        BankCardType.register(TaggedBankCard.CARD_TYPE_ID, TaggedBankCard.class, TaggedBankCard.MAP_CODEC);
    }

    /** 注册内置银行卡 UI 信息。 */
    private static void registerCardInfos() {
        registerTradableCardInfo(CENTRAL_STONE_ORES_CARD, Sprites.RECT_RD_T);
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(CENTRAL_STONE_ORES_CARD.nameIndex()), "中央银行石料矿石卡", "Central Stone & Ores Card");

        registerTradableCardInfo(FARMERS_PLANTS_FOOD_CARD, Sprites.RECT_RD_LIGHT);
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(FARMERS_PLANTS_FOOD_CARD.nameIndex()), "农业银行植物食物卡", "Farmers Plants & Food Card");

        registerTradableCardInfo(MERCHANT_MOB_DROPS_CARD, Sprites.RECT_RD);
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(MERCHANT_MOB_DROPS_CARD.nameIndex()), "星空银行生物掉落卡", "Merchant Mob Drops Card");

        registerTradableCardInfo(CENTRAL_SINGLE_CURRENCY_CARD, Sprites.RECT_RD_T);
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(CENTRAL_SINGLE_CURRENCY_CARD.nameIndex()), "中央银行单货币卡", "Central Single Currency Card");

        CardInfo.register(CENTRAL_LARGE_SINGLE_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD_T_DARK, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> LargeSingleCurrencyBankCardInfo.createSimplifiedUI((LargeSingleCurrencyBankCard) card),
                (player, card) -> LargeSingleCurrencyBankCardInfo.createDetailsUI(player, (LargeSingleCurrencyBankCard) card));
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(CENTRAL_LARGE_SINGLE_CURRENCY_CARD.nameIndex()), "中央银行大额单货币卡", "Central Large Single Currency Card");

        CardInfo.register(CENTRAL_MULTI_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> MultiCurrencyBankCardInfo.createSimplifiedUI((MultiCurrencyBankCard) card),
                (player, card) -> MultiCurrencyBankCardInfo.createDetailsUI(player, (MultiCurrencyBankCard) card));
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(CENTRAL_MULTI_CURRENCY_CARD.nameIndex()), "中央银行多货币卡", "Central Multi Currency Card");

        CardInfo.register(CENTRAL_TAGGED_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, Sprites.BORDER1_RT0_TRANSLATE,
                (_, card) -> TaggedBankCardInfo.createSimplifiedUI((TaggedBankCard) card),
                (player, card) -> TaggedBankCardInfo.createDetailsUI(player, (TaggedBankCard) card));
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(CENTRAL_TAGGED_CARD.nameIndex()), "中央银行标记卡", "Central Tagged Card");

        CardInfo.register(NATURE_SINGLE_CURRENCY_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, DEFAULT_DETAIL_BACKGROUND,
                (_, card) -> SingleCurrencyBankCardInfo.createSimplifiedUI((SingleCurrencyBankCard) card),
                (player, card) -> SingleCurrencyBankCardInfo.createDetailsUI(player, (SingleCurrencyBankCard) card));
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(NATURE_SINGLE_CURRENCY_CARD.nameIndex()), "自然银行单货币卡", "Nature Single Currency Card");

        CardInfo.register(MAGIC_TAGGED_CARD.nameIndex(),
                Sprites.RECT_RD_LIGHT, Sprites.BORDER1_RT0_TRANSLATE,
                (_, card) -> TaggedBankCardInfo.createSimplifiedUI((TaggedBankCard) card),
                (player, card) -> TaggedBankCardInfo.createDetailsUI(player, (TaggedBankCard) card));
        MBTLangHandler.addLang(getBankCardFactoryTranslationKey(MAGIC_TAGGED_CARD.nameIndex()), "魔法银行标记卡", "Magic Tagged Card");
    }

    /** 注册可交易单货币卡的 UI 信息。 */
    private static void registerTradableCardInfo(BankCardFactory factory, IGuiTexture simplifiedBackground) {
        CardInfo.register(factory.nameIndex(),
                simplifiedBackground, DEFAULT_DETAIL_BACKGROUND,
                (player, card) -> TradableSingleCurrencyBankCardInfo.createSimplifiedUI((TradableSingleCurrencyBankCard) card),
                (player, card) -> card instanceof TradableSingleCurrencyBankCard tradableCard ? TradableSingleCurrencyBankCardInfo.createDetailsUI(player, tradableCard) : SingleCurrencyBankCardInfo.createDetailsUI(player, (SingleCurrencyBankCard) card));
    }
}

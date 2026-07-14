package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import java.util.*;

/**
 * 银行卡名称索引对应的创建定义；nameIndex 不等同于 card_type。
 */
public record BankCardFactory(Identifier nameIndex, BankType bankType, Factory factory) {

    // ==============================================
    // 注册表
    // ==============================================

    /** 银行卡创建定义注册表：nameIndex -> BankCardFactory，按注册顺序保存。 */
    private static final Map<Identifier, BankCardFactory> BANK_CARD_FACTORY_MAP = new LinkedHashMap<>();

    // ==============================================
    // 构造
    // ==============================================

    /** 创建银行卡创建定义并校验必要字段。 */
    public BankCardFactory {
        Objects.requireNonNull(nameIndex, "nameIndex");
        Objects.requireNonNull(bankType, "bankType");
        Objects.requireNonNull(factory, "factory");
    }

    // ==============================================
    // 注册
    // ==============================================

    /** 注册银行卡创建定义，重复注册时返回已有定义。 */
    public static BankCardFactory register(Identifier nameIndex, String cnName, String enName, BankType bankType, Factory factory) {
        if (nameIndex == null || bankType == null || factory == null) return null;
        if (BANK_CARD_FACTORY_MAP.containsKey(nameIndex)) {
            MapleBankTrade.LOGGER.error("Bank card factory with name index {} already exists", nameIndex);
            return BANK_CARD_FACTORY_MAP.get(nameIndex);
        }
        BankCardFactory cardFactory = new BankCardFactory(nameIndex, bankType, factory);
        MBTLangHandler.addLang(BankCardFactory.getTranslationKey(nameIndex), cnName, enName);
        BANK_CARD_FACTORY_MAP.put(nameIndex, cardFactory);
        return cardFactory;
    }

    /** 获取银行卡名称索引对应的翻译键。 */
    public static String getTranslationKey(Identifier nameIndex) {
        return "bank_card." + nameIndex.getNamespace() + "." + nameIndex.getPath();
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 查询已注册银行卡创建定义；未知名称索引返回 null。 */
    public static BankCardFactory requireByNameIndex(Identifier nameIndex) {
        return BANK_CARD_FACTORY_MAP.get(nameIndex);
    }

    /** 获取全部已注册银行卡创建定义，顺序与注册顺序一致。 */
    public static Collection<BankCardFactory> values() {
        return Collections.unmodifiableCollection(BANK_CARD_FACTORY_MAP.values());
    }

    // ==============================================
    // 创建
    // ==============================================

    /** 创建并校验银行卡实例。 */
    public BankCard create(UUID cardUuid) {
        if (cardUuid == null) return null;
        if (bankType == null) return null;
        BankCard card = factory.create(cardUuid, bankType, nameIndex);
        if (card == null) return null;
        if (!nameIndex.equals(card.getNameIndex())) return null;
        if (!bankType.id().equals(card.getBankTypeId())) return null;
        BankCardType<?> typeDefinition = BankCardType.get(card.getCardTypeId());
        if (typeDefinition == null) return null;
        return typeDefinition.matches(card) ? card : null;
    }

    // ==============================================
    // 内部类型
    // ==============================================

    /** 银行卡创建工厂。 */
    @FunctionalInterface
    public interface Factory {

        /** 创建一张指定银行类型和名称索引的新卡。 */
        BankCard create(UUID cardUuid, BankType bankType, Identifier nameIndex);
    }
}

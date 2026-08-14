package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.item.BankDataComponent;

import java.util.*;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.api.bank.WalletApiRegistration.BANK_CARD_AUTHENTICATION_KEY;

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
    public static BankCardFactory register(Identifier nameIndex, BankType bankType, Factory factory, ResourceKey<CreativeModeTab> tabResourceKey) {
        if (nameIndex == null || bankType == null || factory == null || tabResourceKey == null) return null;
        if (BANK_CARD_FACTORY_MAP.containsKey(nameIndex)) {
            MapleBankTrade.LOGGER.error("Bank card factory with name index {} already exists", nameIndex);
            return BANK_CARD_FACTORY_MAP.get(nameIndex);
        }
        BankCardFactory cardFactory = new BankCardFactory(nameIndex, bankType, factory);
        BANK_CARD_FACTORY_MAP.put(nameIndex, cardFactory);
        REGISTRY.modifyCreativeModeTab(tabResourceKey, tab -> tab.accept(cardFactory.getAuthenticationKey(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        return cardFactory;
    }

    /** 获取银行卡名称索引对应的翻译键。 */
    public static String getBankCardFactoryTranslationKey(Identifier nameIndex) {
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
    // 获取对应物品
    // ==============================================

    public ItemStack getAuthenticationKey() {
        ItemStack stack = BANK_CARD_AUTHENTICATION_KEY.asStack();
        BankDataComponent.CARD_NAME_INDEX.set(stack, nameIndex);
        return stack;
    }

    // ==============================================
    // 创建
    // ==============================================

    /** 创建并校验银行卡实例。 */
    public BankCard create(UUID cardUuid) {
        if (cardUuid == null) return null;
        if (bankType == null) return null;
        BankCard card = factory.create(BankCard.BankCardIdentity.of(cardUuid, bankType, nameIndex));
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
        BankCard create(BankCard.BankCardIdentity identity);
    }
}

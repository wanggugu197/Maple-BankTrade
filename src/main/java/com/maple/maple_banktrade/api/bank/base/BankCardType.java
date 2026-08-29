package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.ResourceLocation;

import com.maple.maple_banktrade.MapleBankTrade;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 银行卡类型定义：card_type 对应的实现类与 Codec。
 */
public record BankCardType<T extends BankCard>(ResourceLocation cardTypeId, Class<T> cardClass, MapCodec<T> codec) {

    // ==============================================
    // 注册表
    // ==============================================

    /** 银行卡类型 ID 到类型定义的映射。 */
    private static final Map<ResourceLocation, BankCardType<?>> REGISTRY = new HashMap<>();

    // ==============================================
    // 构造
    // ==============================================

    /** 创建银行卡类型定义并校验必要字段。 */
    public BankCardType {
        Objects.requireNonNull(cardTypeId, "cardTypeId");
        Objects.requireNonNull(cardClass, "cardClass");
        Objects.requireNonNull(codec, "codec");
    }

    // ==============================================
    // 注册
    // ==============================================

    /** 注册银行卡实现类型定义。 */
    public static <T extends BankCard> void register(ResourceLocation cardTypeId, Class<T> cardClass, MapCodec<T> codec) {
        if (cardTypeId == null || cardClass == null || codec == null) return;
        if (REGISTRY.containsKey(cardTypeId)) {
            MapleBankTrade.LOGGER.error("Bank card type with id {} already exists", cardTypeId);
            return;
        }
        REGISTRY.put(cardTypeId, new BankCardType<>(cardTypeId, cardClass, codec));
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 查询银行卡类型定义，未知类型返回 null。 */
    public static BankCardType<?> get(ResourceLocation cardTypeId) {
        if (cardTypeId == null) return null;
        return REGISTRY.get(cardTypeId);
    }

    /** 查询银行卡类型定义对应的 MapCodec；未知类型返回错误，由 dispatch 上报。 */
    public static DataResult<? extends MapCodec<? extends BankCard>> getCodecResult(ResourceLocation cardTypeId) {
        BankCardType<?> definition = get(cardTypeId);
        return definition == null ? DataResult.error(() -> "Unknown bank card type: " + cardTypeId) : DataResult.success(definition.codec());
    }

    /** 判断银行卡实例是否匹配当前定义。 */
    public boolean matches(BankCard card) {
        return card != null && cardTypeId.equals(card.getCardTypeId()) && cardClass.isInstance(card);
    }
}

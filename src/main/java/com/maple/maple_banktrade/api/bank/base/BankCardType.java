package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 银行卡类型定义：card_type 对应的实现类与 Codec。
 */
public record BankCardType<T extends BankCard>(Identifier cardTypeId, Class<T> cardClass, Codec<T> codec) {

    // ==============================================
    // 注册表
    // ==============================================

    /** 银行卡类型 ID 到类型定义的映射。 */
    private static final Map<Identifier, BankCardType<?>> REGISTRY = new HashMap<>();

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
    public static <T extends BankCard> void register(Identifier cardTypeId, Class<T> cardClass, Codec<T> codec) {
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
    public static BankCardType<?> get(Identifier cardTypeId) {
        if (cardTypeId == null) return null;
        return REGISTRY.get(cardTypeId);
    }

    /** 查询指定银行卡类型 ID 的 Codec。 */
    public static Codec<? extends BankCard> getCodec(Identifier cardTypeId) {
        BankCardType<?> definition = get(cardTypeId);
        return definition == null ? null : definition.codec();
    }

    /** 查询银行卡实例对应的 Codec，并校验实现类匹配。 */
    public static Codec<? extends BankCard> getCodec(BankCard card) {
        if (card == null) return null;
        BankCardType<?> definition = get(card.getCardTypeId());
        if (definition == null || !definition.matches(card)) return null;
        return definition.codec();
    }

    /** 判断银行卡实例是否匹配当前定义。 */
    public boolean matches(BankCard card) {
        return card != null && cardTypeId.equals(card.getCardTypeId()) && cardClass.isInstance(card);
    }

    // ==============================================
    // Codec
    // ==============================================

    /** 按存档中的 card_type 分发解码，缺失或未知时交给外层跳过。 */
    public static <O> DataResult<Pair<BankCard, O>> decodeCard(DynamicOps<O> ops, O input) {
        return ops.getMap(input).flatMap(map -> {
            O cardTypeValue = map.get("card_type");
            if (cardTypeValue == null) {
                return DataResult.error(() -> "Missing card_type for bank card");
            }

            O bankTypeValue = map.get("bank_type");
            if (bankTypeValue == null) {
                return DataResult.error(() -> "Missing bank_type for bank card");
            }
            return Identifier.CODEC.parse(ops, bankTypeValue)
                    .flatMap(bankTypeId -> {
                        BankType bankType = BankType.requireById(bankTypeId);
                        if (bankType == null) {
                            return DataResult.error(() -> "Unknown bank type: " + bankTypeId);
                        }
                        return Identifier.CODEC.parse(ops, cardTypeValue)
                                .flatMap(cardTypeId -> decodeByCardType(ops, input, cardTypeId));
                    });
        });
    }

    /** 使用当前定义的 Codec 解码银行卡。 */
    public <O> DataResult<Pair<BankCard, O>> decode(DynamicOps<O> ops, O input) {
        return codec.decode(ops, input).flatMap(pair -> {
            BankCard card = pair.getFirst();
            if (!matches(card)) {
                return DataResult.error(() -> "Decoded bank card " + card.getClass().getName() + " does not match card type: " + cardTypeId);
            }
            return DataResult.success(Pair.of(card, pair.getSecond()));
        });
    }

    /** 使用指定银行卡类型 ID 解码具体银行卡。 */
    private static <O> DataResult<Pair<BankCard, O>> decodeByCardType(DynamicOps<O> ops, O input, Identifier cardTypeId) {
        BankCardType<?> definition = get(cardTypeId);
        return definition == null ? DataResult.error(() -> "Unknown bank card type: " + cardTypeId) : definition.decode(ops, input);
    }
}

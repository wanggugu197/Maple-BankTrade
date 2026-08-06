package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.math.BigInteger;
import java.util.*;

public class LargeMultiCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    // ==============================================
    // 常量
    // ==============================================

    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("large_multi_currency");

    // ==============================================
    // Codec
    // ==============================================

    private static final Codec<BigInteger> BIG_INTEGER_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(new BigInteger(value));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid BigInteger: " + value);
        }
    }, BigInteger::toString);

    private static final Codec<Map<Identifier, BigInteger>> BALANCES_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Map<Identifier, BigInteger> input, DynamicOps<T> ops, T prefix) {
            return Codec.unboundedMap(CurrencyType.ID_CODEC, BIG_INTEGER_CODEC).encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<Map<Identifier, BigInteger>, T>> decode(DynamicOps<T> ops, T input) {
            Map<Identifier, BigInteger> result = new HashMap<>();
            ops.getMapValues(input)
                    .resultOrPartial(message -> MapleBankTrade.LOGGER.error("无法读取大额多货币银行卡余额表，跳过全部余额: {}", message))
                    .ifPresent(entries -> entries.forEach(entry -> {
                        Identifier currencyId = Identifier.CODEC.parse(ops, entry.getFirst())
                                .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过大额多货币银行卡余额项，货币 ID 无法反序列化: {}", message))
                                .orElse(null);
                        if (currencyId == null) return;
                        if (CurrencyType.requireById(currencyId) == null) {
                            MapleBankTrade.LOGGER.error("跳过大额多货币银行卡余额项，货币未注册: {}", currencyId);
                            return;
                        }
                        BIG_INTEGER_CODEC.parse(ops, entry.getSecond())
                                .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过大额多货币银行卡 {} 余额项，金额无法反序列化: {}", currencyId, message))
                                .ifPresent(amount -> result.put(currencyId, amount));
                    }));
            return DataResult.success(Pair.of(result, ops.empty()));
        }
    };

    public static final MapCodec<LargeMultiCurrencyBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            BALANCES_CODEC.fieldOf("balances").forGetter(LargeMultiCurrencyBankCard::getBalances))
            .apply(instance, LargeMultiCurrencyBankCard::new));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Map<Identifier, BigInteger> balances;

    // ==============================================
    // 构造方法
    // ==============================================

    public LargeMultiCurrencyBankCard(BankCardIdentity identity, CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(BigInteger.ZERO, currencyTypes));
    }

    public LargeMultiCurrencyBankCard(BankCardIdentity identity, Collection<CurrencyType> currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, BigInteger.ZERO));
    }

    protected LargeMultiCurrencyBankCard(BankCardIdentity identity, Map<Identifier, BigInteger> balances) {
        super(identity, CARD_TYPE_ID);
        this.balances = new HashMap<>();
        balances.forEach((type, amount) -> {
            Identifier currencyId = normalizeCurrencyId(type);
            if (currencyId != null) {
                this.balances.put(currencyId, normalizeBigAmount(amount));
            }
        });
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Collections.unmodifiableSet(balances.keySet());
    }

    @Override
    public BigInteger getCurrencyBalance(Identifier currencyTypeId) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        return currencyId == null ? BigInteger.ZERO : balances.getOrDefault(currencyId, BigInteger.ZERO);
    }

    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        return CurrencyStorageBankCard.saturatingLongValue(getCurrencyBalance(currencyTypeId));
    }

    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) return false;
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (currencyId == null || !balances.containsKey(currencyId)) return false;
        balances.computeIfPresent(currencyId, (_, current) -> current.add(amount));
        return true;
    }

    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        return increaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) return false;
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (currencyId == null || !balances.containsKey(currencyId)) return false;
        BigInteger current = balances.get(currencyId);
        if (current.compareTo(amount) < 0) return false;
        balances.put(currencyId, current.subtract(amount));
        return true;
    }

    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        return decreaseCurrency(currencyTypeId, BigInteger.valueOf(amount));
    }

    protected static Identifier normalizeCurrencyId(Identifier typeId) {
        CurrencyType type = CurrencyType.requireById(typeId);
        return type == null ? null : type.id();
    }

    protected static BigInteger normalizeBigAmount(BigInteger amount) {
        return amount == null || amount.signum() < 0 ? BigInteger.ZERO : amount;
    }
}

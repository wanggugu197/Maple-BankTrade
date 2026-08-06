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

import java.util.*;

/**
 * 多货币银行卡，保存 currency -> long 余额表。
 */
public class MultiCurrencyBankCard extends BankCard implements CurrencyStorageBankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 多货币银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("multi_currency");

    // ==============================================
    // Codec
    // ==============================================

    /** 余额表 Codec，读取时跳过未注册货币项。 */
    private static final Codec<Map<Identifier, Long>> BALANCES_CODEC = new Codec<>() {

        /** 严格编码余额表。 */
        @Override
        public <T> DataResult<T> encode(Map<Identifier, Long> input, DynamicOps<T> ops, T prefix) {
            return Codec.unboundedMap(CurrencyType.ID_CODEC, Codec.LONG).encode(input, ops, prefix);
        }

        /** 宽松解码余额表。 */
        @Override
        public <T> DataResult<Pair<Map<Identifier, Long>, T>> decode(DynamicOps<T> ops, T input) {
            return decodeBalances(ops, input);
        }
    };

    /** 多货币银行卡序列化编解码器。 */
    public static final MapCodec<MultiCurrencyBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            BALANCES_CODEC.fieldOf("balances").forGetter(MultiCurrencyBankCard::getBalances))
            .apply(instance, MultiCurrencyBankCard::new));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Map<Identifier, Long> balances;

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行、名称索引和固定货币表的多货币卡。 */
    public MultiCurrencyBankCard(BankCardIdentity identity, CurrencyType... currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(0L, currencyTypes));
    }

    /** 创建指定银行、名称索引和固定货币表的多货币卡。 */
    public MultiCurrencyBankCard(BankCardIdentity identity, Collection<CurrencyType> currencyTypes) {
        this(identity, CurrencyStorageBankCard.createInitialBalances(currencyTypes, 0L));
    }

    /** 从存档字段恢复多货币卡。 */
    protected MultiCurrencyBankCard(BankCardIdentity identity, Map<Identifier, Long> balances) {
        super(identity, CARD_TYPE_ID);
        this.balances = new HashMap<>();
        balances.forEach((type, amount) -> {
            Identifier currencyId = normalizeCurrencyId(type);
            if (currencyId != null) {
                this.balances.put(currencyId, normalizeLongAmount(amount));
            }
        });
    }

    // ==============================================
    // 查询方法
    // ==============================================

    /** 查询指定货币 ID 的余额。 */
    private long getBalance(Identifier typeId) {
        Identifier currencyId = normalizeCurrencyId(typeId);
        return currencyId == null ? 0L : balances.getOrDefault(currencyId, 0L);
    }

    // ==============================================
    // 统一货币接口
    // ==============================================

    /** 返回固定货币键集合。 */
    @Override
    public Set<Identifier> getSupportedCurrencyIds() {
        return Collections.unmodifiableSet(balances.keySet());
    }

    /** 查询指定货币 long 余额。 */
    @Override
    public long getCurrencyBalanceAsLong(Identifier currencyTypeId) {
        return getBalance(currencyTypeId);
    }

    /** 增加指定货币余额。 */
    @Override
    public boolean increaseCurrency(Identifier currencyTypeId, long amount) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (amount <= 0 || currencyId == null || !balances.containsKey(currencyId)) return false;

        long balance = balances.getOrDefault(currencyId, 0L);
        if (Long.MAX_VALUE - balance < amount) return false;
        balances.put(currencyId, balance + amount);
        return true;
    }

    /** 减少指定货币余额。 */
    @Override
    public boolean decreaseCurrency(Identifier currencyTypeId, long amount) {
        Identifier currencyId = normalizeCurrencyId(currencyTypeId);
        if (amount <= 0 || currencyId == null || !balances.containsKey(currencyId)) return false;

        long balance = balances.getOrDefault(currencyId, 0L);
        if (balance < amount) return false;
        balances.put(currencyId, balance - amount);
        return true;
    }

    // ==============================================
    // 工具方法
    // ==============================================

    /** 规范化货币 ID，未知货币返回 null 并由调用方跳过。 */
    protected static Identifier normalizeCurrencyId(Identifier typeId) {
        CurrencyType type = CurrencyType.requireById(typeId);
        return type == null ? null : type.id();
    }

    /** 宽松读取余额表，跳过无法解析或未注册的货币项。 */
    private static <T> DataResult<Pair<Map<Identifier, Long>, T>> decodeBalances(DynamicOps<T> ops, T input) {
        Map<Identifier, Long> result = new HashMap<>();
        ops.getMapValues(input)
                .resultOrPartial(message -> MapleBankTrade.LOGGER.error("无法读取多货币银行卡余额表，跳过全部余额: {}", message))
                .ifPresent(entries -> entries.forEach(entry -> {
                    Identifier currencyId = Identifier.CODEC.parse(ops, entry.getFirst())
                            .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过多货币银行卡余额项，货币 ID 无法反序列化: {}", message))
                            .orElse(null);
                    if (currencyId == null) return;
                    if (CurrencyType.requireById(currencyId) == null) {
                        MapleBankTrade.LOGGER.error("跳过多货币银行卡余额项，货币未注册: {}", currencyId);
                        return;
                    }
                    Codec.LONG.parse(ops, entry.getSecond())
                            .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过多货币银行卡 {} 余额项，金额无法反序列化: {}", currencyId, message)).ifPresent(amount -> result.put(currencyId, amount));
                }));
        return DataResult.success(Pair.of(result, ops.empty()));
    }

    /** 将 long 金额规范化为非负值。 */
    protected static long normalizeLongAmount(long amount) {
        return Math.max(0L, amount);
    }
}

package com.maple.maple_banktrade.api.bank.resource;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.*;

/**
 * {@link CurrencyStorageBankCard} 的 ResourceHandler 适配器。
 */
public final class BankCurrencyResourceHandler extends SnapshotJournal<BankCurrencyResourceHandler.CurrencySnapshot> implements ResourceHandler<CurrencyResource> {

    // ==============================================
    // 常量 / 字段
    // ==============================================

    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger ZERO = BigInteger.ZERO;

    private final CurrencyStorageBankCard card;
    private final Runnable onChanged;
    private final List<CurrencyResource> currencies;

    // ==============================================
    // 构造 / 工厂
    // ==============================================

    /** 创建处理器，变更时无回调。 */
    public BankCurrencyResourceHandler(CurrencyStorageBankCard card) {
        this(card, () -> {});
    }

    /** 创建处理器并在根事务提交后触发变更回调。 */
    public BankCurrencyResourceHandler(CurrencyStorageBankCard card, Runnable onChanged) {
        this.card = Objects.requireNonNull(card, "card");
        this.onChanged = Objects.requireNonNull(onChanged, "onChanged");
        this.currencies = card.getSupportedCurrencyIds().stream()
                .map(CurrencyResource::of)
                .filter(resource -> !resource.isEmpty())
                .sorted(Comparator.comparing(resource -> resource.currencyTypeId().toString()))
                .toList();
    }

    /** 若卡支持货币存储则创建处理器。 */
    public static BankCurrencyResourceHandler of(BankCard card) {
        return of(card, () -> {});
    }

    /** 若卡支持货币存储则创建处理器并绑定变更回调。 */
    public static BankCurrencyResourceHandler of(BankCard card, Runnable onChanged) {
        if (card instanceof CurrencyStorageBankCard currencyCard) {
            return new BankCurrencyResourceHandler(currencyCard, onChanged);
        }
        return null;
    }

    /** 获取底层货币卡。 */
    public CurrencyStorageBankCard card() {
        return card;
    }

    // ==============================================
    // ResourceHandler
    // ==============================================

    /** 槽位数量。 */
    @Override
    public int size() {
        return currencies.size();
    }

    /** 获取指定槽位的货币资源。 */
    @Override
    public @NonNull CurrencyResource getResource(int index) {
        return currencies.get(Objects.checkIndex(index, size()));
    }

    /** 以 long 查询槽位余额。 */
    @Override
    public long getAmountAsLong(int index) {
        CurrencyResource resource = getResource(index);
        return card.getCurrencyBalanceAsLong(resource.currencyTypeId());
    }

    /** 以 BigInteger 查询槽位余额。 */
    public BigInteger getAmountAsBigInteger(int index) {
        CurrencyResource resource = getResource(index);
        return card.getCurrencyBalance(resource.currencyTypeId());
    }

    /** 返回槽位容量（支持时为 Long.MAX_VALUE）。 */
    @Override
    public long getCapacityAsLong(int index, @NonNull CurrencyResource resource) {
        Objects.checkIndex(index, size());
        return resource.isEmpty() || isValid(index, resource) ? Long.MAX_VALUE : 0L;
    }

    /** 判断资源是否可写入该槽位。 */
    @Override
    public boolean isValid(int index, @NonNull CurrencyResource resource) {
        Objects.checkIndex(index, size());
        return !resource.isEmpty() && getResource(index).equals(resource) && card.supportsCurrency(resource.currencyTypeId());
    }

    /** int 版插入。 */
    @Override
    public int insert(int index, @NonNull CurrencyResource resource, int amount, @NonNull TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return (int) insertLong(index, resource, amount, transaction);
    }

    /** int 版提取。 */
    @Override
    public int extract(int index, @NonNull CurrencyResource resource, int amount, @NonNull TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return (int) extractLong(index, resource, amount, transaction);
    }

    // ==============================================
    // long / BigInteger 操作
    // ==============================================

    /** 按资源查找槽位并 long 插入。 */
    public long insertLong(CurrencyResource resource, long amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        int index = indexOf(resource);
        return index < 0 ? 0L : insertLong(index, resource, amount, transaction);
    }

    /** 指定槽位 long 插入。 */
    public long insertLong(int index, CurrencyResource resource, long amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        return insertBigInteger(index, resource, BigInteger.valueOf(amount), transaction).longValue();
    }

    /** 按资源查找槽位并 long 提取。 */
    public long extractLong(CurrencyResource resource, long amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        int index = indexOf(resource);
        return index < 0 ? 0L : extractLong(index, resource, amount, transaction);
    }

    /** 指定槽位 long 提取。 */
    public long extractLong(int index, CurrencyResource resource, long amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        return extractBigInteger(index, resource, BigInteger.valueOf(amount), transaction).longValue();
    }

    /** 按资源查找槽位并 BigInteger 插入。 */
    public BigInteger insertBigInteger(CurrencyResource resource, BigInteger amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        int index = indexOf(resource);
        return index < 0 ? ZERO : insertBigInteger(index, resource, amount, transaction);
    }

    /** 指定槽位 BigInteger 插入，必要时回退到 long 可表示范围。 */
    public BigInteger insertBigInteger(int index, CurrencyResource resource, BigInteger amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        Objects.requireNonNull(transaction, "transaction");
        Objects.checkIndex(index, size());
        if (amount.signum() == 0 || !isValid(index, resource)) return ZERO;

        updateSnapshots(transaction);
        ResourceLocation currencyId = resource.currencyTypeId();
        if (card.increaseCurrency(currencyId, amount)) {
            return amount;
        }

        BigInteger fallback = finiteLongRoom(currencyId).min(amount);
        if (fallback.signum() > 0 && !fallback.equals(amount) && card.increaseCurrency(currencyId, fallback)) {
            return fallback;
        }
        return ZERO;
    }

    /** 按资源查找槽位并 BigInteger 提取。 */
    public BigInteger extractBigInteger(CurrencyResource resource, BigInteger amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        int index = indexOf(resource);
        return index < 0 ? ZERO : extractBigInteger(index, resource, amount, transaction);
    }

    /** 指定槽位 BigInteger 提取，必要时回退到 long 可表示范围。 */
    public BigInteger extractBigInteger(int index, CurrencyResource resource, BigInteger amount, TransactionContext transaction) {
        checkNonEmptyNonNegative(resource, amount);
        Objects.requireNonNull(transaction, "transaction");
        Objects.checkIndex(index, size());
        if (amount.signum() == 0 || !isValid(index, resource)) return ZERO;

        ResourceLocation currencyId = resource.currencyTypeId();
        BigInteger extracted = card.getCurrencyBalance(currencyId).min(amount);
        if (extracted.signum() <= 0) return ZERO;

        updateSnapshots(transaction);
        if (card.decreaseCurrency(currencyId, extracted)) {
            return extracted;
        }

        BigInteger fallback = extracted.min(LONG_MAX);
        if (fallback.signum() > 0 && !fallback.equals(extracted) && card.decreaseCurrency(currencyId, fallback)) {
            return fallback;
        }
        return ZERO;
    }

    // ==============================================
    // 快照 / 工具
    // ==============================================

    /** 计算在 long 范围内仍可增加的空间。 */
    private BigInteger finiteLongRoom(ResourceLocation currencyId) {
        BigInteger current = card.getCurrencyBalance(currencyId);
        if (current.compareTo(LONG_MAX) >= 0) return ZERO;
        return LONG_MAX.subtract(current);
    }

    /** 创建当前余额快照。 */
    @Override
    protected CurrencySnapshot createSnapshot() {
        Map<ResourceLocation, BigInteger> balances = new LinkedHashMap<>();
        for (CurrencyResource resource : currencies) {
            balances.put(resource.currencyTypeId(), card.getCurrencyBalance(resource.currencyTypeId()));
        }
        return new CurrencySnapshot(balances);
    }

    /** 回滚到快照余额。 */
    @Override
    protected void revertToSnapshot(CurrencySnapshot snapshot) {
        if (snapshot != null) snapshot.balances().forEach(this::restoreBalance);
    }

    /** 根事务提交后若余额变化则回调。 */
    @Override
    protected void onRootCommit(CurrencySnapshot originalState) {
        if (!matchesCurrentBalances(originalState)) {
            onChanged.run();
        }
    }

    /** 判断快照是否与当前余额一致。 */
    private boolean matchesCurrentBalances(CurrencySnapshot snapshot) {
        for (Map.Entry<ResourceLocation, BigInteger> entry : snapshot.balances().entrySet()) {
            if (!card.getCurrencyBalance(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** 将单币种余额恢复到目标值。 */
    private void restoreBalance(ResourceLocation currencyId, BigInteger target) {
        BigInteger current = card.getCurrencyBalance(currencyId);
        int comparison = current.compareTo(target);
        if (comparison < 0) {
            BigInteger increase = target.subtract(current);
            if (!card.increaseCurrency(currencyId, increase)) {
                throw new IllegalStateException("Failed to restore currency balance for " + currencyId);
            }
        } else if (comparison > 0) {
            BigInteger decrease = current.subtract(target);
            if (!card.decreaseCurrency(currencyId, decrease)) {
                throw new IllegalStateException("Failed to restore currency balance for " + currencyId);
            }
        }
    }

    /** 查找资源对应槽位。 */
    private int indexOf(CurrencyResource resource) {
        return currencies.indexOf(resource);
    }

    /** 校验资源非空且金额非负（long）。 */
    private static void checkNonEmptyNonNegative(CurrencyResource resource, long amount) {
        TransferPreconditions.checkNonEmpty(resource);
        if (amount < 0L) {
            throw new IllegalArgumentException("Expected amount to be non-negative: " + amount);
        }
    }

    /** 校验资源非空且金额非负（BigInteger）。 */
    private static void checkNonEmptyNonNegative(CurrencyResource resource, BigInteger amount) {
        TransferPreconditions.checkNonEmpty(resource);
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Expected amount to be non-negative: " + amount);
        }
    }

    /** 余额快照。 */
    protected record CurrencySnapshot(Map<ResourceLocation, BigInteger> balances) {}
}

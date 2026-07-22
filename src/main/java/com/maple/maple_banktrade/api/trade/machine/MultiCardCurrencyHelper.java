package com.maple.maple_banktrade.api.trade.machine;

import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.maple.maple_banktrade.api.bank.resource.BankCurrencyResourceHandler;
import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

/**
 * 多银行卡货币扣款 / 入账（事务内，顺序敏感）。
 */
@UtilityClass
public class MultiCardCurrencyHelper {

    /**
     * 按 handler 迭代顺序分摊扣款，必须刚好扣满 {@code amount}。
     *
     * @return true 表示已扣满
     */
    public static boolean extractExact(@NonNull Collection<BankCurrencyResourceHandler> handlers,
                                       @NonNull CurrencyResource resource,
                                       @NonNull BigInteger amount,
                                       @NonNull TransactionContext tx) {
        if (resource.isEmpty() || amount.signum() <= 0) {
            return amount.signum() == 0;
        }

        BigInteger remaining = amount;
        for (BankCurrencyResourceHandler handler : handlers) {
            if (handler == null || remaining.signum() <= 0) continue;
            BigInteger extracted = handler.extractBigInteger(resource, remaining, tx);
            remaining = remaining.subtract(extracted);
        }
        return remaining.signum() == 0;
    }

    /**
     * 向第一张能完整收下该金额的卡入账；若无一卡可收满则失败。
     *
     * @return true 表示已入账成功
     */
    public static boolean insertExact(@NonNull Collection<BankCurrencyResourceHandler> handlers,
                                      @NonNull CurrencyResource resource,
                                      @NonNull BigInteger amount,
                                      @NonNull TransactionContext tx) {
        if (resource.isEmpty() || amount.signum() <= 0) {
            return amount.signum() == 0;
        }

        for (BankCurrencyResourceHandler handler : handlers) {
            if (handler == null) continue;
            BigInteger inserted = handler.insertBigInteger(resource, amount, tx);
            if (inserted.equals(amount)) {
                return true;
            }
            // insert 部分成功时依赖事务回滚；继续尝试下一张前，本事务内部分插入会在失败时整体回滚
            if (inserted.signum() > 0) {
                return false;
            }
        }
        return false;
    }

    /** 批量精确扣款。 */
    public static boolean extractAll(Collection<BankCurrencyResourceHandler> handlers,
                                     List<CurrencyIO> list,
                                     TransactionContext tx) {
        if (list == null || list.isEmpty()) return true;
        for (CurrencyIO io : list) {
            if (!extractExact(handlers, io.resource(), io.amount(), tx)) {
                return false;
            }
        }
        return true;
    }

    /** 批量精确入账。 */
    public static boolean insertAll(Collection<BankCurrencyResourceHandler> handlers,
                                    List<CurrencyIO> list,
                                    TransactionContext tx) {
        if (list == null || list.isEmpty()) return true;
        for (CurrencyIO io : list) {
            if (!insertExact(handlers, io.resource(), io.amount(), tx)) {
                return false;
            }
        }
        return true;
    }
}

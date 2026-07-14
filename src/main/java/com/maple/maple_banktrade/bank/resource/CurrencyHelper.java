package com.maple.maple_banktrade.bank.resource;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;

import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

import static com.maple.maple_banktrade.api.bank.MBTBankStates.getBankCards;

/**
 * 基于资源系统对银行卡执行货币存取的工具入口。
 */
public final class CurrencyHelper {

    /** 完整存入指定金额后提交，失败返回 0。 */
    public static BigInteger insertCurrency(MinecraftServer server, UUID cardUuid, Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) return BigInteger.ZERO;
        BankCardsWorldData data = getBankCards(server);
        BankCurrencyResourceHandler handler = currencyHandler(data, cardUuid);
        if (handler == null) return BigInteger.ZERO;

        CurrencyResource resource = CurrencyResource.of(currencyTypeId);
        if (resource.isEmpty()) return BigInteger.ZERO;
        try (Transaction transaction = Transaction.openRoot()) {
            BigInteger inserted = handler.insertBigInteger(resource, amount, transaction);
            if (inserted.equals(amount)) {
                transaction.commit();
                return inserted;
            }
            return BigInteger.ZERO;
        }
    }

    /** 完整取出指定金额后提交，失败返回 0。 */
    public static BigInteger extractCurrency(MinecraftServer server, UUID cardUuid, Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) return BigInteger.ZERO;
        BankCardsWorldData data = getBankCards(server);
        BankCurrencyResourceHandler handler = currencyHandler(data, cardUuid);
        if (handler == null) return BigInteger.ZERO;

        CurrencyResource resource = CurrencyResource.of(currencyTypeId);
        if (resource.isEmpty()) return BigInteger.ZERO;
        try (Transaction transaction = Transaction.openRoot()) {
            BigInteger extracted = handler.extractBigInteger(resource, amount, transaction);
            if (extracted.equals(amount)) {
                transaction.commit();
                return extracted;
            }
            return BigInteger.ZERO;
        }
    }

    /** 在指定维度服务端完整存入货币。 */
    public static BigInteger insertCurrency(ServerLevel level, UUID cardUuid, Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(level, "level");
        return insertCurrency(level.getServer(), cardUuid, currencyTypeId, amount);
    }

    /** 在指定维度服务端完整取出货币。 */
    public static BigInteger extractCurrency(ServerLevel level, UUID cardUuid, Identifier currencyTypeId, BigInteger amount) {
        Objects.requireNonNull(level, "level");
        return extractCurrency(level.getServer(), cardUuid, currencyTypeId, amount);
    }

    /** long 版完整存入。 */
    public static long insertCurrency(MinecraftServer server, UUID cardUuid, Identifier currencyTypeId, long amount) {
        return insertCurrency(server, cardUuid, currencyTypeId, BigInteger.valueOf(amount)).longValue();
    }

    /** long 版完整取出。 */
    public static long extractCurrency(MinecraftServer server, UUID cardUuid, Identifier currencyTypeId, long amount) {
        return extractCurrency(server, cardUuid, currencyTypeId, BigInteger.valueOf(amount)).longValue();
    }

    /** long 版在指定维度完整存入。 */
    public static long insertCurrency(ServerLevel level, UUID cardUuid, Identifier currencyTypeId, long amount) {
        return insertCurrency(level, cardUuid, currencyTypeId, BigInteger.valueOf(amount)).longValue();
    }

    /** long 版在指定维度完整取出。 */
    public static long extractCurrency(ServerLevel level, UUID cardUuid, Identifier currencyTypeId, long amount) {
        return extractCurrency(level, cardUuid, currencyTypeId, BigInteger.valueOf(amount)).longValue();
    }

    /** 为指定银行卡创建资源处理器。 */
    private static BankCurrencyResourceHandler currencyHandler(BankCardsWorldData data, UUID cardUuid) {
        BankCard card = data.getCard(cardUuid);
        return BankCurrencyResourceHandler.of(card, data::setDirty);
    }

    private CurrencyHelper() {}
}

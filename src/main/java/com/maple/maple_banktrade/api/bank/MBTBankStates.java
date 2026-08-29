package com.maple.maple_banktrade.api.bank;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 银行系统的世界数据注册入口。
 */
public final class MBTBankStates {

    // ==============================================
    // 查询
    // ==============================================

    /** 获取全服银行数据。 */
    public static BankCardsWorldData getBankCards(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        return getBankCards(level.getServer());
    }

    /** 获取全服银行数据（MinecraftServer 全局 SavedDataStorage，1.21.1 使用主世界存储）。 */
    public static BankCardsWorldData getBankCards(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return server.overworld().getDataStorage().computeIfAbsent(BankCardsWorldData.FACTORY, BankCardsWorldData.DATA_NAME);
    }

    // ==============================================
    // 修改
    // ==============================================

    /** 修改全服银行数据。 */
    public static void modifyBankCards(ServerLevel level, Consumer<BankCardsWorldData> action) {
        Objects.requireNonNull(level, "level");
        modifyBankCards(level.getServer(), action);
    }

    /** 修改全服银行数据，回调结束后标记需保存。 */
    public static void modifyBankCards(MinecraftServer server, Consumer<BankCardsWorldData> action) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(action, "action");
        BankCardsWorldData data = getBankCards(server);
        try {
            action.accept(data);
        } finally {
            data.setDirty();
        }
    }

    // ==============================================
    // 快捷脏标记
    // ==============================================

    /**
     * 标记全服银行数据需要保存。
     * 用于 {@link com.maple.maple_banktrade.api.bank.resource.BankCurrencyResourceHandler}
     * 等无需执行修改逻辑、仅需通知持久化的场景。
     */
    public static void markDirty(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        getBankCards(server).setDirty();
    }

    /** 标记全服银行数据需要保存（ServerLevel 重载）。 */
    public static void markDirty(ServerLevel level) {
        Objects.requireNonNull(level, "level");
        markDirty(level.getServer());
    }

    // ==============================================
    // 初始化
    // ==============================================

    /** 触发静态注册。 */
    public static void init() {}

    /** 禁止实例化工具类。 */
    private MBTBankStates() {}
}

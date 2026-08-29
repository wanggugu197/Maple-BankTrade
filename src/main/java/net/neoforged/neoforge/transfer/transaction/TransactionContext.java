package net.neoforged.neoforge.transfer.transaction;

/**
 * 兼容层：模拟 NeoForge 26.x 的事务上下文标记接口。
 * <p>
 * 1.21.1 的 NeoForge 没有事务 API，本兼容层仅在模组内部传递，
 * 具体回滚/提交逻辑见 {@link Transaction}。
 * </p>
 */
public interface TransactionContext {}

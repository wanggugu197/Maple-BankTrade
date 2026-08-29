package net.neoforged.neoforge.transfer.resource;

/**
 * 兼容层：模拟 NeoForge 26.x 的传输资源抽象。
 * <p>
 * 该包（以及整个 {@code net.neoforged.neoforge.transfer} 子树）是本次
 * 26.1.2 → 1.21.1 移植时为本模组提供的 API 兼容层，仅用于编译与运行，
 * 并不存在于 NeoForge 21.1.238 中。
 * </p>
 */
public interface Resource {

    /** 是否为空资源。 */
    boolean isEmpty();
}

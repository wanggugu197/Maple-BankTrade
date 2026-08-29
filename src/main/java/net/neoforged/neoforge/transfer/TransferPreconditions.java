package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.resource.Resource;

/**
 * 兼容层：模拟 NeoForge 26.x 的传输前置条件校验。
 */
public final class TransferPreconditions {

    private TransferPreconditions() {}

    /** 校验资源非空。 */
    public static void checkNonEmpty(@org.jspecify.annotations.Nullable Object resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Expected resource to not be null");
        }
        if (resource instanceof Resource r && r.isEmpty()) {
            throw new IllegalArgumentException("Expected resource to not be empty");
        }
    }

    /** 校验资源非空且数量非负。 */
    public static void checkNonEmptyNonNegative(@org.jspecify.annotations.Nullable Object resource, int amount) {
        checkNonEmpty(resource);
        if (amount < 0) {
            throw new IllegalArgumentException("Expected amount to be non-negative: " + amount);
        }
    }
}

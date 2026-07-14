package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.util.StringRepresentable;

import org.jspecify.annotations.NonNull;

/**
 * 玩家对银行卡的权限级别：OWNER &gt; ADMIN &gt; USABLE &gt; UNUSABLE。
 */
public enum BankCardPermission implements StringRepresentable {

    // ==============================================
    // 枚举值
    // ==============================================

    /** 拥有者：完整管理权与使用权。 */
    OWNER("owner"),
    /** 管理员：可使用并管理授权（不能取代拥有者）。 */
    ADMIN("admin"),
    /** 被授权使用，不可管理权限。 */
    USABLE("usable"),
    /** 明确记录为不可使用。 */
    UNUSABLE("unusable");

    // ==============================================
    // 字段
    // ==============================================

    /** 序列化名称。 */
    private final String name;

    // ==============================================
    // 构造
    // ==============================================

    /** 创建权限枚举并保存序列化名称。 */
    BankCardPermission(String name) {
        this.name = name;
    }

    // ==============================================
    // 权限
    // ==============================================

    /** 是否可使用银行卡（OWNER / ADMIN / USABLE）。 */
    public boolean canUse() {
        return this == OWNER || this == ADMIN || this == USABLE;
    }

    /** 是否可管理该卡（OWNER / ADMIN）；转让拥有者等另限 OWNER。 */
    public boolean canManage() {
        return this == OWNER || this == ADMIN;
    }

    /** 是否为卡的拥有者。 */
    public boolean isOwner() {
        return this == OWNER;
    }

    /** 权限等级（数值越大越高）。 */
    public int rank() {
        return switch (this) {
            case OWNER -> 3;
            case ADMIN -> 2;
            case USABLE -> 1;
            case UNUSABLE -> 0;
        };
    }

    /** 按权限等级从高到低比较；等级相同返回 0。 */
    public static int compareRankDesc(BankCardPermission a, BankCardPermission b) {
        int ra = a == null ? BankCardPermission.UNUSABLE.rank() : a.rank();
        int rb = b == null ? BankCardPermission.UNUSABLE.rank() : b.rank();
        return Integer.compare(rb, ra);
    }

    // ==============================================
    // 序列化
    // ==============================================

    /** 返回用于存档和网络传输的权限名。 */
    @Override
    public @NonNull String getSerializedName() {
        return name;
    }

    /** 获取权限显示名称对应的翻译键。 */
    public String getTranslationKey() {
        return "bank_card_permission.maple_banktrade." + name;
    }

    /** 按序列化名称解析权限，未知时返回 UNUSABLE。 */
    public static BankCardPermission bySerializedName(String name) {
        for (BankCardPermission permission : values()) {
            if (permission.name.equals(name)) {
                return permission;
            }
        }
        return UNUSABLE;
    }
}

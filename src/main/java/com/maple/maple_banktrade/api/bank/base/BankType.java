package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import com.maple.maple_banktrade.MapleBankTrade;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 银行分类，以 ResourceLocation 为主键。
 */
public record BankType(ResourceLocation id) implements StringRepresentable {

    // ==============================================
    // 注册表
    // ==============================================

    /** 银行类型注册表：id -> BankType，按注册顺序保存。 */
    private static final Map<ResourceLocation, BankType> BANK_TYPE_MAP = new LinkedHashMap<>();

    // ==============================================
    // 构造
    // ==============================================

    /** 创建银行类型并校验银行 ID。 */
    public BankType {
        if (id == null) {
            throw new IllegalArgumentException("bank type id must not be null");
        }
    }

    // ==============================================
    // 注册
    // ==============================================

    /** 触发静态注册入口。 */
    public static void init() {}

    /** 注册银行类型，重复注册时返回已有类型。 */
    public static BankType register(ResourceLocation id) {
        if (BANK_TYPE_MAP.containsKey(id)) {
            MapleBankTrade.LOGGER.error("Bank type with id {} already exists", id);
            return BANK_TYPE_MAP.get(id);
        }
        BankType type = new BankType(id);
        BANK_TYPE_MAP.put(id, type);
        return type;
    }

    // ==============================================
    // 序列化
    // ==============================================

    /** 返回用于存档和网络传输的类型名。 */
    @Override
    public @NonNull String getSerializedName() {
        return id.toString();
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 查询已注册类型；未知类型返回 null。 */
    public static BankType requireById(ResourceLocation id) {
        return BANK_TYPE_MAP.get(id);
    }

    /** 获取全部已注册银行类型。 */
    public static Collection<BankType> values() {
        return Collections.unmodifiableCollection(BANK_TYPE_MAP.values());
    }
}

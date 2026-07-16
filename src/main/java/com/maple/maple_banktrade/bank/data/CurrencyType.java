package com.maple.maple_banktrade.bank.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * 银行卡货币类型，以 Identifier 为键统一注册。
 * <p>
 * {@code currencyTexture} 可为 {@link IGuiTexture#dynamic} 包装的 lazy 图标
 * （如 {@code ItemStackTexture(Items.*)}，首次绘制时再构造）。
 * </p>
 */
public record CurrencyType(Identifier id,
                           List<Component> description,
                           IGuiTexture currencyTexture,
                           IGuiTexture backgroundTexture)
        implements StringRepresentable {

    /** 预留初始化入口。 */
    public static void init() {}

    // ==============================================
    // 注册表
    // ==============================================

    private static final Map<Identifier, CurrencyType> REGISTRY = new LinkedHashMap<>();
    /** 仅允许已注册货币 ID 通过。 */
    public static final Codec<Identifier> ID_CODEC = Identifier.CODEC.flatXmap(CurrencyType::decodeId, CurrencyType::decodeId);

    // ==============================================
    // 构造方法
    // ==============================================

    /** 校验 ID 并归一化显示信息。 */
    public CurrencyType {
        if (id == null) {
            throw new IllegalArgumentException("currency type id must not be null");
        }
        description = description == null ? List.of() : List.copyOf(description);
        currencyTexture = currencyTexture == null ? IGuiTexture.EMPTY : currencyTexture;
    }

    /** 生成货币翻译键。 */
    public static String getTranslationKey(Identifier id) {
        return "currency." + id.getNamespace() + "." + id.getPath();
    }

    /** 生成货币翻译键。 */
    public Component getHoverName() {
        return Component.translatable("currency." + id.getNamespace() + "." + id.getPath());
    }

    // ==============================================
    // 注册方法
    // ==============================================

    /** 注册货币类型；重复注册时返回已有实例。 */
    public static CurrencyType CurrencyTypeRegister(Identifier id, String cnName, String enName, List<Component> description, IGuiTexture currencyTexture, IGuiTexture backgroundTexture) {
        if (REGISTRY.containsKey(id)) {
            MapleBankTrade.LOGGER.error("Currency type with id {} already exists", id);
            return REGISTRY.get(id);
        }
        MBTLangHandler.addLang(getTranslationKey(id), cnName, enName);
        return register(id, description, currencyTexture, backgroundTexture);
    }

    /** 写入注册表。 */
    private static CurrencyType register(Identifier id, List<Component> description, IGuiTexture currencyTexture, IGuiTexture backgroundTexture) {
        CurrencyType type = new CurrencyType(id, description, currencyTexture, backgroundTexture);
        REGISTRY.put(id, type);
        return type;
    }

    // ==============================================
    // 序列化
    // ==============================================

    /** 返回存档与网络用类型名。 */
    @Override
    public @NonNull String getSerializedName() {
        return id.toString();
    }

    // ==============================================
    // 查询方法
    // ==============================================

    /** 查询已注册类型；未知类型不自动注册。 */
    public static Optional<CurrencyType> findById(Identifier id) {
        return Optional.ofNullable(REGISTRY.get(Objects.requireNonNull(id, "id")));
    }

    /** 查询已注册类型；未知类型返回 null。 */
    public static CurrencyType requireById(Identifier id) {
        return id == null ? null : REGISTRY.get(id);
    }

    /** 获取全部已注册货币类型。 */
    public static Collection<CurrencyType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /** 解析并规范化已注册货币 ID。 */
    private static DataResult<Identifier> decodeId(Identifier id) {
        return findById(id)
                .map(type -> DataResult.success(type.id()))
                .orElseGet(() -> DataResult.error(() -> "Unknown currency type: " + id));
    }
}

package com.maple.maple_banktrade.api.bank.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.maple.maple_banktrade.MapleBankTrade;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * 银行卡侧可交易类型的显示元数据。
 * <p>
 * {@code tradableIcon} 建议用 {@link IGuiTexture#dynamic} 延迟构造
 * {@code ItemStackTexture(Items.*)}，避免加载期 components 未绑定。
 * </p>
 */
public record TradableType(Identifier id,
                           List<Component> description,
                           IGuiTexture tradableIcon,
                           IGuiTexture backgroundTexture)
        implements StringRepresentable {

    /** 预留初始化入口。 */
    public static void init() {}

    // ==============================================
    // 注册表
    // ==============================================

    private static final Map<Identifier, TradableType> REGISTRY = new LinkedHashMap<>();

    // ==============================================
    // 构造方法
    // ==============================================

    /** 校验 ID 并归一化显示信息。 */
    public TradableType {
        if (id == null) {
            throw new IllegalArgumentException("tradable type id must not be null");
        }
        description = description == null ? List.of() : List.copyOf(description);
        tradableIcon = tradableIcon == null ? IGuiTexture.EMPTY : tradableIcon;
        backgroundTexture = backgroundTexture == null ? IGuiTexture.EMPTY : backgroundTexture;
    }

    /** 生成可交易类型翻译键。 */
    public static String getTradableTypeTranslationKey(Identifier id) {
        return "tradable." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /** 本类型显示名（翻译组件）。 */
    public Component getDisplayName() {
        return Component.translatable(getTradableTypeTranslationKey(id));
    }

    /** 按 ID 取显示名；未注册时回退为 path 末段的字面量。 */
    public static Component getDisplayName(Identifier id) {
        TradableType type = requireById(id);
        if (type != null) {
            return type.getDisplayName();
        }
        if (id == null) {
            return Component.empty();
        }
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String shortId = slash >= 0 && slash + 1 < path.length() ? path.substring(slash + 1) : path;
        return Component.literal(shortId);
    }

    // ==============================================
    // 注册方法
    // ==============================================

    /** 注册交易类型显示信息；重复注册时返回已有实例。 */
    public static TradableType TradableTypeRegister(Identifier id,
                                                    List<Component> description,
                                                    IGuiTexture tradableIcon,
                                                    IGuiTexture backgroundTexture) {
        if (REGISTRY.containsKey(id)) {
            MapleBankTrade.LOGGER.error("Tradable type with id {} already exists", id);
            return REGISTRY.get(id);
        }
        return register(id, description, tradableIcon, backgroundTexture);
    }

    /** 写入注册表。 */
    private static TradableType register(Identifier id,
                                         List<Component> description,
                                         IGuiTexture tradableIcon,
                                         IGuiTexture backgroundTexture) {
        TradableType type = new TradableType(id, description, tradableIcon, backgroundTexture);
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

    /** 查询已注册类型；未知类型返回 null。 */
    public static TradableType requireById(Identifier id) {
        return id == null ? null : REGISTRY.get(id);
    }

    /** 获取全部已注册交易类型。 */
    public static Collection<TradableType> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }
}

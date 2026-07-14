package com.maple.maple_banktrade.api.bank.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.style.BasicStyle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 银行类型的客户端显示信息。
 */
public record BankInfo(BankType type, Component name, List<Component> description, IGuiTexture background) {

    // ==============================================
    // 常量
    // ==============================================

    /** 银行槽位透明按钮的 CSS 类名。 */
    public static final String BANK_SLOT_BUTTON_CLASS = "mbt-bank-slot-button";

    // ==============================================
    // 注册表
    // ==============================================

    /** 银行类型到显示信息的映射。 */
    private static final Map<BankType, BankInfo> REGISTRY = new HashMap<>();

    // ==============================================
    // 构造
    // ==============================================

    /** 创建银行显示信息并归一化可选显示字段。 */
    public BankInfo {
        Objects.requireNonNull(type, "type");
        name = name == null ? Component.translatable(getTranslationKey(type)) : name;
        description = description == null ? List.of() : List.copyOf(description);
        background = background == null ? Sprites.RECT_RD_T : background;
    }

    // ==============================================
    // 注册
    // ==============================================

    /** 触发静态注册入口。 */
    public static void init() {}

    /** 注册银行类型的显示信息。 */
    public static void register(BankInfo info) {
        if (info == null) return;
        REGISTRY.put(info.type(), info);
    }

    /** 按路径与贴图注册银行显示信息与双语文本。 */
    public static void registerBankInfo(BankType type, String path, String texturePath,
                                        String cnName, String enName,
                                        String cnDescription, String enDescription) {
        String key = "bank.maple_banktrade." + path;
        register(new BankInfo(
                type,
                MBTLangHandler.addLang(key, cnName, enName),
                List.of(MBTLangHandler.addLang(key + ".desc", cnDescription, enDescription)),
                SpriteTexture.of("maple_banktrade:textures/gui/bank/" + texturePath + ".png")));
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 根据银行类型查询显示信息，未注册时退回到序列化名称。 */
    public static BankInfo of(BankType type) {
        if (type == null) return null;
        BankInfo info = REGISTRY.get(type);
        return info == null ? new BankInfo(type, Component.translatable(getTranslationKey(type)), List.of(), Sprites.RECT_RD_T) : info;
    }

    /** 获取银行显示名称的翻译键。 */
    private static String getTranslationKey(BankType type) {
        Identifier id = type.id();
        return "bank." + id.getNamespace() + "." + id.getPath();
    }

    // ==============================================
    // 显示
    // ==============================================

    /** 组装银行 tooltip：名称、描述与持卡数量。 */
    private Component[] createBankTooltips(int cardCount) {
        Component[] components = new Component[description.size() + 2];
        components[0] = name;
        for (int i = 0; i < description.size(); i++) {
            components[i + 1] = description.get(i);
        }
        components[components.length - 1] = Component.translatable("tooltip.bank.card_count", cardCount);
        return components;
    }

    /** 应用银行背景与持卡数量 tooltip。 */
    public void applyBankStyle(BasicStyle style, int cardCount) {
        style.backgroundTexture(background);
        style.tooltips(createBankTooltips(cardCount));
    }

    /** 应用空银行槽位样式。 */
    public static void applyEmptyBankStyle(BasicStyle style) {
        style.backgroundTexture(Sprites.RECT_RD_T);
        style.tooltips();
    }

    /** 透明点击层；尺寸见 bank.lss（.mbt-bank-slot-button）。 */
    public static Button createTransparentButton(Consumer<Button> buttonSetup) {
        Button button = new Button().setText("", false);
        button.addClass(BANK_SLOT_BUTTON_CLASS);
        if (buttonSetup != null) {
            buttonSetup.accept(button);
        }
        return button;
    }
}

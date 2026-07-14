package com.maple.maple_banktrade.api.bank.data;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.maple.maple_banktrade.api.bank.base.BankCard;

import java.util.HashMap;
import java.util.Map;

/**
 * 银行卡名称索引对应的客户端 UI 与贴图信息。
 */
public record CardInfo(Identifier nameIndex,
                       IGuiTexture simplifiedButtonBackground,
                       IGuiTexture detailBackground,
                       CardUIFactory simplifiedUIFactory,
                       CardUIFactory detailsUIFactory) {

    // ==============================================
    // 注册表
    // ==============================================

    /** 名称索引到卡面显示信息的映射。 */
    private static final Map<Identifier, CardInfo> REGISTRY = new HashMap<>();

    // ==============================================
    // 注册
    // ==============================================

    /** 触发静态注册入口。 */
    public static void init() {}

    /** 注册银行卡的列表/详情背景与 UI 工厂。 */
    public static void register(Identifier nameIndex,
                                IGuiTexture simplifiedButtonBackground,
                                IGuiTexture detailBackground,
                                CardUIFactory simplifiedUIFactory,
                                CardUIFactory detailsUIFactory) {
        CardInfo cardInfo = new CardInfo(nameIndex, simplifiedButtonBackground, detailBackground, simplifiedUIFactory, detailsUIFactory);
        if (nameIndex != null) {
            REGISTRY.put(nameIndex, cardInfo);
        }
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 根据名称索引查询卡面信息，未注册返回 null。 */
    public static CardInfo of(Identifier nameIndex) {
        return REGISTRY.get(nameIndex);
    }

    // ==============================================
    // 内部类型
    // ==============================================

    /** 卡面 UI 工厂：根据玩家与银行卡创建 UI 元素。 */
    @FunctionalInterface
    public interface CardUIFactory {

        /** 创建卡面 UI 元素。 */
        UIElement create(Player player, BankCard card);
    }
}

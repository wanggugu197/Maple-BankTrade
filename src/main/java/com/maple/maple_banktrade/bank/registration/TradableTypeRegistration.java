package com.maple.maple_banktrade.bank.registration;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.bank.data.TradableType;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;
import com.maple.maple_banktrade.trade.registration.CurrencyItemTradeRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * 可交易类型显示包装注册（银行卡 / UI 侧）。
 * <p>
 * {@code tradableIcon} 使用 lazy {@link ItemStackTexture}，首次绘制时再构造。
 * </p>
 */
public final class TradableTypeRegistration {

    // ==============================================
    // 交易类型
    // ==============================================

    public static final TradableType STONE_AND_ORES = register(
            CurrencyItemTradeRegistration.STONE_AND_ORES.id(),
            Items.RAW_IRON,
            Sprites.RECT_RD_LIGHT,
            "石料与矿石", "Stone & Ores",
            "买卖石料、矿物与锭类物品。", "Trade stone, ores, and ingots.");

    public static final TradableType PLANTS_AND_FOOD = register(
            CurrencyItemTradeRegistration.PLANTS_AND_FOOD.id(),
            Items.WHEAT,
            Sprites.RECT_RD,
            "植物与食物", "Plants & Food",
            "买卖农作物、食材与食物。", "Trade crops, ingredients, and food.");

    public static final TradableType MOB_DROPS = register(
            CurrencyItemTradeRegistration.MOB_DROPS.id(),
            Items.ROTTEN_FLESH,
            Sprites.RECT_RD_T,
            "生物掉落", "Mob Drops",
            "买卖生物掉落物与材料。", "Trade mob drops and materials.");

    /** 预留初始化入口（静态字段加载时已完成注册）。 */
    public static void init() {}

    // ==============================================
    // 注册
    // ==============================================

    /**
     * @param iconItem        类型图标物品（lazy ItemStackTexture → tradableIcon）
     * @param panelBackground 面板背景（Sprites）
     */
    private static TradableType register(Identifier id,
                                         Item iconItem,
                                         IGuiTexture panelBackground,
                                         String nameCn,
                                         String nameEn,
                                         String... description) {
        List<Component> descriptionComponents = new ArrayList<>();
        String s = "tradable." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        MBTLangHandler.addLang(s, nameCn, nameEn);
        for (int i = 0; i < description.length / 2; i++) {
            descriptionComponents.add(MBTLangHandler.addLang(s + "." + i, description[i * 2], description[i * 2 + 1]));
        }
        IGuiTexture icon = IGuiTexture.dynamic(() -> new ItemStackTexture(iconItem));
        return TradableType.TradableTypeRegister(id, descriptionComponents, icon, panelBackground);
    }

    private TradableTypeRegistration() {}
}

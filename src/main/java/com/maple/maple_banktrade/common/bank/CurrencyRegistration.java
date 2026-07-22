package com.maple.maple_banktrade.common.bank;

import net.minecraft.world.item.Items;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;

import java.util.ArrayList;

/**
 * 内置货币类型注册。
 * <p>
 * 基于原版物品的图标用 {@link IGuiTexture#dynamic} 延迟构造 {@link ItemStackTexture}，
 * 避免加载期 {@code Holder.components()} 未绑定导致 NPE。
 * </p>
 */
public class CurrencyRegistration {

    // ==============================================
    // 货币
    // ==============================================

    public static final CurrencyType COINS = CurrencyType.register(
            MapleBankTrade.id("coins"),
            "金币", "Coins",
            new ArrayList<>(),
            SpriteTexture.of("maple_banktrade:textures/item/coins.png"),
            Sprites.RECT_RD_LIGHT);

    public static final CurrencyType GOLD = CurrencyType.register(
            MapleBankTrade.id("gold"),
            "黄金", "Gold",
            new ArrayList<>(),
            IGuiTexture.dynamic(() -> new ItemStackTexture(Items.GOLD_INGOT)),
            Sprites.RECT_RD_LIGHT);

    public static final CurrencyType DIAMONDS = CurrencyType.register(
            MapleBankTrade.id("diamonds"),
            "钻石", "Diamonds",
            new ArrayList<>(),
            IGuiTexture.dynamic(() -> new ItemStackTexture(Items.DIAMOND)),
            Sprites.RECT_RD_LIGHT);

    /** 预留初始化入口（静态字段加载时已完成注册）。 */
    public static void init() {}
}

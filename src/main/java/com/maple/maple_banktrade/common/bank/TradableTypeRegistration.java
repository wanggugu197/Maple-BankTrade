package com.maple.maple_banktrade.common.bank;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.api.bank.data.TradableType;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 可交易类型显示包装注册（银行卡 / 交易站 UI 侧）。
 * <p>
 * {@code tradableIcon} 使用 lazy {@link ItemStackTexture}，首次绘制时再构造。
 * 名称键：{@code tradable.&lt;namespace&gt;.&lt;path 以 . 分隔&gt;}。
 * </p>
 */
public final class TradableTypeRegistration {

    /** 预留初始化入口（静态字段加载时已完成注册）。 */
    public static void init() {
        // ==============================================
        // 货币物品交易类型（银行卡）
        // ==============================================

        register(
                TradeTypeRegistration.STONE_AND_ORES.id(),
                Items.RAW_IRON,
                Sprites.RECT_RD_LIGHT,
                "石料与矿石", "Stone & Ores",
                "买卖石料、矿物与锭类物品。", "Trade stone, ores, and ingots.");

        register(
                TradeTypeRegistration.PLANTS_AND_FOOD.id(),
                Items.WHEAT,
                Sprites.RECT_RD,
                "植物与食物", "Plants & Food",
                "买卖农作物、食材与食物。", "Trade crops, ingredients, and food.");

        register(
                TradeTypeRegistration.MOB_DROPS.id(),
                Items.ROTTEN_FLESH,
                Sprites.RECT_RD_T,
                "生物掉落", "Mob Drops",
                "买卖生物掉落物与材料。", "Trade mob drops and materials.");

        // ==============================================
        // 机器交易类型（交易站标签页）
        // ==============================================

        register(
                TradeTypeRegistration.MACHINE_BENCH.id(),
                Items.BLAST_FURNACE,
                Sprites.RECT_RD_LIGHT,
                "电冶台", "Smelter",
                "消耗能量将原矿冶炼为金属锭。", "Smelt raw ores into metal ingots using energy.");

        register(
                TradeTypeRegistration.MACHINE_WASHER.id(),
                Items.WATER_BUCKET,
                Sprites.RECT_RD,
                "水洗台", "Washer",
                "使用流体清洗或处理物品。", "Wash or process items with fluids.");

        register(
                TradeTypeRegistration.MACHINE_FORGE.id(),
                Items.ANVIL,
                Sprites.RECT_RD_T,
                "锻压台", "Forge",
                "压缩、成型与精炼类加工。", "Compress, form, and refine materials.");

        register(
                TradeTypeRegistration.MACHINE_BANK.id(),
                Items.GOLD_INGOT,
                Sprites.RECT_RD_LIGHT,
                "银行台", "Bank Desk",
                "铸币与货币买卖。", "Mint coins and trade with currency.");

        register(
                TradeTypeRegistration.MACHINE_POWER.id(),
                Items.BLAZE_ROD,
                Sprites.RECT_RD,
                "能量台", "Power Unit",
                "将燃料或流体转化为能量。", "Convert fuel or fluids into energy.");

        register(
                TradeTypeRegistration.MACHINE_ITEM_DESK.id(),
                Items.CHEST,
                Sprites.RECT_RD_LIGHT,
                "物品柜", "Item Desk",
                "仅物品与银行卡货币的买卖，无需能量与流体。",
                "Trade items with bank-card currency; no energy or fluid required.");

        register(
                TradeTypeRegistration.AUTO_SELL_ORES_LAVA.id(),
                Items.RAW_IRON,
                Sprites.RECT_RD_LIGHT,
                "自动出售·矿物岩浆", "Auto-Sell Ores & Lava",
                "自动将矿石、矿物、锭与岩浆出售为硬币。",
                "Automatically sell ores, minerals, ingots, and lava for coins.");

        register(
                TradeTypeRegistration.AUTO_SELL_MOB_DROPS.id(),
                Items.ROTTEN_FLESH,
                Sprites.RECT_RD,
                "自动出售·怪物掉落", "Auto-Sell Mob Drops",
                "自动将怪物掉落物出售为硬币。",
                "Automatically sell mob drops for coins.");
    }

    // ==============================================
    // 注册
    // ==============================================

    /**
     * @param iconItem        类型图标物品（lazy ItemStackTexture → tradableIcon）
     * @param panelBackground 面板背景（Sprites）
     * @param description     中英文描述对：cn0, en0, cn1, en1, …
     */
    private static void register(ResourceLocation id,
                                 Item iconItem,
                                 IGuiTexture panelBackground,
                                 String nameCn,
                                 String nameEn,
                                 String... description) {
        List<Component> descriptionComponents = new ArrayList<>();
        String nameKey = TradableType.getTradableTypeTranslationKey(id);
        MBTLangHandler.addLang(nameKey, nameCn, nameEn);
        for (int i = 0; i < description.length / 2; i++) {
            descriptionComponents.add(MBTLangHandler.addLang(
                    nameKey + "." + i,
                    description[i * 2],
                    description[i * 2 + 1]));
        }
        IGuiTexture icon = IGuiTexture.dynamic(() -> new ItemStackTexture(iconItem));
        TradableType.TradableTypeRegister(id, descriptionComponents, icon, panelBackground);
    }

    private TradableTypeRegistration() {}
}

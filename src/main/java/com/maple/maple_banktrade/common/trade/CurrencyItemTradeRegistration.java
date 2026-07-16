package com.maple.maple_banktrade.common.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTrade;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTrade.Mode;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTradeStorage;
import com.maple.maple_banktrade.trade.currency_item.CurrencyItemTradeType;

/**
 * 注册内置货币-物品交易类型与价目条目。
 */
public final class CurrencyItemTradeRegistration {

    // ==============================================
    // 交易类型
    // ==============================================

    public static final CurrencyItemTradeType STONE_AND_ORES = new CurrencyItemTradeType(MapleBankTrade.id("trade_type/stone_and_ores"));
    public static final CurrencyItemTradeType PLANTS_AND_FOOD = new CurrencyItemTradeType(MapleBankTrade.id("trade_type/plants_and_food"));
    public static final CurrencyItemTradeType MOB_DROPS = new CurrencyItemTradeType(MapleBankTrade.id("trade_type/mob_drops"));

    private static boolean registered;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册全部内置交易类型与价目（幂等）。 */
    public static void init() {
        if (registered) return;
        registered = true;

        fill(STONE_AND_ORES,
                e("cobblestone", Items.COBBLESTONE, 1),
                e("stone", Items.STONE, 1),
                e("deepslate", Items.DEEPSLATE, 1),
                e("granite", Items.GRANITE, 1),
                e("diorite", Items.DIORITE, 1),
                e("andesite", Items.ANDESITE, 1),
                e("coal", Items.COAL, 4),
                e("raw_copper", Items.RAW_COPPER, 6),
                e("raw_iron", Items.RAW_IRON, 12),
                e("raw_gold", Items.RAW_GOLD, 20),
                e("copper_ingot", Items.COPPER_INGOT, 8),
                e("iron_ingot", Items.IRON_INGOT, 30),
                e("gold_ingot", Items.GOLD_INGOT, 40),
                e("redstone", Items.REDSTONE, 5),
                e("lapis_lazuli", Items.LAPIS_LAZULI, 6),
                e("quartz", Items.QUARTZ, 8),
                e("amethyst_shard", Items.AMETHYST_SHARD, 10),
                e("diamond", Items.DIAMOND, 150),
                e("emerald", Items.EMERALD, 120),
                e("netherite_scrap", Items.NETHERITE_SCRAP, 200),
                e("ancient_debris", Items.ANCIENT_DEBRIS, 250));

        fill(PLANTS_AND_FOOD,
                e("wheat_seeds", Items.WHEAT_SEEDS, 1),
                e("wheat", Items.WHEAT, 2),
                e("carrot", Items.CARROT, 3),
                e("potato", Items.POTATO, 3),
                e("beetroot", Items.BEETROOT, 2),
                e("sugar_cane", Items.SUGAR_CANE, 2),
                e("cocoa_beans", Items.COCOA_BEANS, 3),
                e("pumpkin", Items.PUMPKIN, 5),
                e("melon_slice", Items.MELON_SLICE, 2),
                e("sweet_berries", Items.SWEET_BERRIES, 2),
                e("kelp", Items.KELP, 1),
                e("dried_kelp", Items.DRIED_KELP, 2),
                e("apple", Items.APPLE, 4),
                e("bread", Items.BREAD, 6),
                e("cooked_beef", Items.COOKED_BEEF, 10),
                e("cooked_porkchop", Items.COOKED_PORKCHOP, 10),
                e("cooked_chicken", Items.COOKED_CHICKEN, 8),
                e("golden_carrot", Items.GOLDEN_CARROT, 40));

        fill(MOB_DROPS,
                e("rotten_flesh", Items.ROTTEN_FLESH, 1),
                e("bone", Items.BONE, 3),
                e("bone_meal", Items.BONE_MEAL, 1),
                e("string", Items.STRING, 2),
                e("spider_eye", Items.SPIDER_EYE, 5),
                e("feather", Items.FEATHER, 2),
                e("leather", Items.LEATHER, 6),
                e("ink_sac", Items.INK_SAC, 3),
                e("glow_ink_sac", Items.GLOW_INK_SAC, 8),
                e("gunpowder", Items.GUNPOWDER, 8),
                e("slime_ball", Items.SLIME_BALL, 10),
                e("ender_pearl", Items.ENDER_PEARL, 40),
                e("blaze_rod", Items.BLAZE_ROD, 30),
                e("ghast_tear", Items.GHAST_TEAR, 50),
                e("phantom_membrane", Items.PHANTOM_MEMBRANE, 25),
                e("shulker_shell", Items.SHULKER_SHELL, 80));
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 注册类型并向存储器填充价目条目。 */
    private static void fill(CurrencyItemTradeType type, CoinEntry... entries) {
        CurrencyItemTradeStorage storage = type.register();
        Identifier coins = CurrencyRegistration.COINS.id();
        for (CoinEntry entry : entries) {
            storage.register(entry.path(), CurrencyItemTrade.of(entry.item(), coins, entry.price(), Mode.BUY_AND_SELL));
        }
    }

    /** 构造价目临时条目。 */
    private static CoinEntry e(String path, Item item, long price) {
        return new CoinEntry(path, item, price);
    }

    /**
     * 内置价目临时数据。
     */
    private record CoinEntry(String path, Item item, long price) {}

    private CurrencyItemTradeRegistration() {}
}

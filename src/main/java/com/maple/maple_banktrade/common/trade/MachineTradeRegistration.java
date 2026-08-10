package com.maple.maple_banktrade.common.trade;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeBuilder;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.common.bank.CardRegistration;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;

import static com.maple.maple_banktrade.common.trade.TradeTypeRegistration.*;

/**
 * 注册内置机器多资源交易类型与示例条目。
 */
public final class MachineTradeRegistration {

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册机器交易类型与内置条目（幂等）。 */
    public static void register() {
        CurrencyResource coins = CurrencyResource.of(CurrencyRegistration.COINS.id());
        registerBench();
        registerWasher();
        registerForge();
        registerBank(coins);
        registerPower();
        registerItemDesk(coins);
        registerAutoSellOresLava(coins);
        registerAutoSellMobDrops(coins);
    }

    private static void registerBench() {
        MachineBench.register("smelt_raw_iron", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.RAW_IRON, 1))
                .addItemOutput(ItemIO.of(Items.IRON_INGOT, 1))
                .energyExtract(200)
                .build());

        MachineBench.register("smelt_raw_gold", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.RAW_GOLD, 1))
                .addItemOutput(ItemIO.of(Items.GOLD_INGOT, 1))
                .energyExtract(250)
                .build());

        MachineBench.register("smelt_raw_copper", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.RAW_COPPER, 1))
                .addItemOutput(ItemIO.of(Items.COPPER_INGOT, 1))
                .energyExtract(150)
                .build());

        MachineBench.register("smelt_raw_iron_bulk", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.RAW_IRON, 8))
                .addItemOutput(ItemIO.of(Items.IRON_INGOT, 8))
                .energyExtract(1400)
                .build());
    }

    private static void registerWasher() {
        MachineWasher.register("wash_gravel", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.GRAVEL, 1))
                .addFluidInput(FluidIO.of(Fluids.WATER, 250))
                .addItemOutput(ItemIO.of(Items.FLINT, 1))
                .addItemOutput(ItemIO.of(Items.SAND, 1))
                .energyExtract(80)
                .build());

        MachineWasher.register("wash_sand", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.SAND, 4))
                .addFluidInput(FluidIO.of(Fluids.WATER, 500))
                .addItemOutput(ItemIO.of(Items.CLAY_BALL, 2))
                .energyExtract(100)
                .build());

        MachineWasher.register("cool_magma", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.MAGMA_BLOCK, 1))
                .addFluidInput(FluidIO.of(Fluids.WATER, 1000))
                .addItemOutput(ItemIO.of(Items.OBSIDIAN, 1))
                .energyExtract(200)
                .build());
    }

    private static void registerForge() {
        MachineForge.register("compact_cobblestone", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.COBBLESTONE, 9))
                .addItemOutput(ItemIO.of(Items.STONE, 1))
                .energyExtract(50)
                .build());

        MachineForge.register("form_quartz_block", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.QUARTZ, 4))
                .addItemOutput(ItemIO.of(Items.QUARTZ_BLOCK, 1))
                .energyExtract(120)
                .build());

        MachineForge.register("stone_to_sandstone", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.STONE, 4))
                .addItemOutput(ItemIO.of(Items.SANDSTONE, 1))
                .build());

        MachineForge.register("refine_iron_block", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 9))
                .addItemInput(ItemIO.of(Items.REDSTONE, 4))
                .addFluidInput(FluidIO.of(Fluids.WATER, 500))
                .energyExtract(400)
                .addItemOutput(ItemIO.of(Items.IRON_BLOCK, 1))
                .build());
    }

    private static void registerBank(CurrencyResource coins) {
        MachineBank.register("mint_coins", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.GOLD_INGOT, 1))
                .energyExtract(100)
                .addCurrencyInsert(CurrencyIO.of(coins, 40))
                .build());

        MachineBank.register("buy_diamond", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 150))
                .addItemOutput(ItemIO.of(Items.DIAMOND, 1))
                .energyExtract(50)
                .build());

        MachineBank.register("buy_emerald", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 80))
                .addItemOutput(ItemIO.of(Items.EMERALD, 1))
                .energyExtract(30)
                .build());

        MachineBank.register("sell_diamond", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.DIAMOND, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 100))
                .energyExtract(20)
                .build());
    }

    private static void registerPower() {
        MachinePower.register("lava_to_energy", new MachineTradeBuilder()
                .addFluidInput(FluidIO.of(Fluids.LAVA, 100))
                .energyInsert(500)
                .build());

        MachinePower.register("coal_to_energy", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.COAL, 1))
                .energyInsert(1_000)
                .build());

        MachinePower.register("blaze_to_energy", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.BLAZE_ROD, 1))
                .energyInsert(4_000)
                .build());

        MachinePower.register("coal_block_to_energy", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.COAL_BLOCK, 1))
                .energyInsert(10_000)
                .build());
    }

    /** 物品柜：只依赖物品与货币，无能量/流体；手动买卖。 */
    private static void registerItemDesk(CurrencyResource coins) {
        MachineItemDesk.register("sell_iron", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 20))
                .build());

        MachineItemDesk.register("sell_gold", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.GOLD_INGOT, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 30))
                .build());

        MachineItemDesk.register("sell_diamond", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.DIAMOND, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 100))
                .build());

        MachineItemDesk.register("buy_diamond", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 150))
                .addItemOutput(ItemIO.of(Items.DIAMOND, 1))
                .build());

        MachineItemDesk.register("buy_emerald", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 80))
                .addItemOutput(ItemIO.of(Items.EMERALD, 1))
                .build());

        MachineItemDesk.register("buy_bread", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 6))
                .addItemOutput(ItemIO.of(Items.BREAD, 1))
                .build());

        MachineItemDesk.register("buy_ccccc", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 1))
                .addItemOutput(ItemIO.of(Items.GOLD_INGOT, 1))
                .build());

        MachineItemDesk.register("buy_aaa", new MachineTradeBuilder()
                .addCurrencyExtract(CurrencyIO.of(coins, 6))
                .addItemOutput(ItemIO.of(Items.DIAMOND_BLOCK, 1))
                .visibility(CheckHasRegister.VISIBILITY_FILTER_BY_NAME,
                        CheckHasRegister.createVisibilityFilterByNameCompoundTag(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex()))
                .build());

        MachineItemDesk.register("buy_bbb", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.EMERALD, 1))
                .addItemOutput(ItemIO.of(Items.DIAMOND_BLOCK, 1))
                .visibility(CheckHasRegister.VISIBILITY_FILTER_BY_NAME,
                        CheckHasRegister.createVisibilityFilterByNameCompoundTag(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex()))
                .build());

        MachineItemDesk.register("buy_dddd", new MachineTradeBuilder()
                .addItemInput(ItemIO.of(Items.CLAY, 1))
                .addItemOutput(ItemIO.of(Items.EMERALD, 1))
                .build());
    }

    /**
     * 自动出售矿物 + 岩浆（价目与货币-物品交易大致对齐，岩浆按桶/份出售）。
     */
    private static void registerAutoSellOresLava(CurrencyResource coins) {
        // 原矿 / 矿物
        autoSellItem(AutoSellOresLava, coins, "sell_coal", Items.COAL, 4);
        autoSellItem(AutoSellOresLava, coins, "sell_raw_copper", Items.RAW_COPPER, 6);
        autoSellItem(AutoSellOresLava, coins, "sell_raw_iron", Items.RAW_IRON, 12);
        autoSellItem(AutoSellOresLava, coins, "sell_raw_gold", Items.RAW_GOLD, 20);
        autoSellItem(AutoSellOresLava, coins, "sell_copper_ingot", Items.COPPER_INGOT, 8);
        autoSellItem(AutoSellOresLava, coins, "sell_iron_ingot", Items.IRON_INGOT, 30);
        autoSellItem(AutoSellOresLava, coins, "sell_gold_ingot", Items.GOLD_INGOT, 40);
        autoSellItem(AutoSellOresLava, coins, "sell_redstone", Items.REDSTONE, 5);
        autoSellItem(AutoSellOresLava, coins, "sell_lapis", Items.LAPIS_LAZULI, 6);
        autoSellItem(AutoSellOresLava, coins, "sell_quartz", Items.QUARTZ, 8);
        autoSellItem(AutoSellOresLava, coins, "sell_amethyst", Items.AMETHYST_SHARD, 10);
        autoSellItem(AutoSellOresLava, coins, "sell_diamond", Items.DIAMOND, 150);
        autoSellItem(AutoSellOresLava, coins, "sell_emerald", Items.EMERALD, 120);
        autoSellItem(AutoSellOresLava, coins, "sell_netherite_scrap", Items.NETHERITE_SCRAP, 200);
        autoSellItem(AutoSellOresLava, coins, "sell_ancient_debris", Items.ANCIENT_DEBRIS, 250);
        autoSellItem(AutoSellOresLava, coins, "sell_coal_ore", Items.COAL_ORE, 8);
        autoSellItem(AutoSellOresLava, coins, "sell_iron_ore", Items.IRON_ORE, 15);
        autoSellItem(AutoSellOresLava, coins, "sell_gold_ore", Items.GOLD_ORE, 25);
        autoSellItem(AutoSellOresLava, coins, "sell_copper_ore", Items.COPPER_ORE, 8);
        autoSellItem(AutoSellOresLava, coins, "sell_diamond_ore", Items.DIAMOND_ORE, 180);
        autoSellItem(AutoSellOresLava, coins, "sell_emerald_ore", Items.EMERALD_ORE, 140);
        autoSellItem(AutoSellOresLava, coins, "sell_redstone_ore", Items.REDSTONE_ORE, 12);
        autoSellItem(AutoSellOresLava, coins, "sell_lapis_ore", Items.LAPIS_ORE, 14);
        autoSellItem(AutoSellOresLava, coins, "sell_nether_quartz_ore", Items.NETHER_QUARTZ_ORE, 10);

        // 岩浆：单流体输入 autoTrade
        AutoSellOresLava.register("sell_lava", new MachineTradeBuilder()
                .addFluidInput(FluidIO.of(Fluids.LAVA, 1000))
                .addCurrencyInsert(CurrencyIO.of(coins, 25))
                .autoTrade(true)
                .build());
    }

    /**
     * 自动出售怪物掉落物（价目与 {@code mob_drops} 货币交易对齐）。
     */
    private static void registerAutoSellMobDrops(CurrencyResource coins) {
        autoSellItem(AutoSellMobDrops, coins, "sell_rotten_flesh", Items.ROTTEN_FLESH, 1);
        autoSellItem(AutoSellMobDrops, coins, "sell_bone", Items.BONE, 3);
        autoSellItem(AutoSellMobDrops, coins, "sell_bone_meal", Items.BONE_MEAL, 1);
        autoSellItem(AutoSellMobDrops, coins, "sell_string", Items.STRING, 2);
        autoSellItem(AutoSellMobDrops, coins, "sell_spider_eye", Items.SPIDER_EYE, 5);
        autoSellItem(AutoSellMobDrops, coins, "sell_feather", Items.FEATHER, 2);
        autoSellItem(AutoSellMobDrops, coins, "sell_leather", Items.LEATHER, 6);
        autoSellItem(AutoSellMobDrops, coins, "sell_ink_sac", Items.INK_SAC, 3);
        autoSellItem(AutoSellMobDrops, coins, "sell_glow_ink_sac", Items.GLOW_INK_SAC, 8);
        autoSellItem(AutoSellMobDrops, coins, "sell_gunpowder", Items.GUNPOWDER, 8);
        autoSellItem(AutoSellMobDrops, coins, "sell_slime_ball", Items.SLIME_BALL, 10);
        autoSellItem(AutoSellMobDrops, coins, "sell_ender_pearl", Items.ENDER_PEARL, 40);
        autoSellItem(AutoSellMobDrops, coins, "sell_blaze_rod", Items.BLAZE_ROD, 30);
        autoSellItem(AutoSellMobDrops, coins, "sell_ghast_tear", Items.GHAST_TEAR, 50);
        autoSellItem(AutoSellMobDrops, coins, "sell_phantom_membrane", Items.PHANTOM_MEMBRANE, 25);
        autoSellItem(AutoSellMobDrops, coins, "sell_shulker_shell", Items.SHULKER_SHELL, 80);
        autoSellItem(AutoSellMobDrops, coins, "sell_prismarine_shard", Items.PRISMARINE_SHARD, 4);
        autoSellItem(AutoSellMobDrops, coins, "sell_prismarine_crystals", Items.PRISMARINE_CRYSTALS, 6);
        autoSellItem(AutoSellMobDrops, coins, "sell_magma_cream", Items.MAGMA_CREAM, 12);
        autoSellItem(AutoSellMobDrops, coins, "sell_wither_skeleton_skull", Items.WITHER_SKELETON_SKULL, 200);
    }

    /** 注册单物品输入、货币产出的 autoTrade 出售条目。 */
    private static void autoSellItem(
                                     MachineTradeStorage storage,
                                     CurrencyResource coins,
                                     String path,
                                     Item item,
                                     long price) {
        storage.register(path, new MachineTradeBuilder()
                .addItemInput(ItemIO.of(item, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, price))
                .autoTrade(true)
                .build());
    }

    private MachineTradeRegistration() {}
}

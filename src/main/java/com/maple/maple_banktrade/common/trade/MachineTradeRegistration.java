package com.maple.maple_banktrade.common.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.trade.machine.MachineTrade;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ItemIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.trade.machine.MachineTradeType;

import java.util.List;

/**
 * 注册内置机器多资源交易类型与示例条目。
 */
public final class MachineTradeRegistration {

    // ==============================================
    // 交易类型
    // ==============================================

    /** 电冶：矿石 / 原料 + 能量 → 锭。 */
    public static final MachineTradeType MACHINE_BENCH = new MachineTradeType(MapleBankTrade.id("trade_type/machine_bench"));

    /** 水洗：物品 + 流体 → 产物。 */
    public static final MachineTradeType MACHINE_WASHER = new MachineTradeType(MapleBankTrade.id("trade_type/machine_washer"));

    /** 锻压成型：压缩 / 精炼 / 成型。 */
    public static final MachineTradeType MACHINE_FORGE = new MachineTradeType(MapleBankTrade.id("trade_type/machine_forge"));

    /** 银行台：铸币 / 采购等货币交易。 */
    public static final MachineTradeType MACHINE_BANK = new MachineTradeType(MapleBankTrade.id("trade_type/machine_bank"));

    /** 能量台：流体 / 燃料 → 能量。 */
    public static final MachineTradeType MACHINE_POWER = new MachineTradeType(MapleBankTrade.id("trade_type/machine_power"));

    /**
     * 物品柜：仅物品 + 银行卡货币，无流体/能量需求。
     * 供 {@code ItemCardTradingStation} 使用。
     */
    public static final MachineTradeType MACHINE_ITEM_DESK = new MachineTradeType(MapleBankTrade.id("trade_type/machine_item_desk"));

    /** 全功能交易站默认绑定的类型（顺序 = UI 标签页顺序）。 */
    public static final List<Identifier> TRADING_STATION_TYPES = List.of(
            MACHINE_BENCH.id(),
            MACHINE_WASHER.id(),
            MACHINE_FORGE.id(),
            MACHINE_BANK.id(),
            MACHINE_POWER.id());

    /** 物品+卡贸易站绑定的类型。 */
    public static final List<Identifier> ITEM_CARD_TRADING_STATION_TYPES = List.of(
            MACHINE_ITEM_DESK.id());

    private static boolean registered;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册机器交易类型与内置条目（幂等）。 */
    public static void init() {
        if (registered) return;
        registered = true;

        CurrencyResource coins = CurrencyResource.of(CurrencyRegistration.COINS.id());
        registerBench();
        registerWasher();
        registerForge();
        registerBank(coins);
        registerPower();
        registerItemDesk(coins);
    }

    private static void registerBench() {
        MachineTradeStorage storage = MACHINE_BENCH.register();

        storage.register("smelt_raw_iron", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.RAW_IRON, 1))
                .addItemOutput(ItemIO.of(Items.IRON_INGOT, 1))
                .energyExtract(200)
                .build());

        storage.register("smelt_raw_gold", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.RAW_GOLD, 1))
                .addItemOutput(ItemIO.of(Items.GOLD_INGOT, 1))
                .energyExtract(250)
                .build());

        storage.register("smelt_raw_copper", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.RAW_COPPER, 1))
                .addItemOutput(ItemIO.of(Items.COPPER_INGOT, 1))
                .energyExtract(150)
                .build());

        storage.register("smelt_raw_iron_bulk", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.RAW_IRON, 8))
                .addItemOutput(ItemIO.of(Items.IRON_INGOT, 8))
                .energyExtract(1400)
                .build());
    }

    private static void registerWasher() {
        MachineTradeStorage storage = MACHINE_WASHER.register();

        storage.register("wash_gravel", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.GRAVEL, 1))
                .addFluidInput(FluidIO.of(Fluids.WATER, 250))
                .addItemOutput(ItemIO.of(Items.FLINT, 1))
                .addItemOutput(ItemIO.of(Items.SAND, 1))
                .energyExtract(80)
                .build());

        storage.register("wash_sand", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.SAND, 4))
                .addFluidInput(FluidIO.of(Fluids.WATER, 500))
                .addItemOutput(ItemIO.of(Items.CLAY_BALL, 2))
                .energyExtract(100)
                .build());

        storage.register("cool_magma", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.MAGMA_BLOCK, 1))
                .addFluidInput(FluidIO.of(Fluids.WATER, 1000))
                .addItemOutput(ItemIO.of(Items.OBSIDIAN, 1))
                .energyExtract(200)
                .build());
    }

    private static void registerForge() {
        MachineTradeStorage storage = MACHINE_FORGE.register();

        storage.register("compact_cobblestone", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.COBBLESTONE, 9))
                .addItemOutput(ItemIO.of(Items.STONE, 1))
                .energyExtract(50)
                .build());

        storage.register("form_quartz_block", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.QUARTZ, 4))
                .addItemOutput(ItemIO.of(Items.QUARTZ_BLOCK, 1))
                .energyExtract(120)
                .build());

        storage.register("stone_to_sandstone", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.STONE, 4))
                .addItemOutput(ItemIO.of(Items.SANDSTONE, 1))
                .build());

        storage.register("refine_iron_block", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 9))
                .addItemInput(ItemIO.of(Items.REDSTONE, 4))
                .addFluidInput(FluidIO.of(Fluids.WATER, 500))
                .energyExtract(400)
                .addItemOutput(ItemIO.of(Items.IRON_BLOCK, 1))
                .build());
    }

    private static void registerBank(CurrencyResource coins) {
        MachineTradeStorage storage = MACHINE_BANK.register();

        storage.register("mint_coins", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.GOLD_INGOT, 1))
                .energyExtract(100)
                .addCurrencyInsert(CurrencyIO.of(coins, 40))
                .build());

        storage.register("buy_diamond", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 150))
                .addItemOutput(ItemIO.of(Items.DIAMOND, 1))
                .energyExtract(50)
                .build());

        storage.register("buy_emerald", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 80))
                .addItemOutput(ItemIO.of(Items.EMERALD, 1))
                .energyExtract(30)
                .build());

        storage.register("sell_diamond", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.DIAMOND, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 100))
                .energyExtract(20)
                .build());
    }

    private static void registerPower() {
        MachineTradeStorage storage = MACHINE_POWER.register();

        storage.register("lava_to_energy", MachineTrade.builder()
                .addFluidInput(FluidIO.of(Fluids.LAVA, 100))
                .energyInsert(500)
                .build());

        storage.register("coal_to_energy", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.COAL, 1))
                .energyInsert(1_000)
                .build());

        storage.register("blaze_to_energy", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.BLAZE_ROD, 1))
                .energyInsert(4_000)
                .build());

        storage.register("coal_block_to_energy", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.COAL_BLOCK, 1))
                .energyInsert(10_000)
                .build());
    }

    /** 物品柜：只依赖物品与货币，无能量/流体。 */
    private static void registerItemDesk(CurrencyResource coins) {
        MachineTradeStorage storage = MACHINE_ITEM_DESK.register();

        storage.register("sell_iron", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 20))
                .build());

        storage.register("sell_gold", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.GOLD_INGOT, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 30))
                .build());

        storage.register("sell_diamond", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.DIAMOND, 1))
                .addCurrencyInsert(CurrencyIO.of(coins, 100))
                .build());

        storage.register("buy_diamond", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 150))
                .addItemOutput(ItemIO.of(Items.DIAMOND, 1))
                .build());

        storage.register("buy_emerald", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 80))
                .addItemOutput(ItemIO.of(Items.EMERALD, 1))
                .build());

        storage.register("buy_bread", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 6))
                .addItemOutput(ItemIO.of(Items.BREAD, 1))
                .build());
    }

    private MachineTradeRegistration() {}
}

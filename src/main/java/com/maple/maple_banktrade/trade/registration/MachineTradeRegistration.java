package com.maple.maple_banktrade.trade.registration;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.bank.registration.CurrencyRegistration;
import com.maple.maple_banktrade.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.trade.machine.MachineTrade;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ItemIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.trade.machine.MachineTradeType;

/**
 * 注册内置机器多资源交易类型与示例条目。
 */
public final class MachineTradeRegistration {

    // ==============================================
    // 交易类型
    // ==============================================

    /** 通用加工台：物品 / 流体 / 能量 / 货币组合交易。 */
    public static final MachineTradeType MACHINE_BENCH = new MachineTradeType(MapleBankTrade.id("trade_type/machine_bench"));

    private static boolean registered;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册机器交易类型与内置条目（幂等）。 */
    public static void init() {
        if (registered) return;
        registered = true;

        MachineTradeStorage storage = MACHINE_BENCH.register();
        CurrencyResource coins = CurrencyResource.of(CurrencyRegistration.COINS.id());

        // 矿石电冶：原料 + 能量 → 锭
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

        // 压缩：9 圆石 + 少量能量 → 1 石头
        storage.register("compact_cobblestone", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.COBBLESTONE, 9))
                .addItemOutput(ItemIO.of(Items.STONE, 1))
                .energyExtract(50)
                .build());

        // 水洗砂砾：砂砾 + 水 → 燧石 + 沙子
        storage.register("wash_gravel", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.GRAVEL, 1))
                .addFluidInput(FluidIO.of(Fluids.WATER, 250))
                .addItemOutput(ItemIO.of(Items.FLINT, 1))
                .addItemOutput(ItemIO.of(Items.SAND, 1))
                .energyExtract(80)
                .build());

        // 熔岩充能：熔岩 → 能量回充（无物品产出）
        storage.register("lava_to_energy", MachineTrade.builder()
                .addFluidInput(FluidIO.of(Fluids.LAVA, 100))
                .energyInsert(500)
                .build());

        // 铸币：金锭 + 能量 → 银行金币入账
        storage.register("mint_coins", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.GOLD_INGOT, 1))
                .energyExtract(100)
                .addCurrencyInsert(CurrencyIO.of(coins, 40))
                .build());

        // 采购：扣金币 → 钻石
        storage.register("buy_diamond", MachineTrade.builder()
                .addCurrencyExtract(CurrencyIO.of(coins, 150))
                .addItemOutput(ItemIO.of(Items.DIAMOND, 1))
                .energyExtract(50)
                .build());

        // 精炼：铁锭 + 红石 + 水 + 金币 + 能量 → 铁块
        storage.register("refine_iron_block", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.IRON_INGOT, 9))
                .addItemInput(ItemIO.of(Items.REDSTONE, 4))
                .addFluidInput(FluidIO.of(Fluids.WATER, 500))
                .addCurrencyExtract(CurrencyIO.of(coins, 20))
                .energyExtract(400)
                .addItemOutput(ItemIO.of(Items.IRON_BLOCK, 1))
                .build());

        // 熔炼石英：下界石英 + 能量 → 石英块
        storage.register("form_quartz_block", MachineTrade.builder()
                .addItemInput(ItemIO.of(Items.QUARTZ, 4))
                .addItemOutput(ItemIO.of(Items.QUARTZ_BLOCK, 1))
                .energyExtract(120)
                .build());
    }

    private MachineTradeRegistration() {}
}

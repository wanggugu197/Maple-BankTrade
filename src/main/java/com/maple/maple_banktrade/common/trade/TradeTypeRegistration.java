package com.maple.maple_banktrade.common.trade;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.currency_item.CurrencyItemTradeStorage;
import com.maple.maple_banktrade.api.trade.currency_item.CurrencyItemTradeType;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeType;

import java.util.List;

public class TradeTypeRegistration {

    public static void init() {}

    // ==============================================
    // 交易类型
    // ==============================================

    public static final CurrencyItemTradeType STONE_AND_ORES = CurrencyItemTradeType.of(MapleBankTrade.id("stone_and_ores"));
    public static final CurrencyItemTradeStorage StoneAndOres = STONE_AND_ORES.register();
    public static final CurrencyItemTradeType PLANTS_AND_FOOD = CurrencyItemTradeType.of(MapleBankTrade.id("plants_and_food"));
    public static final CurrencyItemTradeStorage PlantsAndFood = PLANTS_AND_FOOD.register();
    public static final CurrencyItemTradeType MOB_DROPS = CurrencyItemTradeType.of(MapleBankTrade.id("mob_drops"));
    public static final CurrencyItemTradeStorage MobDrops = MOB_DROPS.register();

    // ==============================================
    // 交易类型
    // ==============================================

    /** 电冶：矿石 / 原料 + 能量 → 锭。 */
    public static final MachineTradeType MACHINE_BENCH = MachineTradeType.of(MapleBankTrade.id("machine_bench"));
    public static final MachineTradeStorage MachineBench = MACHINE_BENCH.register();
    /** 水洗：物品 + 流体 → 产物。 */
    public static final MachineTradeType MACHINE_WASHER = MachineTradeType.of(MapleBankTrade.id("machine_washer"));
    public static final MachineTradeStorage MachineWasher = MACHINE_WASHER.register();
    /** 锻压成型：压缩 / 精炼 / 成型。 */
    public static final MachineTradeType MACHINE_FORGE = MachineTradeType.of(MapleBankTrade.id("machine_forge"));
    public static final MachineTradeStorage MachineForge = MACHINE_FORGE.register();
    /** 银行台：铸币 / 采购等货币交易。 */
    public static final MachineTradeType MACHINE_BANK = MachineTradeType.of(MapleBankTrade.id("machine_bank"));
    public static final MachineTradeStorage MachineBank = MACHINE_BANK.register();
    /** 能量台：流体 / 燃料 → 能量。 */
    public static final MachineTradeType MACHINE_POWER = MachineTradeType.of(MapleBankTrade.id("machine_power"));
    public static final MachineTradeStorage MachinePower = MACHINE_POWER.register();
    /** 全功能交易站默认绑定的类型（顺序 = UI 标签页顺序）。 */
    public static final List<Identifier> TRADING_STATION_TYPES = List.of(
            MACHINE_BENCH.id(),
            MACHINE_WASHER.id(),
            MACHINE_FORGE.id(),
            MACHINE_BANK.id(),
            MACHINE_POWER.id());
    /**
     * 物品柜：仅物品 + 银行卡货币；手动交易。
     * 供 {@code ItemCardTradingStation} 使用（不开启自动交易）。
     */
    public static final MachineTradeType MACHINE_ITEM_DESK = MachineTradeType.of(MapleBankTrade.id("machine_item_desk"));
    public static final MachineTradeStorage MachineItemDesk = MACHINE_ITEM_DESK.register();
    /** 物品+卡贸易站绑定的类型。 */
    public static final List<Identifier> ITEM_CARD_TRADING_STATION_TYPES = List.of(
            MACHINE_ITEM_DESK.id());
    /**
     * 自动出售：矿物 / 原矿 / 矿锭 + 岩浆 → 硬币。
     * 允许 {@code autoTrade} 单输入条目。
     */
    public static final MachineTradeType AUTO_SELL_ORES_LAVA = MachineTradeType.of(MapleBankTrade.id("auto_sell_ores_lava"), true);
    public static final MachineTradeStorage AutoSellOresLava = AUTO_SELL_ORES_LAVA.register();
    /**
     * 自动出售：怪物掉落物 → 硬币。
     * 允许 {@code autoTrade} 单输入条目。
     */
    public static final MachineTradeType AUTO_SELL_MOB_DROPS = MachineTradeType.of(MapleBankTrade.id("auto_sell_mob_drops"), true);
    public static final MachineTradeStorage AutoSellMobDrops = AUTO_SELL_MOB_DROPS.register();
    /** 自动贸易站绑定的类型（顺序 = UI 标签页顺序）。 */
    public static final List<Identifier> AUTO_TRADING_STATION_TYPES = List.of(
            AUTO_SELL_ORES_LAVA.id(),
            AUTO_SELL_MOB_DROPS.id());
}

package com.maple.maple_banktrade.common.trade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;
import com.maple.maple_banktrade.common.bank.CardRegistration;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.trade.hooks.checkHook.*;
import com.maple.maple_banktrade.trade.hooks.stateHook.*;
import com.maple.maple_banktrade.trade.hooks.successHook.*;

import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

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
        MachineBench.register(MachineTrade.builder(MapleBankTrade.id("smelt_raw_iron"))
                .addItemInput(Items.RAW_IRON, 1)
                .addItemOutput(Items.IRON_INGOT, 1)
                .energyExtract(200)
                .build());

        MachineBench.register(MachineTrade.builder(MapleBankTrade.id("smelt_raw_gold"))
                .addItemInput(Items.RAW_GOLD, 1)
                .addItemOutput(Items.GOLD_INGOT, 1)
                .energyExtract(250)
                .build());

        MachineBench.register(MachineTrade.builder(MapleBankTrade.id("smelt_raw_copper"))
                .addItemInput(Items.RAW_COPPER, 1)
                .addItemOutput(Items.COPPER_INGOT, 1)
                .energyExtract(150)
                .build());

        MachineBench.register(MachineTrade.builder(MapleBankTrade.id("smelt_raw_iron_bulk"))
                .addItemInput(Items.RAW_IRON, 8)
                .addItemOutput(Items.IRON_INGOT, 8)
                .energyExtract(1400)
                .build());
    }

    private static void registerWasher() {
        MachineWasher.register(MachineTrade.builder(MapleBankTrade.id("wash_gravel"))
                .addItemInput(Items.GRAVEL, 1)
                .addFluidInput(Fluids.WATER, 250)
                .addItemOutput(Items.FLINT, 1)
                .addItemOutput(Items.SAND, 1)
                .energyExtract(80)
                .build());

        MachineWasher.register(MachineTrade.builder(MapleBankTrade.id("wash_sand"))
                .addItemInput(Items.SAND, 4)
                .addFluidInput(Fluids.WATER, 500)
                .addItemOutput(Items.CLAY_BALL, 2)
                .energyExtract(100)
                .build());

        MachineWasher.register(MachineTrade.builder(MapleBankTrade.id("cool_magma"))
                .addItemInput(Items.MAGMA_BLOCK, 1)
                .addFluidInput(Fluids.WATER, 1000)
                .addItemOutput(Items.OBSIDIAN, 1)
                .energyExtract(200)
                .build());
    }

    private static void registerForge() {
        MachineForge.register(MachineTrade.builder(MapleBankTrade.id("compact_cobblestone"))
                .addItemInput(Items.COBBLESTONE, 9)
                .addItemOutput(Items.STONE, 1)
                .energyExtract(50)
                .build());

        MachineForge.register(MachineTrade.builder(MapleBankTrade.id("form_quartz_block"))
                .addItemInput(Items.QUARTZ, 4)
                .addItemOutput(Items.QUARTZ_BLOCK, 1)
                .energyExtract(120)
                .build());

        MachineForge.register(MachineTrade.builder(MapleBankTrade.id("stone_to_sandstone"))
                .addItemInput(Items.STONE, 4)
                .addItemOutput(Items.SANDSTONE, 1)
                .build());

        MachineForge.register(MachineTrade.builder(MapleBankTrade.id("refine_iron_block"))
                .addItemInput(Items.IRON_INGOT, 9)
                .addItemInput(Items.REDSTONE, 4)
                .addFluidInput(Fluids.WATER, 500)
                .energyExtract(400)
                .addItemOutput(Items.IRON_BLOCK, 1)
                .build());
    }

    private static void registerBank(CurrencyResource coins) {
        MachineBank.register(MachineTrade.builder(MapleBankTrade.id("mint_coins"))
                .addItemInput(Items.GOLD_INGOT, 1)
                .energyExtract(100)
                .addCurrencyInsert(coins, 40)
                .build());

        MachineBank.register(MachineTrade.builder(MapleBankTrade.id("buy_diamond"))
                .addCurrencyExtract(coins, 150)
                .addItemOutput(Items.DIAMOND, 1)
                .energyExtract(50)
                .build());

        MachineBank.register(MachineTrade.builder(MapleBankTrade.id("buy_emerald"))
                .addCurrencyExtract(coins, 80)
                .addItemOutput(Items.EMERALD, 1)
                .energyExtract(30)
                .build());

        MachineBank.register(MachineTrade.builder(MapleBankTrade.id("sell_diamond"))
                .addItemInput(Items.DIAMOND, 1)
                .addCurrencyInsert(coins, 100)
                .energyExtract(20)
                .build());
    }

    private static void registerPower() {
        MachinePower.register(MachineTrade.builder(MapleBankTrade.id("lava_to_energy"))
                .addFluidInput(Fluids.LAVA, 100)
                .energyInsert(500)
                .build());

        MachinePower.register(MachineTrade.builder(MapleBankTrade.id("coal_to_energy"))
                .addItemInput(Items.COAL, 1)
                .energyInsert(1_000)
                .build());

        MachinePower.register(MachineTrade.builder(MapleBankTrade.id("blaze_to_energy"))
                .addItemInput(Items.BLAZE_ROD, 1)
                .energyInsert(4_000)
                .build());

        MachinePower.register(MachineTrade.builder(MapleBankTrade.id("coal_block_to_energy"))
                .addItemInput(Items.COAL_BLOCK, 1)
                .energyInsert(10_000)
                .build());
    }

    /** 物品柜：只依赖物品与货币，无能量/流体；手动买卖。 */
    private static void registerItemDesk(CurrencyResource coins) {
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("sell_iron"))
                .addItemInput(Items.IRON_INGOT, 1)
                .addCurrencyInsert(coins, 20)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("sell_gold"))
                .addItemInput(Items.GOLD_INGOT, 1)
                .addCurrencyInsert(coins, 30)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("sell_diamond"))
                .addItemInput(Items.DIAMOND, 1)
                .addCurrencyInsert(coins, 100)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_diamond"))
                .addCurrencyExtract(coins, 150)
                .addItemOutput(Items.DIAMOND, 1)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_emerald"))
                .addCurrencyExtract(coins, 80)
                .addItemOutput(Items.EMERALD, 1)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_bread"))
                .addCurrencyExtract(coins, 6)
                .addItemOutput(Items.BREAD, 1)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_ccccc"))
                .addItemInput(Items.IRON_INGOT, 1)
                .addItemOutput(Items.GOLD_INGOT, 1)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_aaa"))
                .addCurrencyExtract(coins, 6)
                .addItemOutput(Items.DIAMOND_BLOCK, 1)
                .stateHook(new CardExistenceVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex()))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_bbb"))
                .addItemInput(Items.EMERALD, 1)
                .addItemOutput(Items.DIAMOND_BLOCK, 1)
                .stateHook(new CardExistenceVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex()))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_dddd"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_eee"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .addDescriptionInvisible(Component.literal("可在地狱中解锁此交易"))
                .addDescriptionVisible(Component.literal("已在地狱中解锁此交易"))
                .stateHook(new DimensionVisibleHook(Level.NETHER.location()))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_fff"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .checkHook(new TimeWindowCheckHook(0, 12000))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_ggg"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .machineTradeIcon(MapleBankTrade.id("textures/item/leaf.png"))
                .addDescriptionVisible(Component.literal("说成功了"))
                .successHook(new LogSuccessHook(Component.literal("成功了")))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("buy_hhh"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .stateHook(new TimeWindowVisibleHook(0, 12000))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("a1"))
                .addItemInput(Items.STONE, 1)
                .addItemOutput(Items.END_STONE, 1)
                .successHook(new FireworkSuccessHook(1))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("a2"))
                .addItemInput(Items.STONE, 1)
                .addItemOutput(Items.END_STONE, 1)
                .successHook(new FireworkSuccessHook(2))
                .build());

        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("a3"))
                .addItemInput(Items.STONE, 1)
                .addItemOutput(Items.END_STONE, 1)
                .successHook(new FireworkSuccessHook(3))
                .build());

        // ==============================================
        // 以下为钩子测试用交易（test_*），可按需删除
        // ==============================================

        // ── 环境类：天气 / 月相 / 高度 / 群系 / 结构 / 区域 ──

        // 下雨天才能出售黏土（WeatherVisibleHook，state: 0=晴 1=雨 2=雷暴）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_weather_rain_buy"))
                .addItemInput(Items.CLAY, 1)
                .addCurrencyInsert(coins, 2)
                .addDescriptionInvisible(Component.literal("下雨天才可出售黏土"))
                .addDescriptionVisible(Component.literal("雨天收购：已解锁"))
                .stateHook(new WeatherVisibleHook((short) 1))
                .build());

        // 雷暴天才允许执行（WeatherCheckHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_weather_thunder_check"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .addDescriptionInvisible(Component.literal("雷暴天才允许执行此交易"))
                .addDescriptionVisible(Component.literal("雷暴天：已解锁"))
                .checkHook(new WeatherCheckHook((short) 2))
                .build());

        // 满月（phase 0）才可见（MoonPhaseVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_moon_full"))
                .addItemInput(Items.STONE, 1)
                .addCurrencyInsert(coins, 3)
                .addDescriptionInvisible(Component.literal("满月之夜才可交易"))
                .addDescriptionVisible(Component.literal("满月之夜：已解锁"))
                .stateHook(new MoonPhaseVisibleHook(0))
                .build());

        // 高空（Y ≥ 120）才可见（HeightVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_height_high"))
                .addItemInput(Items.STONE, 1)
                .addCurrencyInsert(coins, 3)
                .addDescriptionInvisible(Component.literal("在 Y ≥ 120 的高空才可交易"))
                .addDescriptionVisible(Component.literal("高空交易：已解锁"))
                .stateHook(new HeightVisibleHook(120))
                .build());

        // 平原群系才可见（BiomeVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_biome_plains"))
                .addItemInput(Items.WHEAT, 1)
                .addCurrencyInsert(coins, 2)
                .addDescriptionInvisible(Component.literal("在平原群系才可交易"))
                .addDescriptionVisible(Component.literal("平原群系：已解锁"))
                .stateHook(new BiomeVisibleHook(ResourceLocation.parse("minecraft:plains")))
                .build());

        // 海滩群系标签才可见（BiomeTagVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_biome_tag_beach"))
                .addItemInput(Items.SAND, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("在海滩群系才可交易"))
                .addDescriptionVisible(Component.literal("海滩群系：已解锁"))
                .stateHook(new BiomeTagVisibleHook(ResourceLocation.parse("minecraft:is_beach")))
                .build());

        // 村庄结构内才可见（StructureTagVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_structure_village"))
                .addItemInput(Items.EMERALD, 1)
                .addItemOutput(Items.IRON_INGOT, 4)
                .addDescriptionInvisible(Component.literal("在村庄内才可交易"))
                .addDescriptionVisible(Component.literal("村庄内：已解锁"))
                .stateHook(new StructureTagVisibleHook(ResourceLocation.parse("minecraft:village")))
                .build());

        // 出生点区域（±100 方块）内才允许执行（AABBCheckHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_aabb_spawn"))
                .addItemInput(Items.DIRT, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("在出生点 ±100 方块区域内才允许执行"))
                .addDescriptionVisible(Component.literal("出生点区域：已解锁"))
                .checkHook(new AABBCheckHook(new BlockPos(-100, -64, -100), new BlockPos(100, 320, 100)))
                .build());

        // ── 玩家类：身份 / 经验等级 ──

        // 指定玩家名才可见（PlayerStateVisibleHook，按名称）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_player_name"))
                .addItemInput(Items.APPLE, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("仅玩家 Steve 可交易"))
                .addDescriptionVisible(Component.literal("玩家 Steve：已解锁"))
                .stateHook(new PlayerStateVisibleHook("Steve"))
                .build());

        // 指定玩家 UUID 才允许执行（PlayerStateCheckHook，按 UUID；替换为自己的玩家 UUID）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_player_uuid_check"))
                .addItemInput(Items.APPLE, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("仅绑定玩家可执行"))
                .addDescriptionVisible(Component.literal("绑定玩家：已解锁"))
                .checkHook(new PlayerStateCheckHook(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .build());

        // 30 级才可见（PlayerExperienceVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_exp_30"))
                .addItemInput(Items.EMERALD, 1)
                .addItemOutput(Items.DIAMOND, 1)
                .addDescriptionInvisible(Component.literal("30 级才可交易"))
                .addDescriptionVisible(Component.literal("30 级：已解锁"))
                .stateHook(new PlayerExperienceVisibleHook(30))
                .build());

        // ── 银行卡类：货币额度 / Tagged 进度系列 ──

        // 卡上金币 > 100 才可见（CurrencyAmountVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_card_currency"))
                .addCurrencyExtract(coins, 50)
                .addItemOutput(Items.DIAMOND_BLOCK, 1)
                .addDescriptionInvisible(Component.literal("卡上金币需大于 100"))
                .addDescriptionVisible(Component.literal("卡上金币充足：已解锁"))
                .stateHook(new CurrencyAmountVisibleHook(coins.currencyTypeId(), BigInteger.valueOf(100)))
                .build());

        // combat_task_5 完成才可见（TaggedCompletedVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_completed"))
                .addCurrencyExtract(coins, 10)
                .addItemOutput(Items.IRON_SWORD, 1)
                .addDescriptionInvisible(Component.literal("完成标记卡任务 combat_task_5 后解锁"))
                .addDescriptionVisible(Component.literal("combat_task_5 已完成：已解锁"))
                .stateHook(new TaggedCompletedVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_5"))
                .build());

        // combat_task_1 进度 ≥ 5 才可见（TaggedProgressVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_progress"))
                .addCurrencyExtract(coins, 5)
                .addItemOutput(Items.BOW, 1)
                .addDescriptionInvisible(Component.literal("标记卡任务 combat_task_1 进度需 ≥ 5"))
                .addDescriptionVisible(Component.literal("combat_task_1 进度达标：已解锁"))
                .stateHook(new TaggedProgressVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_1", 5))
                .build());

        // combat_task_1 与 combat_task_2 全部完成才可见（TaggedMultiCompletedVisibleHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_multi"))
                .addCurrencyExtract(coins, 20)
                .addItemOutput(Items.DIAMOND_HELMET, 1)
                .addDescriptionInvisible(Component.literal("完成 combat_task_1 与 combat_task_2 后解锁"))
                .addDescriptionVisible(Component.literal("任务组完成：已解锁"))
                .stateHook(new TaggedMultiCompletedVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(),
                        Set.of("combat_task_1", "combat_task_2")))
                .build());

        // tier 1 完成数 ≥ 3 才可见（TaggedTierCompletedCountHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_tier_count"))
                .addCurrencyExtract(coins, 15)
                .addItemOutput(Items.GOLDEN_APPLE, 1)
                .addDescriptionInvisible(Component.literal("标记卡 tier 1 完成数需 ≥ 3"))
                .addDescriptionVisible(Component.literal("tier 1 完成数达标：已解锁"))
                .stateHook(new TaggedTierCompletedCountHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), (short) 1, 3))
                .build());

        // tier 1 完成比例 ≥ 50% 才可见（TaggedTierCompletedRatioHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_tier_ratio"))
                .addCurrencyExtract(coins, 15)
                .addItemOutput(Items.GOLDEN_APPLE, 1)
                .addDescriptionInvisible(Component.literal("标记卡 tier 1 完成比例需 ≥ 50%"))
                .addDescriptionVisible(Component.literal("tier 1 完成比例达标：已解锁"))
                .stateHook(new TaggedTierCompletedRatioHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), (short) 1, 50))
                .build());

        // 总完成数 ≥ 5 才可见（TaggedTotalCompletedCountHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_total_count"))
                .addCurrencyExtract(coins, 10)
                .addItemOutput(Items.ENDER_PEARL, 1)
                .addDescriptionInvisible(Component.literal("标记卡总完成数需 ≥ 5"))
                .addDescriptionVisible(Component.literal("总完成数达标：已解锁"))
                .stateHook(new TaggedTotalCompletedCountHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), 5))
                .build());

        // 总完成比例 ≥ 30% 才可见（TaggedTotalCompletedRatioHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_tagged_total_ratio"))
                .addCurrencyExtract(coins, 10)
                .addItemOutput(Items.ENDER_PEARL, 1)
                .addDescriptionInvisible(Component.literal("标记卡总完成比例需 ≥ 30%"))
                .addDescriptionVisible(Component.literal("总完成比例达标：已解锁"))
                .stateHook(new TaggedTotalCompletedRatioHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), 30))
                .build());

        // ── 组合 / 限购 ──

        // 白天 且 携带中央标记卡 才可见（CompositeVisibilityHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_composite_visible"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .addDescriptionInvisible(Component.literal("需在白天且携带中央标记卡"))
                .addDescriptionVisible(Component.literal("白天 + 标记卡：已解锁"))
                .stateHook(new CompositeVisibilityHook(2,
                        new TimeWindowVisibleHook(0, 12000),
                        new CardExistenceVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex())))
                .build());

        // 白天 且 高度 ≥ 100 才允许执行（CompositeCheckHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_composite_check"))
                .addItemInput(Items.CLAY, 1)
                .addItemOutput(Items.EMERALD, 1)
                .addDescriptionInvisible(Component.literal("需在白天且高度 ≥ 100 才允许执行"))
                .addDescriptionVisible(Component.literal("条件满足：已解锁"))
                .checkHook(new CompositeCheckHook(2,
                        new TimeWindowCheckHook(0, 12000),
                        new HeightCheckHook(100)))
                .build());

        // 每次执行最多 1 次（LimitCountCheckHook 限购）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_limit_1"))
                .addItemInput(Items.CLAY, 64)
                .addCurrencyInsert(coins, 5)
                .addDescriptionInvisible(Component.literal("限购交易：每次最多执行 1 次"))
                .addDescriptionVisible(Component.literal("限购：每次最多 1 组"))
                .checkHook(new LimitCountCheckHook(1))
                .build());

        // ── 跨交易连锁 / 任务链 ──

        // 任务链第 1 步：卖骨头推进 combat_task_1（TaggedIncreaseSuccessHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_quest_1"))
                .addItemInput(Items.BONE, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("任务链第 1 步：出售骨头"))
                .addDescriptionVisible(Component.literal("任务链第 1 步：推进 combat_task_1"))
                .successHook(new TaggedIncreaseSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_1"))
                .build());

        // 任务链第 2 步：combat_task_1 完成后解锁；且仅下雨天可执行（为 sibling check 提供门禁）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_quest_2"))
                .addCurrencyExtract(coins, 10)
                .addItemOutput(Items.IRON_SWORD, 1)
                .addDescriptionInvisible(Component.literal("完成 combat_task_1 后解锁（仅下雨天可执行）"))
                .addDescriptionVisible(Component.literal("任务链第 2 步：已解锁（下雨天可执行）"))
                .stateHook(new TaggedCompletedVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_1"))
                .checkHook(new WeatherCheckHook((short) 1))
                .build());

        // 任务链第 3 步：仅当 quest_2 可见时可见（SiblingTradeVisibleHook 连锁传播）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_quest_3"))
                .addCurrencyExtract(coins, 30)
                .addItemOutput(Items.DIAMOND_SWORD, 1)
                .addDescriptionInvisible(Component.literal("解锁 quest_2 后可见"))
                .addDescriptionVisible(Component.literal("任务链第 3 步：已解锁"))
                .stateHook(new SiblingTradeVisibleHook(MapleBankTrade.id("test_quest_2")))
                .build());

        // 仅当 quest_2 的检查（下雨）通过才允许执行（SiblingTradeCheckHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_quest_check"))
                .addCurrencyExtract(coins, 30)
                .addItemOutput(Items.DIAMOND_SWORD, 1)
                .addDescriptionInvisible(Component.literal("需通过 quest_2 的检查（下雨）才允许执行"))
                .addDescriptionVisible(Component.literal("quest_2 检查通过：已解锁"))
                .checkHook(new SiblingTradeCheckHook(MapleBankTrade.id("test_quest_2")))
                .build());

        // 循环任务演示：成交后 combat_task_2 +1，并把 combat_task_1 重置为 0（TaggedResetSuccessHook）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_reset_loop"))
                .addItemInput(Items.ROTTEN_FLESH, 1)
                .addCurrencyInsert(coins, 1)
                .addDescriptionInvisible(Component.literal("循环任务：成交后推进并重置标记卡进度"))
                .addDescriptionVisible(Component.literal("循环任务：combat_task_2 +1，combat_task_1 重置"))
                .successHook(new CompositeSuccessHook(
                        new TaggedIncreaseSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_2"),
                        new TaggedResetSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "combat_task_1")))
                .build());

        // ── 成功回调：服务端指令奖励 ──

        // 成交后给玩家发钻石（CommandSuccessHook，以 op 权限执行）
        MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("test_command_reward"))
                .addItemInput(Items.EMERALD, 1)
                .addCurrencyInsert(coins, 8)
                .addDescriptionInvisible(Component.literal("成交后发放指令奖励"))
                .addDescriptionVisible(Component.literal("成交后发放奖励：钻石 ×1"))
                .successHook(new CommandSuccessHook("give @p minecraft:diamond 1"))
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
        AutoSellOresLava.register(MachineTrade.builder(MapleBankTrade.id("sell_lava"))
                .addFluidInput(Fluids.LAVA, 1000)
                .addCurrencyInsert(coins, 25)
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
        storage.register(MachineTrade.builder(MapleBankTrade.id(path))
                .addItemInput(item, 1)
                .addCurrencyInsert(coins, price)
                .autoTrade(true)
                .build());
    }

    private MachineTradeRegistration() {}
}

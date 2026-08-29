package com.maple.maple_banktrade.common;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;

import java.util.List;
import java.util.Optional;

/**
 * 配方数据生成辅助常量。
 */
public class MBTRecipe {

    // ==============================================
    // 解锁条件
    // ==============================================

    /** 无条件解锁的背包变更 Trigger 实例。 */
    private static final InventoryChangeTrigger.TriggerInstance UNCONDITIONAL_TRIGGER_INSTANCE = new InventoryChangeTrigger.TriggerInstance(Optional.empty(),
            InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of());

    /** 无条件解锁的配方 Criterion。 */
    public static final Criterion<InventoryChangeTrigger.TriggerInstance> UNCONDITIONAL_CRITERION = CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(UNCONDITIONAL_TRIGGER_INSTANCE);

    // ==============================================
    // 初始化
    // ==============================================

    /** 预留初始化入口。 */
    public static void init() {}
}

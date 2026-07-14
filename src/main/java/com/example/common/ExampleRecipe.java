package com.example.common;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

import static com.example.ExampleMod.REGISTRY;

public class ExampleRecipe {

    public static void init() {}

    /**
     * 无条件解锁的TriggerInstance
     */
    private static final InventoryChangeTrigger.TriggerInstance UNCONDITIONAL_TRIGGER_INSTANCE = new InventoryChangeTrigger.TriggerInstance(Optional.empty(),
            InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of());

    /**
     * 封装无条件解锁的Criterion
     */
    public static final Criterion<InventoryChangeTrigger.TriggerInstance> UNCONDITIONAL_CRITERION = CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(UNCONDITIONAL_TRIGGER_INSTANCE);

    static {

        REGISTRY.addRecipeData(prov -> prov.shaped(RecipeCategory.COMBAT, ExampleRegistration.NXAMPLE_BLOCK)
                .pattern("QQQ")
                .pattern("QIQ")
                .pattern("QQQ")
                .define('Q', Items.QUARTZ_PILLAR)
                .define('I', ExampleRegistration.NXAMPLE_ITEM)
                .unlockedBy("unlocked", UNCONDITIONAL_CRITERION)
                .save(prov, "example_recipe"));
    }
}

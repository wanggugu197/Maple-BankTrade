package com.maple.maple_banktrade.common.quests;

import net.minecraft.world.item.Items;

import com.maple.maple_banktrade.api.quests.condition.HasItemCondition;
import com.maple.maple_banktrade.api.quests.condition.HasPotionEffectCondition;
import com.maple.maple_banktrade.api.quests.condition.HealthCondition;
import com.maple.maple_banktrade.api.quests.condition.IsRainingCondition;
import com.maple.maple_banktrade.api.quests.condition.LevelCondition;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.impl.BaseTaskDefinition;
import com.maple.maple_banktrade.api.quests.reward.ItemReward;
import com.maple.maple_banktrade.api.quests.tasktype.SubmitItemTaskType;

import java.util.Arrays;
import java.util.List;

/**
 * 具体任务蓝图注册中心（数据驱动配置）。
 *
 * <p>
 * v3.6 重构：条件、奖励、任务类型改为直接存储对象实例，
 * 不再通过 {@code Identifier} + {@code CompoundTag} 间接存储。
 *
 * <p>
 * 当前包含 26 个示例任务，覆盖全部 6 种行为模式。
 */
public final class QuestBlueprints {

    private QuestBlueprints() {}

    public static List<ITaskDefinition> getAllBlueprints() {
        return Arrays.asList(
                // ==================== 主线任务链 ====================
                buildMainRoot(),
                buildMainForest(),
                buildMainCave(),
                buildMainCastle(),

                // ==================== 支线任务 ====================
                buildSideVillage(),
                buildSideVillageCollect(),
                buildSideHermit(),

                // ==================== 临时/循环任务 ====================
                buildTempFishing(),
                buildTempHunting(),
                buildDailyMining(),

                // ==================== 高级行为模式任务 ====================
                buildMultiKill(),
                buildRandomPool(),
                buildMultiRandomPool(),
                buildHiddenChain(),
                buildRandomActivate(),

                // ==================== 新功能示例 ====================
                buildOneCompletedDeps(),
                buildSubmitItemTask(),
                buildItemRewardTask(),

                // ==================== 占位任务 ====================
                buildSideAfterKills(),
                buildFinalReward(),
                buildSecretQuest2(),
                buildSubTaskA(),
                buildSubTaskB(),
                buildSubTaskC(),
                buildSubTaskX(),
                buildSubTaskY());
    }

    // ==============================================
    // 1. 主线任务链
    // ==============================================

    private static ITaskDefinition buildMainRoot() {
        return new BaseTaskDefinition.Builder()
                .id("main_root")
                .type(TaskType.MAIN)
                .isGroup(true)
                .childrenIds(Arrays.asList("main_forest", "main_cave"))
                .forceParentVisible(true)
                .build();
    }

    /** 第一个主线任务：需玩家等级 ≥ 1 才能解锁。 */
    private static ITaskDefinition buildMainForest() {
        return new BaseTaskDefinition.Builder()
                .id("main_forest")
                .type(TaskType.MAIN)
                .parentId("main_root")
                .unlockCondition(new LevelCondition(1, true))
                .build();
    }

    /** 第二个主线任务：兄弟链排在 main_forest 之后，完成后跨链跳转到 main_castle。 */
    private static ITaskDefinition buildMainCave() {
        return new BaseTaskDefinition.Builder()
                .id("main_cave")
                .type(TaskType.MAIN)
                .parentId("main_root")
                .prevSiblingId("main_forest")
                .nextTaskInChain("main_castle")
                .build();
    }

    /** 第三个主线任务：需持有绿宝石才能解锁。 */
    private static ITaskDefinition buildMainCastle() {
        return new BaseTaskDefinition.Builder()
                .id("main_castle")
                .type(TaskType.MAIN)
                .parentId("main_root")
                .prevSiblingId("main_cave")
                .unlockCondition(new HasItemCondition(Items.EMERALD))
                .build();
    }

    // ==============================================
    // 2. 支线任务
    // ==============================================

    private static ITaskDefinition buildSideVillage() {
        return new BaseTaskDefinition.Builder()
                .id("side_village")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .dependentNodes(Arrays.asList("main_forest"))
                .nextTaskInChain("side_village_collect")
                .build();
    }

    private static ITaskDefinition buildSideVillageCollect() {
        return new BaseTaskDefinition.Builder()
                .id("side_village_collect")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .prevSiblingId("side_village")
                .build();
    }

    /** 支线任务：依赖 main_castle 完成，有再生药水效果时才可见。 */
    private static ITaskDefinition buildSideHermit() {
        return new BaseTaskDefinition.Builder()
                .id("side_hermit")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .dependentNodes(Arrays.asList("main_castle"))
                .visibilityCondition(HasPotionEffectCondition.of("regeneration"))
                .build();
    }

    // ==============================================
    // 3. 临时任务 & 循环任务
    // ==============================================

    /** 临时任务：下雨时触发，可循环最多 5 次。 */
    private static ITaskDefinition buildTempFishing() {
        return new BaseTaskDefinition.Builder()
                .id("temp_fishing")
                .type(TaskType.TEMPORARY)
                .repeatable(true)
                .maxRepeatTimes(5)
                .visibilityCondition(new IsRainingCondition())
                .build();
    }

    /** 临时任务：残血时触发，无限循环。 */
    private static ITaskDefinition buildTempHunting() {
        return new BaseTaskDefinition.Builder()
                .id("temp_hunting")
                .type(TaskType.TEMPORARY)
                .repeatable(true)
                .maxRepeatTimes(-1)
                .visibilityCondition(new HealthCondition(10.0f, true))
                .build();
    }

    /** 临时任务：每日 1 次。 */
    private static ITaskDefinition buildDailyMining() {
        return new BaseTaskDefinition.Builder()
                .id("daily_mining")
                .type(TaskType.TEMPORARY)
                .repeatable(true)
                .maxRepeatTimes(1)
                .build();
    }

    // ==============================================
    // 4. 高级行为模式任务
    // ==============================================

    private static ITaskDefinition buildMultiKill() {
        return new BaseTaskDefinition.Builder()
                .id("multi_kill")
                .type(TaskType.SIDE)
                .behavior(TaskBehavior.MULTI_COMPLETION)
                .requiredCompletions(5)
                .parentId("main_root")
                .nextTaskInChain("side_after_kills")
                .build();
    }

    private static ITaskDefinition buildRandomPool() {
        return new BaseTaskDefinition.Builder()
                .id("random_pool_task")
                .type(TaskType.SIDE)
                .behavior(TaskBehavior.RANDOM_POOL)
                .poolIds(Arrays.asList("sub_task_a", "sub_task_b", "sub_task_c"))
                .parentId("main_root")
                .build();
    }

    private static ITaskDefinition buildMultiRandomPool() {
        return new BaseTaskDefinition.Builder()
                .id("multi_random_pool")
                .type(TaskType.SIDE)
                .behavior(TaskBehavior.MULTI_RANDOM_POOL)
                .requiredCompletions(3)
                .poolIds(Arrays.asList("sub_task_x", "sub_task_y"))
                .parentId("main_root")
                .nextTaskInChain("final_reward")
                .build();
    }

    private static ITaskDefinition buildHiddenChain() {
        return new BaseTaskDefinition.Builder()
                .id("hidden_quest")
                .type(TaskType.TEMPORARY)
                .behavior(TaskBehavior.HIDDEN_CHAIN)
                .nextChainTriggerChance(0.3)
                .nextTaskInChain("secret_quest_2")
                .build();
    }

    private static ITaskDefinition buildRandomActivate() {
        return new BaseTaskDefinition.Builder()
                .id("random_activate")
                .type(TaskType.TEMPORARY)
                .behavior(TaskBehavior.RANDOM_ACTIVATE)
                .autoResetToHidden(true)
                .build();
    }

    // ==============================================
    // 5. 新功能示例
    // ==============================================

    /** 依赖模式 ONE_COMPLETED：side_village 或 side_hermit 任一完成即可解锁。 */
    private static ITaskDefinition buildOneCompletedDeps() {
        return new BaseTaskDefinition.Builder()
                .id("side_any_dep")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .dependentNodes(Arrays.asList("side_village", "side_hermit"))
                .dependencyRequirement(DependencyRequirement.ONE_COMPLETED)
                .build();
    }

    /** 提交物品任务：需提交 3 个钻石才能完成。 */
    private static ITaskDefinition buildSubmitItemTask() {
        return new BaseTaskDefinition.Builder()
                .id("submit_diamonds")
                .type(TaskType.TEMPORARY)
                .taskTypes(List.of(SubmitItemTaskType.of(Items.DIAMOND, 3)))
                .repeatable(true)
                .maxRepeatTimes(-1)
                .build();
    }

    /** 带物品奖励的任务：完成后获得 1 个绿宝石和 5 个经验瓶。 */
    private static ITaskDefinition buildItemRewardTask() {
        return new BaseTaskDefinition.Builder()
                .id("reward_demo")
                .type(TaskType.TEMPORARY)
                .rewards(Arrays.asList(
                        ItemReward.of(Items.EMERALD, 1),
                        ItemReward.of(Items.EXPERIENCE_BOTTLE, 5)))
                .repeatable(true)
                .maxRepeatTimes(3)
                .build();
    }

    // ==============================================
    // 6. 占位任务
    // ==============================================

    private static ITaskDefinition buildSideAfterKills() {
        return new BaseTaskDefinition.Builder()
                .id("side_after_kills")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .build();
    }

    private static ITaskDefinition buildFinalReward() {
        return new BaseTaskDefinition.Builder()
                .id("final_reward")
                .type(TaskType.SIDE)
                .parentId("main_root")
                .build();
    }

    private static ITaskDefinition buildSecretQuest2() {
        return new BaseTaskDefinition.Builder()
                .id("secret_quest_2")
                .type(TaskType.TEMPORARY)
                .build();
    }

    private static ITaskDefinition buildSubTaskA() {
        return new BaseTaskDefinition.Builder()
                .id("sub_task_a")
                .type(TaskType.SIDE)
                .parentId("random_pool_task")
                .build();
    }

    private static ITaskDefinition buildSubTaskB() {
        return new BaseTaskDefinition.Builder()
                .id("sub_task_b")
                .type(TaskType.SIDE)
                .parentId("random_pool_task")
                .build();
    }

    private static ITaskDefinition buildSubTaskC() {
        return new BaseTaskDefinition.Builder()
                .id("sub_task_c")
                .type(TaskType.SIDE)
                .parentId("random_pool_task")
                .build();
    }

    private static ITaskDefinition buildSubTaskX() {
        return new BaseTaskDefinition.Builder()
                .id("sub_task_x")
                .type(TaskType.SIDE)
                .parentId("multi_random_pool")
                .build();
    }

    private static ITaskDefinition buildSubTaskY() {
        return new BaseTaskDefinition.Builder()
                .id("sub_task_y")
                .type(TaskType.SIDE)
                .parentId("multi_random_pool")
                .build();
    }
}

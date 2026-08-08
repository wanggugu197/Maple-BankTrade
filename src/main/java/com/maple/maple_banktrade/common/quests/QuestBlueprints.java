package com.maple.maple_banktrade.common.quests;

import net.minecraft.nbt.CompoundTag;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.enums.DependencyRequirement;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.impl.BaseTaskDefinition;
import com.maple.maple_banktrade.api.quests.reward.RewardDef;

import java.util.Arrays;
import java.util.List;

/**
 * 具体任务蓝图注册中心（数据驱动配置）。
 *
 * <p>
 * 集中定义游戏中的所有任务模板。实际项目中可改为从 JSON/DataPack 动态加载。
 *
 * <p>
 * 当前包含 18 个示例任务，覆盖全部 6 种行为模式 + 新功能：
 * <ul>
 * <li>主线任务链 (MAIN)：4 个 —— 含分组节点、兄弟链、跨链跳转、条件解锁</li>
 * <li>支线任务 (SIDE)：3 个 —— 含多依赖、可见条件、链式推进</li>
 * <li>临时/循环任务 (TEMPORARY)：3 个 —— 含有限循环、无限循环、每日限制</li>
 * <li>高级行为模式：5 个 —— MULTI_COMPLETION、RANDOM_POOL、MULTI_RANDOM_POOL、HIDDEN_CHAIN、RANDOM_ACTIVATE</li>
 * <li>新功能示例：3 个 —— ONE_COMPLETED 依赖、submit_item 任务类型、item 奖励</li>
 * </ul>
 */
public final class QuestBlueprints {

    private QuestBlueprints() {
        // 工具类，禁止实例化
    }

    /**
     * 获取所有预定义的任务蓝图（共 18 个）。
     */
    public static List<ITaskDefinition> getAllBlueprints() {
        return Arrays.asList(
                // ==================== 主线任务链 ====================
                buildMainRoot(),
                buildMainForest(),
                buildMainCave(),
                buildMainCastle(),

                // ==================== 支线任务（依赖主线解锁） ====================
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
                buildItemRewardTask());
    }

    // ==============================================
    // 1. 主线任务链
    // ==============================================

    /** 主线根节点：纯分组节点，不参与进度计算，强制可见。 */
    private static ITaskDefinition buildMainRoot() {
        return new BaseTaskDefinition.Builder()
                .id("main_root")
                .type(TaskType.MAIN)
                .isGroup(true)
                .parentId(null)
                .prevSiblingId(null)
                .childrenIds(Arrays.asList("main_forest", "main_cave"))
                .forceParentVisible(true)
                .build();
    }

    /** 第一个主线任务 (SIMPLE)：开局可见，需玩家等级 ≥ 1。 */
    private static ITaskDefinition buildMainForest() {
        CompoundTag params = new CompoundTag();
        params.putInt("level", 1);
        return new BaseTaskDefinition.Builder()
                .id("main_forest")
                .type(TaskType.MAIN)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId(null)
                .dependentNodes(Arrays.asList())
                .nextTaskInChain(null)
                .repeatable(false)
                .maxRepeatTimes(-1)
                .forceParentVisible(false)
                .unlockConditionId(MapleBankTrade.id("level_ge"))
                .unlockConditionParams(params)
                .build();
    }

    /** 第二个主线任务 (SIMPLE)：兄弟链排在 main_forest 之后，完成后跨链跳转到 main_castle。 */
    private static ITaskDefinition buildMainCave() {
        return new BaseTaskDefinition.Builder()
                .id("main_cave")
                .type(TaskType.MAIN)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId("main_forest")
                .dependentNodes(Arrays.asList())
                .nextTaskInChain("main_castle")
                .repeatable(false)
                .build();
    }

    /** 第三个主线任务 (SIMPLE)：兄弟链排在 main_cave 之后，需持有绿宝石才能解锁。 */
    private static ITaskDefinition buildMainCastle() {
        CompoundTag params = new CompoundTag();
        params.putString("item", "minecraft:emerald");
        return new BaseTaskDefinition.Builder()
                .id("main_castle")
                .type(TaskType.MAIN)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId("main_cave")
                .dependentNodes(Arrays.asList())
                .nextTaskInChain(null)
                .repeatable(false)
                .unlockConditionId(MapleBankTrade.id("has_item"))
                .unlockConditionParams(params)
                .build();
    }

    // ==============================================
    // 2. 支线任务（依赖主线节点）
    // ==============================================

    /** 支线任务 (SIMPLE)：依赖 main_forest 完成，完成后链式触发 side_village_collect。 */
    private static ITaskDefinition buildSideVillage() {
        return new BaseTaskDefinition.Builder()
                .id("side_village")
                .type(TaskType.SIDE)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId(null)
                .dependentNodes(Arrays.asList("main_forest"))
                .nextTaskInChain("side_village_collect")
                .repeatable(false)
                .build();
    }

    /** 支线后继 (SIMPLE)：兄弟链排在 side_village 之后。 */
    private static ITaskDefinition buildSideVillageCollect() {
        return new BaseTaskDefinition.Builder()
                .id("side_village_collect")
                .type(TaskType.SIDE)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId("side_village")
                .dependentNodes(Arrays.asList())
                .nextTaskInChain(null)
                .repeatable(false)
                .build();
    }

    /** 支线任务 (SIMPLE)：依赖 main_castle 完成，有再生药水效果时才可见。 */
    private static ITaskDefinition buildSideHermit() {
        CompoundTag params = new CompoundTag();
        params.putString("effect", "regeneration");
        return new BaseTaskDefinition.Builder()
                .id("side_hermit")
                .type(TaskType.SIDE)
                .isGroup(false)
                .parentId("main_root")
                .prevSiblingId(null)
                .dependentNodes(Arrays.asList("main_castle"))
                .nextTaskInChain(null)
                .repeatable(false)
                .visibilityConditionId(MapleBankTrade.id("has_effect"))
                .visibilityConditionParams(params)
                .build();
    }

    // ==============================================
    // 3. 临时任务 & 循环任务
    // ==============================================

    /** 临时任务 (SIMPLE)：下雨时触发，可循环最多 5 次。 */
    private static ITaskDefinition buildTempFishing() {
        return new BaseTaskDefinition.Builder()
                .id("temp_fishing")
                .type(TaskType.TEMPORARY)
                .isGroup(false)
                .parentId(null)
                .repeatable(true)
                .maxRepeatTimes(5)
                .visibilityConditionId(MapleBankTrade.id("is_raining"))
                .build();
    }

    /** 临时任务 (SIMPLE)：残血时触发，无限循环。 */
    private static ITaskDefinition buildTempHunting() {
        CompoundTag params = new CompoundTag();
        params.putFloat("health", 10.0f);
        return new BaseTaskDefinition.Builder()
                .id("temp_hunting")
                .type(TaskType.TEMPORARY)
                .isGroup(false)
                .repeatable(true)
                .maxRepeatTimes(-1)
                .visibilityConditionId(MapleBankTrade.id("low_health"))
                .visibilityConditionParams(params)
                .build();
    }

    /** 临时任务 (SIMPLE)：每日 1 次，由外部时间戳判断重置。 */
    private static ITaskDefinition buildDailyMining() {
        return new BaseTaskDefinition.Builder()
                .id("daily_mining")
                .type(TaskType.TEMPORARY)
                .isGroup(false)
                .repeatable(true)
                .maxRepeatTimes(1)
                .build();
    }

    // ==============================================
    // 4. 高级行为模式任务
    // ==============================================

    /** 多次击杀任务 (MULTI_COMPLETION)：需完成 5 次，完成后解锁 side_after_kills。 */
    private static ITaskDefinition buildMultiKill() {
        return new BaseTaskDefinition.Builder()
                .id("multi_kill")
                .type(TaskType.SIDE)
                .behavior(TaskBehavior.MULTI_COMPLETION)
                .requiredCompletions(5)
                .parentId("main_root")
                .prevSiblingId(null)
                .nextTaskInChain("side_after_kills")
                .build();
    }

    /** 单次随机池任务 (RANDOM_POOL)：激活时从 3 个子任务中随机选一个执行。 */
    private static ITaskDefinition buildRandomPool() {
        return new BaseTaskDefinition.Builder()
                .id("random_pool_task")
                .type(TaskType.SIDE)
                .behavior(TaskBehavior.RANDOM_POOL)
                .poolIds(Arrays.asList("sub_task_a", "sub_task_b", "sub_task_c"))
                .parentId("main_root")
                .build();
    }

    /** 多次随机池任务 (MULTI_RANDOM_POOL)：需完成 3 次，每次从 2 个子任务中随机选，完成后解锁 final_reward。 */
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

    /** 隐藏链任务 (HIDDEN_CHAIN)：30% 概率触发后继 secret_quest_2。 */
    private static ITaskDefinition buildHiddenChain() {
        return new BaseTaskDefinition.Builder()
                .id("hidden_quest")
                .type(TaskType.TEMPORARY)
                .behavior(TaskBehavior.HIDDEN_CHAIN)
                .nextChainTriggerChance(0.3)
                .nextTaskInChain("secret_quest_2")
                .build();
    }

    /** 随机激活任务 (RANDOM_ACTIVATE)：完成后自动变为 HIDDEN，可被再次随机触发，不记录永久完成。 */
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

    /** 示例：依赖模式 ONE_COMPLETED —— side_village 或 side_hermit 任一完成即可解锁。 */
    private static ITaskDefinition buildOneCompletedDeps() {
        return new BaseTaskDefinition.Builder()
                .id("side_any_dep")
                .type(TaskType.SIDE)
                .isGroup(false)
                .parentId("main_root")
                .dependentNodes(Arrays.asList("side_village", "side_hermit"))
                .dependencyRequirement(DependencyRequirement.ONE_COMPLETED)
                .repeatable(false)
                .build();
    }

    /** 示例：提交物品任务 —— 需提交 3 个钻石才能完成。 */
    private static ITaskDefinition buildSubmitItemTask() {
        CompoundTag typeParams = new CompoundTag();
        typeParams.putString("item", "minecraft:diamond");
        typeParams.putInt("count", 3);
        return new BaseTaskDefinition.Builder()
                .id("submit_diamonds")
                .type(TaskType.TEMPORARY)
                .isGroup(false)
                .parentId(null)
                .taskTypeId(MapleBankTrade.id("submit_item"))
                .taskTypeParams(typeParams)
                .repeatable(true)
                .maxRepeatTimes(-1)
                .build();
    }

    /** 示例：带物品奖励的任务 —— 完成后获得 1 个绿宝石和 5 个经验瓶。 */
    private static ITaskDefinition buildItemRewardTask() {
        CompoundTag rewards1 = new CompoundTag();
        rewards1.putString("item", "minecraft:emerald");
        rewards1.putInt("count", 1);
        CompoundTag rewards2 = new CompoundTag();
        rewards2.putString("item", "minecraft:experience_bottle");
        rewards2.putInt("count", 5);
        return new BaseTaskDefinition.Builder()
                .id("reward_demo")
                .type(TaskType.TEMPORARY)
                .isGroup(false)
                .parentId(null)
                .rewards(Arrays.asList(
                        new RewardDef(MapleBankTrade.id("item"), rewards1),
                        new RewardDef(MapleBankTrade.id("item"), rewards2)))
                .repeatable(true)
                .maxRepeatTimes(3)
                .build();
    }
}

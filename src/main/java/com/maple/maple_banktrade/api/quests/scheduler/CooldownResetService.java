package com.maple.maple_banktrade.api.quests.scheduler;

import com.maple.maple_banktrade.api.quests.calculator.VisibilityCalculator;
import com.maple.maple_banktrade.api.quests.condition.IScriptEvaluator;
import com.maple.maple_banktrade.api.quests.condition.ResolutionContext;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskBehavior;
import com.maple.maple_banktrade.api.quests.enums.TaskStatus;
import com.maple.maple_banktrade.api.quests.enums.TaskType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 冷却/重置服务：处理循环任务的冷却和每日重置逻辑。
 *
 * <p>
 * 基于完成记录的时间戳判断任务是否可再次触发：
 * <ul>
 * <li>循环任务（repeatable=true）：检查上次完成时间是否超过冷却期</li>
 * <li>每日任务（maxRepeatTimes=1）：检查是否跨天</li>
 * <li>随机激活任务（RANDOM_ACTIVATE）：始终可重置</li>
 * </ul>
 */
public final class CooldownResetService {

    private CooldownResetService() {
        // 工具类，禁止实例化
    }

    /**
     * 默认冷却时间（游戏 tick）：20 分钟 = 24000 ticks。
     */
    public static final long DEFAULT_COOLDOWN_TICKS = 24000L;

    /**
     * 批量检查并重置所有符合条件的任务。
     *
     * @param repository      任务仓储
     * @param currentTick     当前游戏 tick
     * @param scriptEvaluator 脚本引擎
     * @return 被重置的任务 ID 列表
     */
    public static List<String> processCooldownResets(IQuestRepository repository, long currentTick,
                                                     IScriptEvaluator scriptEvaluator) {
        Map<String, ITaskState> states = repository.getAllStates();
        ResolutionContext context = new ResolutionContext(repository, scriptEvaluator, states);

        List<String> resetIds = new ArrayList<>();
        for (Map.Entry<String, ITaskState> entry : states.entrySet()) {
            String taskId = entry.getKey();
            ITaskState state = entry.getValue();

            if (shouldReset(taskId, state, context, currentTick)) {
                resetTask(taskId, state, repository, currentTick);
                resetIds.add(taskId);
            }
        }
        return resetIds;
    }

    /**
     * 判断单个任务是否应该重置。
     */
    public static boolean shouldReset(String taskId, ITaskState state, ResolutionContext context, long currentTick) {
        ITaskDefinition def = context.getDefinition(taskId);
        if (def == null) return false;

        // 随机激活任务：如果状态是 HIDDEN，检查是否可以触发
        if (def.getBehavior() == TaskBehavior.RANDOM_ACTIVATE) {
            return state.getStatus() == TaskStatus.HIDDEN;
        }

        // 已完成的任务：检查循环是否可重置
        if (state.getStatus() == TaskStatus.COMPLETED) {
            if (!def.isRepeatable()) return false;

            List<ICompletionRecord> records = context.getCompletionRecords(taskId);
            if (records.isEmpty()) return false;

            // 检查最大重复次数
            int maxRepeats = def.getMaxRepeatTimes();
            if (maxRepeats >= 0 && records.size() >= maxRepeats) {
                return false;
            }

            // 检查冷却时间
            ICompletionRecord lastRecord = records.get(records.size() - 1);
            long elapsed = currentTick - lastRecord.getCompletedGameTime();
            return elapsed >= getCooldownTicks(def);
        }

        return false;
    }

    /**
     * 重置单个任务的状态。
     */
    public static void resetTask(String taskId, ITaskState state, IQuestRepository repository, long currentTick) {
        ITaskDefinition def = repository.getDefinition(taskId).orElse(null);
        if (def == null) return;

        TaskBehavior behavior = def.getBehavior();

        if (behavior == TaskBehavior.RANDOM_ACTIVATE) {
            // 随机激活任务：HIDDEN → VISIBLE_LOCKED
            state.setStatus(TaskStatus.VISIBLE_LOCKED);
        } else {
            // 循环任务：COMPLETED → VISIBLE_LOCKED
            state.setStatus(TaskStatus.VISIBLE_LOCKED);
            state.setCurrentProgress(0);
        }
        state.setActiveTimestamp(0);
        repository.saveState(state);
    }

    /**
     * 获取任务的冷却时间（tick）。
     */
    private static long getCooldownTicks(ITaskDefinition def) {
        // 每日任务（maxRepeatTimes=1）：使用 24000 ticks（一个 Minecraft 日）
        if (def.getMaxRepeatTimes() == 1) {
            return DEFAULT_COOLDOWN_TICKS;
        }
        return DEFAULT_COOLDOWN_TICKS;
    }

    // ==============================================
    // 临时任务随机触发
    // ==============================================

    /**
     * 从临时任务池中随机选取一个任务触发。
     *
     * @param repository         任务仓储
     * @param scriptEvaluator    脚本引擎
     * @param maxActiveTempTasks 最大同时激活的临时任务数
     * @param random             随机数生成器
     * @return 被触发的任务 ID，或 null 表示无任务触发
     */
    public static String tryRandomTempTask(IQuestRepository repository, IScriptEvaluator scriptEvaluator,
                                           int maxActiveTempTasks, Random random) {
        List<ITaskDefinition> tempDefs = repository.getDefinitionsByType(TaskType.TEMPORARY);
        if (tempDefs.isEmpty()) return null;

        Map<String, ITaskState> states = repository.getAllStates();

        // 检查当前激活的临时任务数
        long activeCount = states.values().stream()
                .filter(s -> s.getStatus() == TaskStatus.ACTIVE || s.getStatus() == TaskStatus.VISIBLE_LOCKED)
                .filter(s -> {
                    ITaskDefinition def = repository.getDefinition(s.getTaskId()).orElse(null);
                    return def != null && def.getType() == TaskType.TEMPORARY;
                })
                .count();

        if (activeCount >= maxActiveTempTasks) return null;

        // 筛选可触发的候选
        List<ITaskDefinition> candidates = tempDefs.stream()
                .filter(def -> {
                    ITaskState state = states.get(def.getId());
                    if (state == null) return true;
                    return state.getStatus() == TaskStatus.HIDDEN;
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;

        // 随机选取
        ITaskDefinition selected = candidates.get(random.nextInt(candidates.size()));

        // 触发
        ITaskState selectedState = repository.getOrCreateState(selected.getId());
        TaskStatus newStatus = VisibilityCalculator.resolveStatus(selected.getId(),
                new ResolutionContext(repository, scriptEvaluator, states));
        selectedState.setStatus(newStatus);
        repository.saveState(selectedState);

        return selected.getId();
    }
}

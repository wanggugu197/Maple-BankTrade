package com.maple.maple_banktrade.api.quests.tasktype;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

/**
 * 任务完成类型接口 —— 定义任务完成时需要满足的条件和副作用。
 *
 * <p>
 * v3.6 重构：移除 {@code getId()}，任务类型实例直接存储在任务定义中，
 * 不再需要通过注册表查找。
 *
 * <p>
 * 与 {@link com.maple.maple_banktrade.api.quests.enums.TaskType}（分类：MAIN/SIDE/TEMPORARY）不同，
 * 本接口定义的是"完成方式"（确认完成、提交物品、击杀实体等）。
 */
public interface ITaskType {

    /**
     * 检查是否满足完成条件。
     * 在 UI 点击完成时调用，返回 false 则拒绝完成。
     *
     * @param def     任务定义
     * @param repo    任务仓储（用于查询状态和进度）
     * @param context 运行时上下文（通常为 {@code ServerPlayer}）
     * @return true 表示可以完成
     */
    boolean canComplete(ITaskDefinition def, IQuestRepository repo, Object context);

    /**
     * 完成任务时的副作用（如扣除物品、消耗资源）。
     * 仅在 {@link #canComplete} 返回 true 后调用。
     *
     * @param def     任务定义
     * @param repo    任务仓储
     * @param context 运行时上下文
     */
    void onComplete(ITaskDefinition def, IQuestRepository repo, Object context);
}

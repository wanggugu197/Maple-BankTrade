package com.maple.maple_banktrade.api.quests.impl;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskInstance;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 方便创建任务实例的工具类。
 */
public final class BaseTaskInstanceFactory {

    private BaseTaskInstanceFactory() {
        // 工具类，禁止实例化
    }

    /**
     * 创建单个任务实例。
     *
     * @throws IllegalArgumentException 如果任务 ID 不存在
     */
    public static ITaskInstance create(String taskId, IQuestRepository repository) {
        ITaskDefinition def = repository.getDefinition(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown task: " + taskId));
        ITaskState state = repository.getOrCreateState(taskId);
        return new BaseTaskInstance(def, state, repository);
    }

    /**
     * 批量创建所有已注册任务的实例。
     */
    public static List<ITaskInstance> createAll(IQuestRepository repository) {
        return repository.getAllDefinitions().stream()
                .map(def -> {
                    ITaskState state = repository.getOrCreateState(def.getId());
                    return new BaseTaskInstance(def, state, repository);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按类型批量创建任务实例。
     */
    public static List<ITaskInstance> createByType(IQuestRepository repository, TaskType type) {
        return repository.getDefinitionsByType(type).stream()
                .map(def -> {
                    ITaskState state = repository.getOrCreateState(def.getId());
                    return new BaseTaskInstance(def, state, repository);
                })
                .collect(Collectors.toList());
    }
}

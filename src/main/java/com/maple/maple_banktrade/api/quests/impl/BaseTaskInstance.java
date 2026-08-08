package com.maple.maple_banktrade.api.quests.impl;

import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskInstance;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 任务实例：持有定义、状态，并通过仓储获取历史记录。
 */
@Getter
@RequiredArgsConstructor
public class BaseTaskInstance implements ITaskInstance {

    private final ITaskDefinition definition;
    private final ITaskState state;
    private final IQuestRepository repository;

    @Override
    public List<ICompletionRecord> getCompletionRecords() {
        return repository.getCompletionRecords(definition.getId());
    }

    @Override
    public String toString() {
        return "BaseTaskInstance{" +
                "id='" + definition.getId() + '\'' +
                ", status=" + state.getStatus() +
                ", behavior=" + definition.getBehavior() +
                '}';
    }
}

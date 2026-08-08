package com.maple.maple_banktrade.api.quests.repository;

import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.impl.BaseTaskState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存仓储实现（可替换为 NBT 持久化）。
 *
 * <p>
 * 内部使用 {@link ConcurrentHashMap} 保证线程安全，
 * 并预构建 {@code parentId → children} 索引以加速 {@link #getChildren} 查询。
 */
public class InMemoryQuestRepository implements IQuestRepository {

    private final Map<String, ITaskDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, ITaskState> states = new ConcurrentHashMap<>();
    private final Map<String, List<ICompletionRecord>> history = new ConcurrentHashMap<>();

    /** 预构建的 parentId → children 索引，避免每次 O(n) 遍历。 */
    private final Map<String, List<ITaskDefinition>> childrenIndex = new ConcurrentHashMap<>();

    // ==============================================
    // 定义加载与索引
    // ==============================================

    /**
     * 批量加载任务定义并重建索引。
     */
    public void loadDefinitions(Collection<ITaskDefinition> defs) {
        defs.forEach(d -> definitions.put(d.getId(), d));
        buildIndex();
    }

    /**
     * 重建 children 索引。
     */
    public void buildIndex() {
        childrenIndex.clear();
        for (ITaskDefinition def : definitions.values()) {
            String parentId = def.getParentId();
            if (parentId != null && !parentId.isEmpty()) {
                childrenIndex.computeIfAbsent(parentId, k -> new ArrayList<>()).add(def);
            }
        }
    }

    // ==============================================
    // 定义查询
    // ==============================================

    @Override
    public Optional<ITaskDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    @Override
    public List<ITaskDefinition> getRoots() {
        return definitions.values().stream()
                .filter(ITaskDefinition::isRoot)
                .collect(Collectors.toList());
    }

    @Override
    public List<ITaskDefinition> getChildren(String parentId) {
        return childrenIndex.getOrDefault(parentId, Collections.emptyList());
    }

    @Override
    public List<ITaskDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }

    @Override
    public List<ITaskDefinition> getDefinitionsByType(TaskType type) {
        return definitions.values().stream()
                .filter(d -> d.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasDefinition(String id) {
        return definitions.containsKey(id);
    }

    @Override
    public int getDefinitionCount() {
        return definitions.size();
    }

    // ==============================================
    // 状态管理
    // ==============================================

    @Override
    public ITaskState getOrCreateState(String taskId) {
        return states.computeIfAbsent(taskId, BaseTaskState::new);
    }

    @Override
    public Map<String, ITaskState> getAllStates() {
        return new HashMap<>(states);
    }

    @Override
    public void saveState(ITaskState state) {
        states.put(state.getTaskId(), state);
    }

    // ==============================================
    // 历史记录
    // ==============================================

    @Override
    public List<ICompletionRecord> getCompletionRecords(String taskId) {
        return history.getOrDefault(taskId, Collections.emptyList());
    }

    @Override
    public void addCompletionRecord(ICompletionRecord record) {
        history.computeIfAbsent(record.getTaskId(), k -> new ArrayList<>()).add(record);
    }

    @Override
    public void pruneRecords(String taskId, int maxKeep) {
        List<ICompletionRecord> list = history.get(taskId);
        if (list != null && list.size() > maxKeep) {
            List<ICompletionRecord> toKeep = list.stream()
                    .sorted(Comparator.comparingLong(ICompletionRecord::getCompletedGameTime).reversed())
                    .limit(maxKeep)
                    .collect(Collectors.toList());
            Collections.reverse(toKeep);
            history.put(taskId, toKeep);
        }
    }
}

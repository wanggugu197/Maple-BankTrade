package com.maple.maple_banktrade.api.quests.repository;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry;
import com.maple.maple_banktrade.api.quests.core.ICompletionRecord;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.core.ITaskState;
import com.maple.maple_banktrade.api.quests.enums.TaskType;
import com.maple.maple_banktrade.api.quests.impl.BaseCompletionRecord;
import com.maple.maple_banktrade.api.quests.impl.BaseTaskState;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 单个玩家的任务持久化数据：仅存储可变内容（状态 + 完成历史）。
 *
 * <p>
 * <b>定义与状态分离：</b>
 * <ul>
 * <li><b>不可变（静态）</b>：任务定义（蓝图）由 {@link QuestDefinitionRegistry} 全局持有，所有玩家共享</li>
 * <li><b>可变（动态）</b>：任务状态和完成历史，由本类按 UUID 独立存储</li>
 * </ul>
 *
 * <p>
 * v3.8 重构：{@link BaseTaskState} 实现 {@link IPersistedSerializable}，
 * 状态序列化使用 {@link PersistedParser#createCodec} 自动生成 Codec，
 * 无需手动编写 {@code RecordCodecBuilder}。
 *
 * <p>
 * 序列化结构（仅存储状态和历史）：
 * 
 * <pre>{@code
 * {
 *   "states": {
 *     "main_forest": { "taskId": "main_forest", "status": "ACTIVE", "currentProgress": 3, "activeTimestamp": 12345 },
 *     "main_cave": { "taskId": "main_cave", "status": "HIDDEN", "currentProgress": 0, "activeTimestamp": 0 }
 *   },
 *   "history": {
 *     "main_forest": [
 *       { "taskId": "main_forest", "index": 1, "gameTime": 1000, "realTime": 1234567890, "rewardClaimed": false }
 *     ]
 *   }
 * }
 * }</pre>
 */
public class PlayerQuestData implements IQuestRepository {

    // ==============================================
    // 字段（仅可变内容）
    // ==============================================

    /** 任务状态：taskId → state。 */
    private final Map<String, BaseTaskState> states = new ConcurrentHashMap<>();
    /** 完成历史：taskId → records。 */
    private final Map<String, List<BaseCompletionRecord>> history = new ConcurrentHashMap<>();

    /** 脏标记回调，由外部设置（如 SavedData::setDirty）。 */
    private Runnable dirtyCallback = () -> {};

    // ==============================================
    // Codec
    // ==============================================

    /** 任务状态序列化 Codec（v3.8：使用 PersistedParser 自动生成）。 */
    private static final Codec<BaseTaskState> STATE_CODEC = PersistedParser.createCodec(BaseTaskState::new);

    /** 完成记录序列化 Codec（v3.7：新增 rewardClaimed 字段持久化）。 */
    private static final Codec<BaseCompletionRecord> RECORD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("taskId").forGetter(BaseCompletionRecord::getTaskId),
            Codec.INT.fieldOf("index").forGetter(BaseCompletionRecord::getCompletionIndex),
            Codec.LONG.fieldOf("gameTime").forGetter(BaseCompletionRecord::getCompletedGameTime),
            Codec.LONG.optionalFieldOf("realTime", 0L)
                    .forGetter(BaseCompletionRecord::getRealTimeStamp),
            Codec.BOOL.optionalFieldOf("rewardClaimed", false)
                    .forGetter(BaseCompletionRecord::isRewardClaimed))
            .apply(instance, (taskId, index, gameTime, realTime, rewardClaimed) -> new BaseCompletionRecord.Builder()
                    .taskId(taskId)
                    .completionIndex(index)
                    .completedGameTime(gameTime)
                    .realTimeStamp(realTime)
                    .rewardClaimed(rewardClaimed)
                    .build()));

    /** 严格状态 Map Codec（用于写入）。 */
    private static final Codec<Map<String, BaseTaskState>> STRICT_STATES_CODEC = Codec.unboundedMap(Codec.STRING, STATE_CODEC);

    /** 严格历史 Map Codec（用于写入）。 */
    private static final Codec<Map<String, List<BaseCompletionRecord>>> STRICT_HISTORY_CODEC = Codec.unboundedMap(Codec.STRING, Codec.list(RECORD_CODEC));

    /** 宽松状态 Map Codec（读取时跳过无法解析的条目）。 */
    private static final Codec<Map<String, BaseTaskState>> STATES_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Map<String, BaseTaskState> input, DynamicOps<T> ops, T prefix) {
            return STRICT_STATES_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<Map<String, BaseTaskState>, T>> decode(DynamicOps<T> ops, T input) {
            Map<String, BaseTaskState> result = new HashMap<>();
            ops.getMapValues(input)
                    .resultOrPartial(msg -> {})
                    .ifPresent(entries -> entries.forEach(entry -> {
                        String key = Codec.STRING.parse(ops, entry.getFirst())
                                .resultOrPartial(msg -> {}).orElse(null);
                        BaseTaskState state = STATE_CODEC.parse(ops, entry.getSecond())
                                .resultOrPartial(msg -> {}).orElse(null);
                        if (key != null && state != null) {
                            result.put(key, state);
                        }
                    }));
            return DataResult.success(Pair.of(result, ops.empty()));
        }
    };

    /** 宽松历史 Map Codec（读取时跳过无法解析的条目）。 */
    private static final Codec<Map<String, List<BaseCompletionRecord>>> HISTORY_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Map<String, List<BaseCompletionRecord>> input, DynamicOps<T> ops, T prefix) {
            return STRICT_HISTORY_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<Map<String, List<BaseCompletionRecord>>, T>> decode(DynamicOps<T> ops, T input) {
            Map<String, List<BaseCompletionRecord>> result = new HashMap<>();
            ops.getMapValues(input)
                    .resultOrPartial(msg -> {}).ifPresent(entries -> entries.forEach(entry -> {
                        String key = Codec.STRING.parse(ops, entry.getFirst())
                                .resultOrPartial(msg -> {}).orElse(null);
                        List<BaseCompletionRecord> records = Codec.list(RECORD_CODEC)
                                .parse(ops, entry.getSecond())
                                .resultOrPartial(msg -> {}).orElse(Collections.emptyList());
                        if (key != null && !records.isEmpty()) {
                            result.put(key, records);
                        }
                    }));
            return DataResult.success(Pair.of(result, ops.empty()));
        }
    };

    /** 玩家任务数据完整 Codec。 */
    public static final Codec<PlayerQuestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            STATES_CODEC.optionalFieldOf("states", Map.of())
                    .forGetter(data -> Collections.unmodifiableMap(data.states)),
            HISTORY_CODEC.optionalFieldOf("history", Map.of())
                    .forGetter(data -> Collections.unmodifiableMap(data.history)))
            .apply(instance, (states, history) -> {
                PlayerQuestData data = new PlayerQuestData();
                states.forEach((k, v) -> data.states.put(k, v));
                history.forEach((k, v) -> data.history.put(k, new ArrayList<>(v)));
                return data;
            }));

    // ==============================================
    // 构造
    // ==============================================

    /** 创建空白的玩家任务数据。 */
    public PlayerQuestData() {}

    /**
     * 设置脏标记回调（通常由外部调用，如 SavedData::setDirty）。
     */
    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback != null ? callback : () -> {};
    }

    private void markDirty() {
        dirtyCallback.run();
    }

    // ==============================================
    // IQuestRepository —— 定义查询（委托给全局注册表）
    // ==============================================

    @Override
    public Optional<ITaskDefinition> getDefinition(String id) {
        return Optional.ofNullable(QuestDefinitionRegistry.getDefinition(id));
    }

    @Override
    public List<ITaskDefinition> getRoots() {
        return QuestDefinitionRegistry.getRoots();
    }

    @Override
    public List<ITaskDefinition> getChildren(String parentId) {
        return QuestDefinitionRegistry.getChildren(parentId);
    }

    @Override
    public List<ITaskDefinition> getAllDefinitions() {
        return QuestDefinitionRegistry.getAllDefinitions();
    }

    @Override
    public List<ITaskDefinition> getDefinitionsByType(TaskType type) {
        return QuestDefinitionRegistry.getDefinitionsByType(type);
    }

    @Override
    public boolean hasDefinition(String id) {
        return QuestDefinitionRegistry.hasDefinition(id);
    }

    @Override
    public int getDefinitionCount() {
        return QuestDefinitionRegistry.size();
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
        if (state instanceof BaseTaskState bs) {
            states.put(state.getTaskId(), bs);
            markDirty();
        } else {
            BaseTaskState bs = new BaseTaskState(state.getTaskId());
            bs.setStatus(state.getStatus());
            bs.setCurrentProgress(state.getCurrentProgress());
            bs.setActiveTimestamp(state.getActiveTimestamp());
            states.put(state.getTaskId(), bs);
            markDirty();
        }
    }

    // ==============================================
    // 历史记录
    // ==============================================

    @Override
    public List<ICompletionRecord> getCompletionRecords(String taskId) {
        List<BaseCompletionRecord> records = history.get(taskId);
        return records == null ? Collections.emptyList() : new ArrayList<>(records);
    }

    @Override
    public void addCompletionRecord(ICompletionRecord record) {
        BaseCompletionRecord br;
        if (record instanceof BaseCompletionRecord b) {
            br = b;
        } else {
            br = new BaseCompletionRecord.Builder()
                    .taskId(record.getTaskId())
                    .completionIndex(record.getCompletionIndex())
                    .completedGameTime(record.getCompletedGameTime())
                    .realTimeStamp(record.getRealTimeStamp())
                    .build();
        }
        history.computeIfAbsent(record.getTaskId(), k -> new ArrayList<>()).add(br);
        markDirty();
    }

    @Override
    public void pruneRecords(String taskId, int maxKeep) {
        List<BaseCompletionRecord> list = history.get(taskId);
        if (list != null && list.size() > maxKeep) {
            List<BaseCompletionRecord> toKeep = list.stream()
                    .sorted(Comparator.comparingLong(BaseCompletionRecord::getCompletedGameTime).reversed())
                    .limit(maxKeep)
                    .collect(Collectors.toList());
            Collections.reverse(toKeep);
            history.put(taskId, toKeep);
            markDirty();
        }
    }

    /**
     * 替换指定位置的完成记录（v3.7 新增）。
     * 用于 markRewardClaimed 时直接替换旧记录。
     *
     * @param taskId 任务 ID
     * @param index  0-based 索引
     * @param record 新记录
     * @return true 表示替换成功
     */
    public boolean replaceCompletionRecord(String taskId, int index, BaseCompletionRecord record) {
        List<BaseCompletionRecord> list = history.get(taskId);
        if (list == null || index < 0 || index >= list.size()) return false;
        list.set(index, record);
        markDirty();
        return true;
    }

    // ==============================================
    // 辅助方法
    // ==============================================

    /**
     * 获取所有有状态记录的任务 ID 集合。
     */
    public Set<String> getAllTaskIds() {
        Set<String> ids = new HashSet<>(states.keySet());
        ids.addAll(history.keySet());
        return Collections.unmodifiableSet(ids);
    }

    /**
     * 获取有状态的任务数量。
     */
    public int getStateCount() {
        return states.size();
    }

    /**
     * 获取有历史记录的任务数量。
     */
    public int getHistoryCount() {
        return history.size();
    }

    @Override
    public String toString() {
        return "PlayerQuestData{" +
                "states=" + states.size() +
                ", history=" + history.size() +
                '}';
    }
}

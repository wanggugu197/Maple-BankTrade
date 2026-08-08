package com.maple.maple_banktrade.api.quests.storage;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.repository.PlayerQuestData;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

import javax.annotation.Nullable;

/**
 * 任务系统世界持久化存储 —— 按 UUID 管理玩家任务数据（仅可变内容）。
 *
 * <p>
 * 参考 {@code BankCardsWorldData} 的 Codec 序列化模式设计：
 * <ul>
 * <li>使用 {@link Codec} 进行 NBT 序列化，挂载到服务器 {@link SavedData} 体系</li>
 * <li>宽松读取（跳过无法解析的 UUID 条目，保证存档兼容性）</li>
 * <li>内部存储 {@code Map<UUID, PlayerQuestData>}，每个 UUID 独立存储一份任务数据</li>
 * <li>任务定义由 {@link com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry} 全局静态持有</li>
 * </ul>
 *
 * <p>
 * 序列化结构（仅状态和历史，不含定义）：
 * 
 * <pre>{@code
 * {
 *   "entries": {
 *     "550e8400-e29b-41d4-a716-446655440000": {
 *       "states": { ... },
 *       "history": { ... }
 *     },
 *     "660e8400-e29b-41d4-a716-446655440001": { ... }
 *   }
 * }
 * }</pre>
 *
 * <p>
 * 使用方式：
 * 
 * <pre>{@code
 * 
 * // 获取存储实例
 * QuestSavedData storage = QuestDataHelper.getOrCreateStorage(server);
 * // 获取某 UUID 的任务数据
 * PlayerQuestData data = storage.getOrCreate(uuid);
 * }</pre>
 */
public class QuestSavedData extends SavedData {

    // ==============================================
    // 字段
    // ==============================================

    /** UUID → 玩家任务数据映射。 */
    private final Map<UUID, PlayerQuestData> entries;

    // ==============================================
    // Codec
    // ==============================================

    /** UUID 作为 Map key 的字符串 Codec。 */
    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UUID string: " + value);
        }
    }, UUID::toString);

    /** 严格 entries Map Codec（用于写入）。 */
    private static final Codec<Map<UUID, PlayerQuestData>> STRICT_ENTRIES_CODEC = Codec.unboundedMap(UUID_STRING_CODEC, PlayerQuestData.CODEC);

    /** 宽松 entries Map Codec（读取时跳过无法解析的条目）。 */
    private static final Codec<Map<UUID, PlayerQuestData>> ENTRIES_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Map<UUID, PlayerQuestData> input, DynamicOps<T> ops, T prefix) {
            return STRICT_ENTRIES_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<Map<UUID, PlayerQuestData>, T>> decode(DynamicOps<T> ops, T input) {
            Map<UUID, PlayerQuestData> result = new HashMap<>();
            ops.getMapValues(input)
                    .resultOrPartial(message -> MapleBankTrade.LOGGER.error(
                            "Failed to read quest saved data entries, skipping all: {}", message))
                    .ifPresent(entries -> entries.forEach(entry -> {
                        UUID uuid = UUID_STRING_CODEC.parse(ops, entry.getFirst())
                                .resultOrPartial(message -> MapleBankTrade.LOGGER.error(
                                        "Skipping quest data for UUID that failed to deserialize: {}", message))
                                .orElse(null);
                        if (uuid == null) return;

                        PlayerQuestData data = PlayerQuestData.CODEC.parse(ops, entry.getSecond())
                                .resultOrPartial(message -> MapleBankTrade.LOGGER.error(
                                        "Skipping quest data for UUID {} that failed to deserialize: {}",
                                        uuid, message))
                                .orElse(null);
                        if (data != null) {
                            result.put(uuid, data);
                        }
                    }));
            return DataResult.success(Pair.of(result, ops.empty()));
        }
    };

    /** 世界数据序列化编解码器。 */
    public static final Codec<QuestSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ENTRIES_CODEC
                    .optionalFieldOf("entries", Map.of())
                    .forGetter(QuestSavedData::entriesSnapshot))
            .apply(instance, QuestSavedData::new));

    /** 全局 SavedData 类型；文件为 data/maple_banktrade/quest_data.dat。 */
    public static final SavedDataType<QuestSavedData> TYPE = new SavedDataType<>(
            MapleBankTrade.id("quest_data"),
            QuestSavedData::new,
            CODEC);

    // ==============================================
    // 构造
    // ==============================================

    /** 创建空白任务存储（新世界初始化）。 */
    public QuestSavedData() {
        this.entries = new HashMap<>();
    }

    /** 反序列化构造：恢复所有 UUID 的任务数据。 */
    private QuestSavedData(Map<UUID, PlayerQuestData> entries) {
        this.entries = new HashMap<>(entries);
    }

    // ==============================================
    // 序列化辅助
    // ==============================================

    /** 获取 entries 的不可变快照（用于序列化）。 */
    private Map<UUID, PlayerQuestData> entriesSnapshot() {
        return Collections.unmodifiableMap(entries);
    }

    // ==============================================
    // 查询
    // ==============================================

    /**
     * 获取指定 UUID 的任务数据。
     *
     * @param uuid 玩家 UUID
     * @return 任务数据，不存在返回 null
     */
    @Nullable
    public PlayerQuestData get(UUID uuid) {
        if (uuid == null) return null;
        return entries.get(uuid);
    }

    /**
     * 获取或创建指定 UUID 的任务数据。
     * 任务定义由 {@link com.maple.maple_banktrade.api.quests.QuestDefinitionRegistry} 全局提供，无需注入。
     *
     * @param uuid 玩家 UUID
     * @return 任务数据，不会返回 null
     */
    public PlayerQuestData getOrCreate(UUID uuid) {
        if (uuid == null) return null;
        return entries.computeIfAbsent(uuid, key -> {
            PlayerQuestData data = new PlayerQuestData();
            data.setDirtyCallback(this::setDirty);
            MapleBankTrade.LOGGER.debug("[QuestSavedData] Created quest data for UUID {}", uuid);
            setDirty();
            return data;
        });
    }

    /**
     * 检查指定 UUID 是否存在任务数据。
     */
    public boolean contains(UUID uuid) {
        return uuid != null && entries.containsKey(uuid);
    }

    /**
     * 获取当前存储的 UUID 数量。
     */
    public int size() {
        return entries.size();
    }

    /**
     * 获取所有已存储 UUID 的只读集合。
     */
    public Set<UUID> getAllUuids() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    // ==============================================
    // 修改
    // ==============================================

    /**
     * 移除指定 UUID 的任务数据（玩家登出时可选择清理）。
     *
     * @param uuid 玩家 UUID
     * @return 被移除的数据，如果不存在返回 null
     */
    @Nullable
    public PlayerQuestData remove(UUID uuid) {
        if (uuid == null) return null;
        PlayerQuestData removed = entries.remove(uuid);
        if (removed != null) {
            setDirty();
            MapleBankTrade.LOGGER.debug("[QuestSavedData] Removed quest data for UUID {}", uuid);
        }
        return removed;
    }

    /**
     * 标记指定 UUID 的数据为脏（触发保存）。
     * 由 {@link PlayerQuestData#setDirtyCallback(Runnable)} 回调间接调用。
     */
    public void markDirty(UUID uuid) {
        if (uuid != null && entries.containsKey(uuid)) {
            setDirty();
        }
    }
}

package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.data.InfoList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 信息存储卡
 * <p>
 * 每张卡通过其 {@link #getNameIndex()} 绑定一个预定义的信息列表（{@link InfoList}），
 * 卡内存储每个信息条目的当前完成次数。
 * </p>
 */
public class TaggedBankCard extends BankCard {

    public static final ResourceLocation CARD_TYPE_ID = MapleBankTrade.id("tagged");

    // ==============================================
    // Codec（由 PersistedParser 生成）
    // ==============================================

    public static final Codec<TaggedBankCard> CODEC = PersistedParser.createCodec(TaggedBankCard::new);
    public static final MapCodec<TaggedBankCard> MAP_CODEC = PersistedParser.createMapCodec(TaggedBankCard::new);

    // ==============================================
    // 字段
    // ==============================================

    /** 进度存储：条目 ID -> 当前已完成次数 */
    @Persisted
    @Getter
    @Setter
    private Map<String, Integer> progress;

    // ==============================================
    // 构造
    // ==============================================

    /** 无参构造（反序列化） */
    public TaggedBankCard() {
        this.progress = new LinkedHashMap<>();
    }

    /**
     * 业务构造，使用卡的身份信息创建空进度卡。
     * 绑定的信息列表由 {@code identity.nameIndex()} 决定。
     */
    public TaggedBankCard(BankCardIdentity identity) {
        this(identity, Collections.emptyMap());
    }

    /**
     * 完整构造，允许预设进度（供内部或测试使用）。
     */
    protected TaggedBankCard(BankCardIdentity identity, Map<String, Integer> initialProgress) {
        super(identity, CARD_TYPE_ID);
        this.progress = new LinkedHashMap<>();
        if (initialProgress != null) {
            initialProgress.forEach((id, count) -> {
                if (count > 0) {
                    this.progress.put(id, count);
                }
            });
        }
    }

    // ==============================================
    // 信息列表绑定
    // ==============================================

    /**
     * 获取此卡绑定的信息列表。
     * 列表 ID 即为卡的 {@link #getNameIndex()}。
     *
     * @return 对应的 InfoList，若未注册则返回 null
     */
    public InfoList getInfoList() {
        return InfoList.requireByNameIndex(getNameIndex());
    }

    // ==============================================
    // 进度查询
    // ==============================================

    /**
     * 获取某个信息条目的当前完成次数。
     *
     * @param entryId 条目 ID
     * @return 当前次数（若条目不存在于任何列表中，返回 0）
     */
    public int getProgress(String entryId) {
        return progress.getOrDefault(entryId, 0);
    }

    /**
     * 获取所有进度的只读视图。
     */
    public Map<String, Integer> getAllProgress() {
        return Collections.unmodifiableMap(progress);
    }

    /**
     * 检查某个条目是否已完成（当前次数 >= 所需次数）。
     *
     * @param entryId 条目 ID
     * @return 是否完成；若条目未定义则返回 false
     */
    public boolean isComplete(String entryId) {
        InfoList list = getInfoList();
        if (list == null) return false;
        InfoList.InfoEntry infoEntry = list.getEntry(entryId);
        if (infoEntry == null) return false;
        return getProgress(entryId) >= infoEntry.requiredCount();
    }

    /**
     * 获取某个条目的完成进度比例（0.0 ~ 1.0）。
     *
     * @param entryId 条目 ID
     * @return 比例，若条目未定义则返回 0.0
     */
    public float getProgressRatio(String entryId) {
        InfoList list = getInfoList();
        if (list == null) return 0.0f;
        InfoList.InfoEntry infoEntry = list.getEntry(entryId);
        if (infoEntry == null || infoEntry.requiredCount() <= 0) return 0.0f;
        return Math.min(1.0f, (float) getProgress(entryId) / infoEntry.requiredCount());
    }

    // ==============================================
    // 任务完成统计
    // ==============================================

    public int getTotalEntries() {
        InfoList list = getInfoList();
        return list == null ? 0 : list.entries().size();
    }

    public int getCompletedEntries() {
        InfoList list = getInfoList();
        if (list == null) return 0;
        int completed = 0;
        for (String id : list.entries().keySet()) {
            if (isComplete(id)) completed++;
        }
        return completed;
    }

    public int getCompletionRatio() {
        int total = getTotalEntries();
        if (total == 0) return 0;
        return 100 * getCompletedEntries() / total;
    }

    // ---- 按 tier 统计 ----
    public int getTotalEntriesByTier(short tier) {
        InfoList list = getInfoList();
        if (list == null) return 0;
        int completed = 0;
        for (InfoList.InfoEntry entry : list.entries().values()) {
            if (entry.tire() == tier) completed++;
        }
        return completed;
    }

    public int getCompletedEntriesByTier(short tier) {
        InfoList list = getInfoList();
        if (list == null) return 0;
        int completed = 0;
        for (InfoList.InfoEntry entry : list.entries().values()) {
            if (entry.tire() == tier && isComplete(entry.id())) {
                completed++;
            }
        }
        return completed;
    }

    public int getCompletionRatioByTier(short tier) {
        int total = getTotalEntriesByTier(tier);
        if (total == 0) return 0;
        return 100 * getCompletedEntriesByTier(tier) / total;
    }
    // ==============================================
    // 等级列表获取
    // ==============================================

    /**
     * 获取该卡绑定的信息列表中所有不重复的 tier 值，按从高到低排序。
     * 若列表未注册，返回空列表。
     */
    public List<Short> getSortedTiers() {
        InfoList list = getInfoList();
        if (list == null) return Collections.emptyList();
        Set<Short> tierSet = new HashSet<>();
        for (InfoList.InfoEntry entry : list.entries().values()) {
            tierSet.add(entry.tire());  // 注意字段名为 tire（与 InfoEntry 定义一致）
        }
        List<Short> sorted = new ArrayList<>(tierSet);
        sorted.sort(Comparator.reverseOrder()); // 降序
        return Collections.unmodifiableList(sorted);
    }

    // ==============================================
    // 进度修改
    // ==============================================

    /**
     * 增加某个条目的进度。
     *
     * @param entryId 条目 ID
     * @param amount  增加的数量（必须 > 0）
     * @param limit   是否限制上限（true：不超过 requiredCount；false：不限制，可无限累加）
     * @return 增加后是否达到完成状态（即增加后 >= requiredCount）。如果条目无效或 amount<=0，返回 false。
     */
    public boolean addProgress(String entryId, int amount, boolean limit) {
        if (amount <= 0) {
            MapleBankTrade.LOGGER.warn("Tried to add non-positive amount {} to entry {}, ignoring.", amount, entryId);
            return false;
        }
        InfoList list = getInfoList();
        if (list == null) {
            MapleBankTrade.LOGGER.warn("InfoList not found for nameIndex: {}, cannot add progress to entry {}", getNameIndex(), entryId);
            return false;
        }
        InfoList.InfoEntry infoEntry = list.getEntry(entryId);
        if (infoEntry == null) {
            MapleBankTrade.LOGGER.warn("Unknown entry id: {} in InfoList {}, cannot add progress.", entryId, getNameIndex());
            return false;
        }

        int required = infoEntry.requiredCount();
        int current = progress.getOrDefault(entryId, 0);
        int newCount;
        if (limit) {
            newCount = Math.min(current + amount, required);
        } else {
            newCount = current + amount;
        }
        if (newCount > current) {
            progress.put(entryId, newCount);
        }
        return newCount >= required;
    }

    /**
     * 增加某个条目的进度（默认限制上限，保持原有行为）。
     *
     * @param entryId 条目 ID
     * @param amount  增加的数量（必须 > 0）
     * @return 增加后是否达到完成状态（即增加后 >= requiredCount）。如果条目无效或 amount<=0，返回 false。
     */
    public boolean addProgress(String entryId, int amount) {
        return addProgress(entryId, amount, true);
    }

    /**
     * 重置某个条目的进度为 0。
     *
     * @param entryId 条目 ID
     */
    public void resetProgress(String entryId) {
        progress.remove(entryId);
    }
}

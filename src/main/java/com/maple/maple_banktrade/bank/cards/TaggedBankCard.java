package com.maple.maple_banktrade.bank.cards;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

/**
 * 标记银行卡，保存一组 Identifier 标记。
 */
public class TaggedBankCard extends BankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 标记银行卡类型 ID。 */
    public static final Identifier CARD_TYPE_ID = MapleBankTrade.id("tagged");

    // ==============================================
    // Codec
    // ==============================================

    /** 标签集合使用列表形式写入存档。 */
    private static final Codec<Set<Identifier>> TAGS_CODEC = Identifier.CODEC.listOf()
            .xmap(HashSet::new, List::copyOf);

    /** 标记银行卡序列化编解码器。 */
    public static final Codec<TaggedBankCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("card_uuid").forGetter(TaggedBankCard::getCardUuid),
            Identifier.CODEC.fieldOf("bank_type").forGetter(TaggedBankCard::getBankTypeId),
            Identifier.CODEC.fieldOf("card_type").forGetter(TaggedBankCard::getCardTypeId),
            Identifier.CODEC.fieldOf("name_index").forGetter(TaggedBankCard::getNameIndex),
            TAGS_CODEC.fieldOf("tags").forGetter(TaggedBankCard::getTags))
            .apply(instance, (cardUuid, bankTypeId, ignoredCardTypeId, nameIndex, tags) -> new TaggedBankCard(cardUuid, bankTypeId, nameIndex, tags)));

    // ==============================================
    // 字段
    // ==============================================

    private final Set<Identifier> tags;

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行和名称索引的标记卡。 */
    public TaggedBankCard(UUID cardUuid, BankType bankType, Identifier nameIndex) {
        this(cardUuid, bankType.id(), nameIndex, Set.of());
    }

    /** 从存档字段恢复标记卡。 */
    protected TaggedBankCard(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex, Set<Identifier> tags) {
        super(cardUuid, bankTypeId, CARD_TYPE_ID, nameIndex);
        this.tags = new HashSet<>(tags);
    }

    // ==============================================
    // 查询方法
    // ==============================================

    /** 获取标签的只读视图。 */
    public Set<Identifier> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    // ==============================================
    // 修改方法
    // ==============================================

    /** 添加标签。 */
    public boolean addTag(Identifier tag) {
        return tag != null && tags.add(tag);
    }

    /** 移除标签。 */
    public boolean removeTag(Identifier tag) {
        return tag != null && tags.remove(tag);
    }
}

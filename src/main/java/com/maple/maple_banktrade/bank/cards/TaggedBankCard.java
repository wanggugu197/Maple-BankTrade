package com.maple.maple_banktrade.bank.cards;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public static final MapCodec<TaggedBankCard> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BankCard.IDENTITY_FIELDS_CODEC.forGetter(BankCardIdentity::of),
            TAGS_CODEC.fieldOf("tags").forGetter(TaggedBankCard::getTags))
            .apply(instance, TaggedBankCard::new));

    // ==============================================
    // 字段
    // ==============================================

    @Getter
    private final Set<Identifier> tags;

    // ==============================================
    // 构造方法
    // ==============================================

    /** 创建指定银行和名称索引的标记卡。 */
    public TaggedBankCard(BankCardIdentity identity) {
        this(identity, Set.of());
    }

    /** 从存档字段恢复标记卡。 */
    protected TaggedBankCard(BankCardIdentity identity, Set<Identifier> tags) {
        super(identity, CARD_TYPE_ID);
        this.tags = new HashSet<>(tags);
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

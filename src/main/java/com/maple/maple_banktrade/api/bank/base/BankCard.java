package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * 银行卡基类，仅保存银行卡身份信息。
 */
public abstract class BankCard {

    // ==============================================
    // 常量
    // ==============================================

    /** 客户端快照中的权限字段键。 */
    private static final String CLIENT_PERMISSION_KEY = "client_permission";

    // ==============================================
    // Codec
    // ==============================================

    /** 银行卡基础身份：card_type 之外的三个存档字段。 */
    public record BankCardIdentity(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex) {

        /** 从银行卡实例提取身份字段。 */
        public static BankCardIdentity of(BankCard card) {
            return new BankCardIdentity(card.getCardUuid(), card.getBankTypeId(), card.getNameIndex());
        }

        /** 使用银行类型与名称索引创建身份字段。 */
        public static BankCardIdentity of(UUID cardUuid, BankType bankType, Identifier nameIndex) {
            return new BankCardIdentity(cardUuid, bankType.id(), nameIndex);
        }
    }

    /** bank_type 字段 Codec：解码时要求银行类型已注册，未注册则拒绝加载该卡。 */
    private static final Codec<Identifier> BANK_TYPE_CODEC = Identifier.CODEC.flatXmap(
            bankTypeId -> {
                BankType bankType = BankType.requireById(bankTypeId);
                return bankType == null ? DataResult.error(() -> "Unknown bank type: " + bankTypeId) : DataResult.success(bankType.id());
            },
            DataResult::success);

    /** 基础身份字段组 Codec；card_type 由 CODEC 的 dispatch 单独读写。 */
    public static final MapCodec<BankCardIdentity> IDENTITY_FIELDS_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("card_uuid").forGetter(BankCardIdentity::cardUuid),
            BANK_TYPE_CODEC.fieldOf("bank_type").forGetter(BankCardIdentity::bankTypeId),
            Identifier.CODEC.fieldOf("name_index").forGetter(BankCardIdentity::nameIndex))
            .apply(instance, BankCardIdentity::new));

    /** 编码按实例 card_type；解码按存档 card_type 分发（DFU 标准 dispatch）。 */
    public static final Codec<BankCard> CODEC = Identifier.CODEC.partialDispatch(
            "card_type",
            card -> DataResult.success(card.getCardTypeId()),
            BankCardType::getCodecResult);

    // ==============================================
    // 字段
    // ==============================================

    /** 银行卡唯一 ID。 */
    @Getter
    private final UUID cardUuid;
    /** 所属银行类型 ID。 */
    @Getter
    private final Identifier bankTypeId;
    /** 银行卡类型 ID，用于选择子类 Codec。 */
    @Getter
    private final Identifier cardTypeId;
    /** 名称索引，用于查询显示名称。 */
    @Getter
    private final Identifier nameIndex;
    /** UI 客户端只读权限快照，不参与世界存档。 */
    @Getter
    private BankCardPermission clientPermission = BankCardPermission.UNUSABLE;

    // ==============================================
    // 构造
    // ==============================================

    /** 使用身份字段组与卡类型 ID 创建基础卡信息。 */
    protected BankCard(BankCardIdentity identity, Identifier cardTypeId) {
        this(identity.cardUuid(), identity.bankTypeId(), cardTypeId, identity.nameIndex());
    }

    /** 使用银行类型、卡类型与名称索引创建基础卡信息。 */
    protected BankCard(UUID cardUuid, Identifier bankTypeId, Identifier cardTypeId, Identifier nameIndex) {
        BankType bankType = BankType.requireById(bankTypeId);
        this.cardUuid = Objects.requireNonNull(cardUuid, "cardUuid");
        this.bankTypeId = bankType == null ? Objects.requireNonNull(bankTypeId, "bankTypeId") : bankType.id();
        this.cardTypeId = Objects.requireNonNull(cardTypeId, "cardTypeId");
        this.nameIndex = Objects.requireNonNull(nameIndex, "nameIndex");
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 获取银行卡所属银行类型。 */
    @Nullable
    public BankType getBankType() {
        return BankType.requireById(bankTypeId);
    }

    /** 客户端快照权限是否可用；服务端权威判断请用 {@link BankCardsWorldData#canUse}。 */
    public boolean canClientUse() {
        return clientPermission != null && clientPermission.canUse();
    }

    /** 编码为客户端快照 Tag，并写入当前权限。 */
    public CompoundTag toClientTag(BankCardPermission permission) {
        Tag encoded = CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        CompoundTag tag = encoded instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
        tag.putString(CLIENT_PERMISSION_KEY, (permission == null ? BankCardPermission.UNUSABLE : permission).getSerializedName());
        return tag;
    }

    /** 从客户端快照 Tag 解码银行卡与权限。 */
    public static BankCard fromClientTag(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag) || compoundTag.isEmpty()) return null;
        BankCard card = CODEC.parse(NbtOps.INSTANCE, compoundTag).result().orElse(null);
        if (card != null) {
            card.clientPermission = BankCardPermission.bySerializedName(compoundTag.getStringOr(CLIENT_PERMISSION_KEY, BankCardPermission.UNUSABLE.getSerializedName()));
        }
        return card;
    }
}

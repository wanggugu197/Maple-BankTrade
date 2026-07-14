package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
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

    /** 编码按实例 card_type；解码按存档 card_type 分发。 */
    public static final Codec<BankCard> CODEC = Codec.of(BankCard::encode, BankCardType::decodeCard);

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

    // ==============================================
    // 工具
    // ==============================================

    /** 按银行卡实际 card_type 选择对应 Codec 写入存档。 */
    @SuppressWarnings("unchecked")
    private static <T> DataResult<T> encode(BankCard input, DynamicOps<T> ops, T prefix) {
        Codec<? extends BankCard> codec = BankCardType.getCodec(input);
        if (codec == null) {
            return DataResult.error(() -> "Bank card class " + input.getClass().getName() + " is not bound to card type: " + input.getCardTypeId());
        }
        return ((Codec<BankCard>) codec).encode(input, ops, prefix);
    }
}

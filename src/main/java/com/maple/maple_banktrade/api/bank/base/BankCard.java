package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * 银行卡基类，仅保存银行卡身份信息。
 * 字段使用 @Persisted 注解，由 LDLib2 自动序列化。
 */
public abstract class BankCard implements IPersistedSerializable {

    private static final String CLIENT_PERMISSION_KEY = "client_permission";

    // ==============================================
    // 字段（非 final，由 PersistedParser 赋值）
    // ==============================================

    @Persisted
    @Getter
    @Setter
    private UUID cardUuid;

    @Persisted
    @Getter
    @Setter
    private Identifier bankTypeId;

    @Persisted
    @Getter
    @Setter
    private Identifier cardTypeId;

    @Persisted
    @Getter
    @Setter
    private Identifier nameIndex;

    @Getter
    private BankCardPermission clientPermission = BankCardPermission.UNUSABLE;

    // ==============================================
    // 构造
    // ==============================================

    /** 无参构造（仅供反序列化） */
    protected BankCard() {}

    /** 业务构造（赋值字段） */
    protected BankCard(BankCardIdentity identity, Identifier cardTypeId) {
        this(identity.cardUuid(), identity.bankTypeId(), cardTypeId, identity.nameIndex());
    }

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

    @Nullable
    public BankType getBankType() {
        return BankType.requireById(bankTypeId);
    }

    public boolean canClientUse() {
        return clientPermission != null && clientPermission.canUse();
    }

    // 客户端快照（不变）
    public CompoundTag toClientTag(BankCardPermission permission) {
        Tag encoded = BankCard.CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        CompoundTag tag = encoded instanceof CompoundTag ct ? ct : new CompoundTag();
        tag.putString(CLIENT_PERMISSION_KEY, (permission == null ? BankCardPermission.UNUSABLE : permission).getSerializedName());
        return tag;
    }

    public static BankCard fromClientTag(Tag tag) {
        if (!(tag instanceof CompoundTag compoundTag) || compoundTag.isEmpty()) return null;
        BankCard card = BankCard.CODEC.parse(NbtOps.INSTANCE, compoundTag).result().orElse(null);
        if (card != null) {
            card.clientPermission = BankCardPermission.bySerializedName(compoundTag.getStringOr(CLIENT_PERMISSION_KEY,
                    BankCardPermission.UNUSABLE.getSerializedName()));
        }
        return card;
    }

    // ==============================================
    // Codec（多态分发，仍使用原方式）
    // ==============================================

    public static final Codec<BankCard> CODEC = Identifier.CODEC.partialDispatch(
            "cardTypeId",
            card -> DataResult.success(card.getCardTypeId()),
            BankCardType::getCodecResult);

    // 辅助记录（保留，不再用于序列化）
    public record BankCardIdentity(UUID cardUuid, Identifier bankTypeId, Identifier nameIndex) {

        public static BankCardIdentity of(BankCard card) {
            return new BankCardIdentity(card.getCardUuid(), card.getBankTypeId(), card.getNameIndex());
        }

        public static BankCardIdentity of(UUID cardUuid, BankType bankType, Identifier nameIndex) {
            return new BankCardIdentity(cardUuid, bankType.id(), nameIndex);
        }
    }
}

package com.maple.maple_banktrade.collaboration.ftbq.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.data.InfoList;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 奖励：增加标记卡指定条目的进度
 */
public class TaggedIncreaseReward extends Reward {

    private static final ResourceLocation DEFAULT = ResourceLocation.withDefaultNamespace("default");

    @Getter
    @Setter
    private ResourceLocation nameIndex = DEFAULT;
    @Getter
    @Setter
    private String entryId = "";
    @Getter
    @Setter
    private int amount = 1;
    @Getter
    @Setter
    private boolean limit = true;   // 默认启用上限

    public TaggedIncreaseReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public @NonNull RewardType getType() {
        return ELFTRewardTypes.TAGGED_INCREASE;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.writeData(tag, provider);
        tag.putString("name_index", nameIndex.toString());
        tag.putString("entry_id", entryId);
        tag.putInt("amount", amount);
        tag.putBoolean("limit", limit);
    }

    @Override
    public void readData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.readData(tag, provider);
        if (tag.contains("name_index")) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("name_index"));
            nameIndex = parsed == null ? DEFAULT : parsed;
        } else {
            nameIndex = DEFAULT;
        }
        entryId = tag.getString("entry_id");
        amount = tag.contains("amount") ? tag.getInt("amount") : 1;
        if (amount < 1) amount = 1;
        limit = !tag.contains("limit") || tag.getBoolean("limit");
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(nameIndex);
        buffer.writeUtf(entryId, 32767);
        buffer.writeVarInt(amount);
        buffer.writeBoolean(limit);
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        nameIndex = buffer.readResourceLocation();
        entryId = buffer.readUtf(32767);
        amount = buffer.readVarInt();
        if (amount < 1) amount = 1;
        limit = buffer.readBoolean();
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull ConfigGroup config) {
        super.fillConfigGroup(config);

        // nameIndex 下拉
        List<ResourceLocation> available = BankCardFactory.values().stream()
                .map(BankCardFactory::nameIndex)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        if (!available.isEmpty()) {
            NameMap<ResourceLocation> nameMap = NameMap.of(available.getFirst(), available.toArray(new ResourceLocation[0]))
                    .name(id -> Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(id))
                            .append(" · ").append(id.toString()))
                    .icon(ignored -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("name_index", nameIndex, v -> nameIndex = v, nameMap)
                    .setNameKey("maple_banktrade.reward.tagged_increase.name_index");
        }

        // entryId 下拉
        InfoList infoList = InfoList.requireByNameIndex(nameIndex);
        if (infoList != null && !infoList.entries().isEmpty()) {
            List<String> entryIds = new ArrayList<>(infoList.entries().keySet());
            NameMap<String> entryMap = NameMap.of(entryIds.getFirst(), entryIds.toArray(new String[0]))
                    .name(id -> {
                        InfoList.InfoEntry entry = infoList.getEntry(id);
                        if (entry != null && entry.display() != null)
                            return entry.display().copy().append(" · ").append(id);
                        return Component.literal(id);
                    })
                    .icon(ignored -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("entry_id", entryId, v -> entryId = v, entryMap)
                    .setNameKey("maple_banktrade.reward.tagged_increase.entry_id");
        } else {
            // 备用文本输入
            config.addString("entry_id", entryId, v -> entryId = v, "")
                    .setNameKey("maple_banktrade.reward.tagged_increase.entry_id");
        }

        // 增加数量
        config.addInt("amount", amount, v -> amount = v, 1, 1, Integer.MAX_VALUE)
                .setNameKey("maple_banktrade.reward.tagged_increase.amount");

        // 是否限制上限
        config.addBool("limit", limit, v -> limit = v, true)
                .setNameKey("maple_banktrade.reward.tagged_increase.limit");
    }

    // ---------- 任务标题 ----------
    @Override
    public @NonNull MutableComponent getAltTitle() {
        Component cardName = Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(nameIndex))
                .withStyle(ChatFormatting.YELLOW);
        Component entryDisplay;
        InfoList infoList = InfoList.requireByNameIndex(nameIndex);
        if (infoList != null) {
            InfoList.InfoEntry entry = infoList.getEntry(entryId);
            if (entry != null && entry.display() != null) entryDisplay = entry.display().copy().withStyle(ChatFormatting.DARK_GREEN);
            else entryDisplay = Component.literal(entryId).withStyle(ChatFormatting.DARK_GREEN);
        } else {
            entryDisplay = Component.literal(entryId).withStyle(ChatFormatting.DARK_GREEN);
        }
        return Component.literal("")
                .append(Component.literal(limit ? "⬆ " : "⬆∞ ").withStyle(limit ? ChatFormatting.GREEN : ChatFormatting.GOLD))
                .append(cardName)
                .append(" · ")
                .append(entryDisplay)
                .append(" ")
                .append(Component.literal("+" + amount).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public @NonNull String getButtonText() {
        return (limit ? "+" : "+∞") + amount;
    }

    // ---------- 核心逻辑 ----------
    @Override
    public void claim(ServerPlayer player, boolean notify) {
        BankCard card = MBTBankStates.getBankCards(player.serverLevel())
                .getUsableCardsForPlayer(BankHelper.getUuid(player))
                .stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny()
                .orElse(null);

        if (card instanceof TaggedBankCard taggedCard) {
            taggedCard.addProgress(entryId, amount, limit);
            MBTBankStates.markDirty(player.serverLevel());
        }
    }

    @Override
    public boolean ignoreRewardBlocking() {
        return true;
    }
}

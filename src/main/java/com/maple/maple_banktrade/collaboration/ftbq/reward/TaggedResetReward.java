package com.maple.maple_banktrade.collaboration.ftbq.reward;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.data.InfoList;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.util.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardAutoClaim;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 奖励：重置标记卡指定条目的进度为 0
 */
public class TaggedResetReward extends Reward {

    private static final Identifier DEFAULT = Identifier.withDefaultNamespace("default");

    @Getter
    @Setter
    private Identifier nameIndex = DEFAULT;
    @Getter
    @Setter
    private String entryId = "";

    public TaggedResetReward(long id, Quest quest) {
        super(id, quest);
        autoclaim = RewardAutoClaim.INVISIBLE;
    }

    @Override
    public @NonNull RewardType getType() {
        return ELFTRewardTypes.TAGGED_RESET;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.writeData(json, provider);
        Json5Util.store(json, "name_index", Identifier.CODEC, nameIndex);
        json.addProperty("entry_id", entryId);
    }

    @Override
    public void readData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.readData(json, provider);
        nameIndex = Json5Util.fetch(json, "name_index", Identifier.CODEC).orElse(DEFAULT);
        entryId = Json5Util.getString(json, "entry_id").orElse("");
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeIdentifier(nameIndex);
        buffer.writeUtf(entryId, 32767);
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        nameIndex = buffer.readIdentifier();
        entryId = buffer.readUtf(32767);
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull EditableConfigGroup config) {
        super.fillConfigGroup(config);

        List<Identifier> available = BankCardFactory.values().stream()
                .map(BankCardFactory::nameIndex)
                .sorted(Comparator.comparing(Identifier::toString))
                .toList();
        if (!available.isEmpty()) {
            NameMap<Identifier> nameMap = NameMap.of(available.getFirst(), available.toArray(new Identifier[0]))
                    .name(id -> Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(id))
                            .append(" · ").append(id.toString()))
                    .icon(_ -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("name_index", nameIndex, v -> nameIndex = v, nameMap)
                    .setNameKey("maple_banktrade.reward.tagged_reset.name_index");
        }

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
                    .icon(_ -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("entry_id", entryId, v -> entryId = v, entryMap)
                    .setNameKey("maple_banktrade.reward.tagged_reset.entry_id");
        }
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
                .append(Component.literal("⟳ ").withStyle(ChatFormatting.RED))
                .append(cardName)
                .append(" · ")
                .append(entryDisplay)
                .append(" ")
                .append(Component.literal("(重置)").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public @NonNull String getButtonText() {
        return "⟳";
    }

    // ---------- 核心逻辑 ----------
    @Override
    public void claim(ServerPlayer player, boolean notify) {
        BankCard card = MBTBankStates.getBankCards(player.level())
                .getUsableCardsForPlayer(BankHelper.getUuid(player))
                .stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny()
                .orElse(null);

        if (card instanceof TaggedBankCard taggedCard) {
            taggedCard.resetProgress(entryId);
            MBTBankStates.markDirty(player.level());
        }
    }

    @Override
    public boolean ignoreRewardBlocking() {
        return true;
    }
}

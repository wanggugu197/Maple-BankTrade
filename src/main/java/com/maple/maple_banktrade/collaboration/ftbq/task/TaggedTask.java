package com.maple.maple_banktrade.collaboration.ftbq.task;

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
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 记录卡完成度检测任务（支持完成状态或进度阈值检测）
 */
public class TaggedTask extends AbstractBooleanTask {

    private static final ResourceLocation DEFAULT = ResourceLocation.withDefaultNamespace("default");

    @Getter
    @Setter
    private ResourceLocation nameIndex = DEFAULT;
    @Getter
    @Setter
    private String entryId = "";

    // 新增字段
    @Getter
    @Setter
    private boolean useProgress = false;
    @Getter
    @Setter
    private int requiredProgress = 1;

    public TaggedTask(long id, Quest quest) {
        super(id, quest);
    }

    // ---------- 任务类型 ----------
    @Override
    public @NonNull TaskType getType() {
        return ELFTTaskTypes.TAGGED_TASK;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.writeData(tag, provider);
        tag.putString("name_index", nameIndex.toString());
        tag.putString("entry_id", entryId);
        tag.putBoolean("use_progress", useProgress);
        tag.putInt("required_progress", requiredProgress);
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
        useProgress = tag.getBoolean("use_progress");
        requiredProgress = tag.contains("required_progress") ? tag.getInt("required_progress") : 1;
        if (requiredProgress < 1) requiredProgress = 1;
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(nameIndex);
        buffer.writeUtf(entryId, 32767);
        buffer.writeBoolean(useProgress);
        buffer.writeVarInt(requiredProgress);
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        nameIndex = buffer.readResourceLocation();
        entryId = buffer.readUtf(32767);
        useProgress = buffer.readBoolean();
        requiredProgress = buffer.readVarInt();
        if (requiredProgress < 1) requiredProgress = 1;
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull ConfigGroup config) {
        super.fillConfigGroup(config);

        List<ResourceLocation> availableNameIndices = BankCardFactory.values().stream()
                .map(BankCardFactory::nameIndex)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();

        if (!availableNameIndices.isEmpty()) {
            NameMap<ResourceLocation> nameMap = NameMap.of(availableNameIndices.getFirst(), availableNameIndices.toArray(new ResourceLocation[0]))
                    .name(id -> Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(id))
                            .append(" · ").append(id.toString()))
                    .icon(ignored -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("name_index", nameIndex, v -> nameIndex = v, nameMap)
                    .setNameKey("maple_banktrade.task.tagged.name_index");
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
                    .icon(ignored -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"))
                    .create();
            config.addEnum("entry_id", entryId, v -> entryId = v, entryMap)
                    .setNameKey("maple_banktrade.task.tagged.entry_id");
        }

        config.addBool("use_progress", useProgress, v -> useProgress = v, false)
                .setNameKey("maple_banktrade.task.tagged.use_progress");

        config.addInt("required_progress", requiredProgress, v -> {
            if (v < 1) v = 1;
            requiredProgress = v;
        }, 1, 1, Integer.MAX_VALUE)
                .setNameKey("maple_banktrade.task.tagged.required_progress");
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
            if (entry != null && entry.display() != null) {
                entryDisplay = entry.display().copy().withStyle(ChatFormatting.DARK_GREEN);
            } else {
                entryDisplay = Component.literal(entryId).withStyle(ChatFormatting.DARK_GREEN);
            }
        } else {
            entryDisplay = Component.literal(entryId).withStyle(ChatFormatting.DARK_GREEN);
        }

        String modeInfo = useProgress ? " (≥ " + requiredProgress + ")" : " (完成)";
        return Component.empty()
                .append(cardName)
                .append(" · ")
                .append(entryDisplay)
                .append(Component.literal(modeInfo).withStyle(ChatFormatting.GRAY));
    }

    // ---------- 完成条件 ----------
    @Override
    public boolean canSubmit(@NonNull TeamData teamData, ServerPlayer player) {
        BankCard card = MBTBankStates.getBankCards(player.serverLevel())
                .getUsableCardsForPlayer(BankHelper.getUuid(player))
                .stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);

        if (card instanceof TaggedBankCard taggedCard) {
            if (useProgress) return taggedCard.getProgress(entryId) >= requiredProgress;
            else return taggedCard.isComplete(entryId);
        }
        return false;
    }

    // ---------- 自动检测 ----------
    @Override
    public int autoSubmitOnPlayerTick() {
        return 300;
    }
}

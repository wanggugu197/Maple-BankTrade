package com.maple.maple_banktrade.collaboration.ftbq.task;

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
import com.maple.maple_banktrade.api.bank.data.InfoList.InfoEntry;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import de.marhali.json5.Json5Object;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.json5.Json5Util;
import dev.ftb.mods.ftblibrary.util.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.AbstractBooleanTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 记录卡完成程度任务（布尔任务，检测是否达成指定条件）
 * 支持多种检测模式，当条件满足时一次性完成
 */
public class TaggedProgressTask extends AbstractBooleanTask {

    private static final Identifier DEFAULT = Identifier.withDefaultNamespace("default");

    // 检测模式枚举
    public enum Mode {
        TOTAL_COUNT,    // 完成指定数量的条目（任意条目）
        TOTAL_RATIO,    // 完成指定比例的条目（百分比）
        TIER_COUNT,     // 在指定 tier 中完成指定数量的条目
        TIER_RATIO,     // 在指定 tier 中完成指定比例的条目
        ALL;            // 完成所有条目
    }

    @Getter
    @Setter
    private Identifier nameIndex = DEFAULT;
    @Getter
    @Setter
    private Mode mode = Mode.TOTAL_COUNT;
    @Getter
    @Setter
    private int tier = 1;
    @Getter
    @Setter
    private int targetValue = 1;   // 统一目标值：数量或百分比

    public TaggedProgressTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public @NonNull TaskType getType() {
        return ELFTTaskTypes.TAGGED_PROGRESS_TASK;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.writeData(json, provider);
        Json5Util.store(json, "name_index", Identifier.CODEC, nameIndex);
        json.addProperty("mode", mode.name());
        json.addProperty("tier", tier);
        json.addProperty("target_value", targetValue);
    }

    @Override
    public void readData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.readData(json, provider);
        nameIndex = Json5Util.fetch(json, "name_index", Identifier.CODEC).orElse(DEFAULT);
        mode = Enum.valueOf(Mode.class, Json5Util.getString(json, "mode").orElse(Mode.TOTAL_COUNT.name()));
        tier = Json5Util.getInt(json, "tier").orElse(1);
        targetValue = Json5Util.getInt(json, "target_value").orElse(1);
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeIdentifier(nameIndex);
        buffer.writeEnum(mode);
        buffer.writeVarInt(tier);
        buffer.writeVarInt(targetValue);
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        nameIndex = buffer.readIdentifier();
        mode = buffer.readEnum(Mode.class);
        tier = buffer.readVarInt();
        targetValue = buffer.readVarInt();
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull EditableConfigGroup config) {
        super.fillConfigGroup(config);

        // nameIndex 选择
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
                    .setNameKey("maple_banktrade.task.tagged_progress.name_index");
        }

        // 模式选择
        NameMap<Mode> modeMap = NameMap.of(Mode.TOTAL_COUNT, Mode.values())
                .name(m -> Component.translatable("maple_banktrade.task.tagged_progress.mode." + m.name().toLowerCase()))
                .create();
        config.addEnum("mode", mode, v -> mode = v, modeMap)
                .setNameKey("maple_banktrade.task.tagged_progress.mode");

        // tier 选项（仅在 TIER_* 模式下显示）
        if (mode == Mode.TIER_COUNT || mode == Mode.TIER_RATIO) {
            config.addInt("tier", tier, v -> tier = v, 1, 0, 32)
                    .setNameKey("maple_banktrade.task.tagged_progress.tier");
        }

        // 统一目标值，根据模式改变标签和范围
        String key;
        int min, max;
        if (mode == Mode.TOTAL_RATIO || mode == Mode.TIER_RATIO) {
            key = "maple_banktrade.task.tagged_progress.percent";
            min = 0;
            max = 100;
        } else if (mode == Mode.ALL) {
            // ALL 模式不需要目标值，保持可编辑但实际不使用
            key = "maple_banktrade.task.tagged_progress.target_value";
            min = 1;
            max = Integer.MAX_VALUE;
        } else {
            key = "maple_banktrade.task.tagged_progress.threshold";
            min = 1;
            max = Integer.MAX_VALUE;
        }
        config.addInt("target_value", targetValue, v -> targetValue = Math.clamp(v, min, max), min, min, max)
                .setNameKey(key);
    }

    // ---------- 任务标题 ----------
    @Override
    public @NonNull MutableComponent getAltTitle() {
        Component cardName = Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(nameIndex))
                .withStyle(ChatFormatting.YELLOW);
        Component modeDesc = Component.translatable("maple_banktrade.task.tagged_progress.mode." + mode.name().toLowerCase())
                .withStyle(ChatFormatting.AQUA);
        String detail = switch (mode) {
            case TOTAL_COUNT, TIER_COUNT -> " (" + targetValue + ")";
            case TOTAL_RATIO, TIER_RATIO -> " (" + targetValue + "%)";
            case ALL -> "";
        };
        if (mode == Mode.TIER_COUNT || mode == Mode.TIER_RATIO) {
            detail = "tier " + tier + " ≥ " + detail;
        }
        return Component.literal("")
                .append(cardName)
                .append(" · ")
                .append(modeDesc)
                .append(Component.literal(detail).withStyle(ChatFormatting.GREEN));
    }

    // ---------- 核心完成条件 ----------
    @Override
    public boolean canSubmit(@NonNull TeamData teamData, ServerPlayer player) {
        BankCard card = MBTBankStates.getBankCards(player.level())
                .getUsableCardsForPlayer(BankHelper.getUuid(player))
                .stream()
                .filter(c -> c.getNameIndex().equals(nameIndex))
                .findAny().orElse(null);

        if (card instanceof TaggedBankCard taggedCard) {
            InfoList list = taggedCard.getInfoList();
            if (list == null) return false;

            Set<String> targets = list.entries().keySet();
            if (targets.isEmpty()) return false;

            long completed = 0;
            if (mode == Mode.TOTAL_COUNT || mode == Mode.ALL) {
                for (String id : targets) if (taggedCard.isComplete(id)) completed++;
                if (mode == Mode.ALL) return completed >= targets.size();
                return completed >= targetValue;
            } else if (mode == Mode.TOTAL_RATIO) {
                int total = targets.size();
                for (String id : targets) if (taggedCard.isComplete(id)) completed++;
                return (int) (100L * completed / total) >= targetValue;
            } else if (mode == Mode.TIER_COUNT) {
                int tierCompleted = 0;
                for (InfoEntry entry : list.entries().values()) if (entry.tire() == tier && targets.contains(entry.id()) && taggedCard.isComplete(entry.id())) tierCompleted++;
                return tierCompleted >= targetValue;
            } else if (mode == Mode.TIER_RATIO) {
                int totalInTier = 0;
                int completedInTier = 0;
                for (InfoEntry entry : list.entries().values()) {
                    if (entry.tire() == tier && targets.contains(entry.id())) {
                        totalInTier++;
                        if (taggedCard.isComplete(entry.id())) completedInTier++;
                    }
                }
                if (totalInTier == 0) return false;
                return (int) (100L * completedInTier / totalInTier) >= targetValue;
            }
        }
        return false;
    }

    // ---------- 自动检测 ----------
    @Override
    public int autoSubmitOnPlayerTick() {
        return 300;
    }
}

package com.maple.maple_banktrade.collaboration.ftbq.task;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * 银行卡货币数量任务（布尔任务）
 * 检测玩家所有银行卡中指定货币的总余额是否达到目标数量，
 * 可选择在提交时消耗该货币。
 */
public class CurrencyTask extends AbstractBooleanTask {

    private static final ResourceLocation DEFAULT_CURRENCY = ResourceLocation.withDefaultNamespace("coins");

    @Getter
    @Setter
    private ResourceLocation currencyTypeId = DEFAULT_CURRENCY;
    @Getter
    @Setter
    private BigInteger amount = BigInteger.valueOf(1000);
    @Getter
    @Setter
    private boolean consume = true;

    public CurrencyTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public @NonNull TaskType getType() {
        return ELFTTaskTypes.CURRENCY_TASK;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.writeData(tag, provider);
        tag.putString("currency", currencyTypeId.toString());
        tag.putString("amount", amount.toString());
        tag.putBoolean("consume", consume);
    }

    @Override
    public void readData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.readData(tag, provider);
        if (tag.contains("currency")) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("currency"));
            currencyTypeId = parsed == null ? DEFAULT_CURRENCY : parsed;
        } else {
            currencyTypeId = DEFAULT_CURRENCY;
        }
        String amtStr = tag.getString("amount");
        try {
            amount = new BigInteger(amtStr.isEmpty() ? "1000" : amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(1000);
        }
        consume = !tag.contains("consume") || tag.getBoolean("consume");
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(currencyTypeId);
        buffer.writeUtf(amount.toString());
        buffer.writeBoolean(consume);
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        currencyTypeId = buffer.readResourceLocation();
        String amtStr = buffer.readUtf();
        try {
            amount = new BigInteger(amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(1000);
        }
        consume = buffer.readBoolean();
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull ConfigGroup config) {
        super.fillConfigGroup(config);

        List<CurrencyType> types = CurrencyType.values().stream()
                .sorted(Comparator.comparing(ct -> ct.id().toString()))
                .toList();
        if (!types.isEmpty()) {
            NameMap<ResourceLocation> nameMap = NameMap.of(types.getFirst().id(),
                    types.stream().map(CurrencyType::id).toArray(ResourceLocation[]::new))
                    .name(id -> Component.translatable(CurrencyType.getCurrencyTypeTranslationKey(id))
                            .append(" · ").append(id.toString()))
                    .icon(id -> Icon.getIcon("maple_banktrade:item/coins"))
                    .create();
            config.addEnum("currency", currencyTypeId, v -> currencyTypeId = v, nameMap)
                    .setNameKey("maple_banktrade.task.currency.currency");
        }

        config.addString("amount", amount.toString(), v -> {
            try {
                amount = new BigInteger(v);
            } catch (NumberFormatException ignored) {}
        }, "1000").setNameKey("maple_banktrade.task.currency.amount");

        config.addBool("consume", consume, v -> consume = v, true)
                .setNameKey("maple_banktrade.task.currency.consume");
    }

    // ---------- 任务标题 ----------
    @Override
    public @NonNull MutableComponent getAltTitle() {
        Component currencyName = Component.translatable(CurrencyType.getCurrencyTypeTranslationKey(currencyTypeId))
                .withStyle(ChatFormatting.YELLOW);
        String amountStr = amount.toString();
        return Component.literal("")
                .append(currencyName)
                .append(" · ")
                .append(Component.literal(amountStr).withStyle(ChatFormatting.GREEN))
                .append(Component.translatable(consume ? "maple_banktrade.task.currency.consume_suffix" : "maple_banktrade.task.currency.detect_only_suffix"));
    }

    // ---------- 核心逻辑 ----------
    /**
     * 获取第一张支持该货币且余额充足的卡
     */
    private BankCard findFirstSufficientCard(ServerPlayer player) {
        List<BankCard> cards = MBTBankStates.getBankCards(player.serverLevel())
                .getUsableCardsForPlayer(BankHelper.getUuid(player));
        for (BankCard card : cards) {
            if (card instanceof CurrencyStorageBankCard currencyCard) {
                BigInteger balance = currencyCard.getCurrencyBalance(currencyTypeId);
                if (balance.compareTo(amount) >= 0) {
                    return card;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canSubmit(@NonNull TeamData teamData, @NonNull ServerPlayer player) {
        return findFirstSufficientCard(player) != null;
    }

    // ---------- submitTask ----------
    @Override
    public void submitTask(@NonNull TeamData teamData, @NonNull ServerPlayer player, @NonNull ItemStack craftedItem) {
        BankCard card = findFirstSufficientCard(player);
        if (card instanceof CurrencyStorageBankCard currencyCard) {
            if (consume) {
                boolean success = currencyCard.decreaseCurrency(currencyTypeId, amount);
                if (success) {
                    MBTBankStates.markDirty(player.serverLevel());
                    super.submitTask(teamData, player, craftedItem);
                }
            } else {
                super.submitTask(teamData, player, craftedItem);
            }
        }
    }

    @Override
    public boolean consumesResources() {
        return consume;
    }
}

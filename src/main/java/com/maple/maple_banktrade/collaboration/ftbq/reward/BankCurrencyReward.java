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
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * 银行卡货币奖励：增加或减少玩家银行卡中的指定货币数量
 */
public class BankCurrencyReward extends Reward {

    private static final ResourceLocation DEFAULT_CURRENCY = ResourceLocation.withDefaultNamespace("coins");

    @Getter
    @Setter
    private ResourceLocation currencyTypeId = DEFAULT_CURRENCY;
    @Getter
    @Setter
    private BigInteger amount = BigInteger.valueOf(100);

    public BankCurrencyReward(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public @NonNull RewardType getType() {
        return ELFTRewardTypes.BANK_CURRENCY;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull CompoundTag tag, HolderLookup.@NonNull Provider provider) {
        super.writeData(tag, provider);
        tag.putString("currency", currencyTypeId.toString());
        tag.putString("amount", amount.toString());
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
            amount = new BigInteger(amtStr.isEmpty() ? "100" : amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(100);
        }
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(currencyTypeId);
        buffer.writeUtf(amount.toString());
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        currencyTypeId = buffer.readResourceLocation();
        String amtStr = buffer.readUtf();
        try {
            amount = new BigInteger(amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(100);
        }
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
                    .setNameKey("maple_banktrade.reward.bank_currency.currency");
        }

        config.addString("amount", amount.toString(), v -> {
            try {
                amount = new BigInteger(v);
            } catch (NumberFormatException ignored) {}
        }, "100")
                .setNameKey("maple_banktrade.reward.bank_currency.amount");
    }

    // ---------- 任务标题 ----------
    @Override
    public @NonNull MutableComponent getAltTitle() {
        Component currencyName = Component.translatable(CurrencyType.getCurrencyTypeTranslationKey(currencyTypeId))
                .withStyle(ChatFormatting.YELLOW);
        String amountStr = amount.toString();
        return Component.literal("")
                .append(Component.literal("⬆ ").withStyle(ChatFormatting.RED))
                .append(currencyName)
                .append(" · ")
                .append(Component.literal(amountStr).withStyle(ChatFormatting.GREEN));
    }

    // ---------- 核心逻辑 ----------
    @Override
    public void claim(@NonNull ServerPlayer player, boolean notify) {
        List<BankCard> cards = MBTBankStates.getBankCards(player.serverLevel())
                .getUsableCardsForPlayer(BankHelper.getUuid(player));
        for (BankCard card : cards) {
            if (card instanceof CurrencyStorageBankCard currencyCard) {
                currencyCard.increaseCurrency(currencyTypeId, amount);
                break;
            }
        }
        MBTBankStates.markDirty(player.serverLevel());
    }

    @Override
    public boolean ignoreRewardBlocking() {
        return true;
    }
}

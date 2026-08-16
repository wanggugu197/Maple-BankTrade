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
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.data.CurrencyType;
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

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;

/**
 * 银行卡货币奖励：增加或减少玩家银行卡中的指定货币数量
 */
public class BankCurrencyReward extends Reward {

    private static final Identifier DEFAULT_CURRENCY = Identifier.withDefaultNamespace("coins");

    @Getter
    @Setter
    private Identifier currencyTypeId = DEFAULT_CURRENCY;
    @Getter
    @Setter
    private BigInteger amount = BigInteger.valueOf(100);

    public BankCurrencyReward(long id, Quest quest) {
        super(id, quest);
        autoclaim = RewardAutoClaim.INVISIBLE;
    }

    @Override
    public @NonNull RewardType getType() {
        return ELFTRewardTypes.BANK_CURRENCY;
    }

    // ---------- 序列化 ----------
    @Override
    public void writeData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.writeData(json, provider);
        Json5Util.store(json, "currency", Identifier.CODEC, currencyTypeId);
        json.addProperty("amount", amount.toString());
    }

    @Override
    public void readData(@NonNull Json5Object json, HolderLookup.@NonNull Provider provider) {
        super.readData(json, provider);
        currencyTypeId = Json5Util.fetch(json, "currency", Identifier.CODEC).orElse(DEFAULT_CURRENCY);
        String amtStr = Json5Util.getString(json, "amount").orElse("100");
        try {
            amount = new BigInteger(amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(100);
        }
    }

    // ---------- 网络同步 ----------
    @Override
    public void writeNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeIdentifier(currencyTypeId);
        buffer.writeUtf(amount.toString());
    }

    @Override
    public void readNetData(@NonNull RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        currencyTypeId = buffer.readIdentifier();
        String amtStr = buffer.readUtf();
        try {
            amount = new BigInteger(amtStr);
        } catch (NumberFormatException e) {
            amount = BigInteger.valueOf(100);
        }
    }

    // ---------- 配置界面 ----------
    @Override
    public void fillConfigGroup(@NonNull EditableConfigGroup config) {
        super.fillConfigGroup(config);

        List<CurrencyType> types = CurrencyType.values().stream()
                .sorted(Comparator.comparing(ct -> ct.id().toString()))
                .toList();
        if (!types.isEmpty()) {
            NameMap<Identifier> nameMap = NameMap.of(types.getFirst().id(),
                    types.stream().map(CurrencyType::id).toArray(Identifier[]::new))
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
        List<BankCard> cards = MBTBankStates.getBankCards(player.level())
                .getUsableCardsForPlayer(BankHelper.getUuid(player));
        for (BankCard card : cards) {
            if (card instanceof CurrencyStorageBankCard currencyCard) {
                currencyCard.increaseCurrency(currencyTypeId, amount);
                break;
            }
        }

        MBTBankStates.markDirty(player.level());
    }

    @Override
    public boolean ignoreRewardBlocking() {
        return true;
    }
}

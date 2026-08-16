package com.maple.maple_banktrade.collaboration.ftbq.reward;

import com.maple.maple_banktrade.MapleBankTrade;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;

public class ELFTRewardTypes {

    public static final RewardType TAGGED_INCREASE = RewardTypes.register(
            MapleBankTrade.id("tagged_increase"),
            TaggedIncreaseReward::new,
            () -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"));

    public static final RewardType TAGGED_RESET = RewardTypes.register(
            MapleBankTrade.id("tagged_reset"),
            TaggedResetReward::new,
            () -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"));

    public static final RewardType BANK_CURRENCY = RewardTypes.register(
            MapleBankTrade.id("bank_currency"),
            BankCurrencyReward::new,
            () -> Icon.getIcon("maple_banktrade:item/coins"));

    public static void init() {}
}

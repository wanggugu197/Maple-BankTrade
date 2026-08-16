package com.maple.maple_banktrade.collaboration.ftbq.task;

import com.maple.maple_banktrade.MapleBankTrade;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;

public class ELFTTaskTypes {

    public static final TaskType TAGGED_TASK = TaskTypes.register(
            MapleBankTrade.id("tagged"),
            TaggedTask::new,
            () -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"));

    public static final TaskType TAGGED_PROGRESS_TASK = TaskTypes.register(
            MapleBankTrade.id("tagged_progress"),
            TaggedProgressTask::new,
            () -> Icon.getIcon("maple_banktrade:item/bank_permissions_card_builder"));

    public static final TaskType CURRENCY_TASK = TaskTypes.register(
            MapleBankTrade.id("currency"),
            CurrencyTask::new,
            () -> Icon.getIcon("maple_banktrade:item/coins"));

    public static void init() {}
}

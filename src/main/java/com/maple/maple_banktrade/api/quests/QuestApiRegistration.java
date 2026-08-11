package com.maple.maple_banktrade.api.quests;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.api.quests.item.QuestBookAttachment;
import com.maple.maple_banktrade.api.quests.reward.RewardRegistry;
import com.maple.maple_banktrade.api.quests.scheduler.QuestTriggerHandler;
import com.maple.maple_banktrade.api.quests.storage.QuestDataManager;
import com.maple.maple_banktrade.api.quests.tasktype.TaskTypeRegistry;
import com.maple.maple_banktrade.api.quests.ui.QuestUIRegistration;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTRecipe.UNCONDITIONAL_CRITERION;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;

/**
 * 任务书物品与任务 UI 的 API 注册入口。
 *
 * <p>
 * 仿照 {@link com.maple.maple_banktrade.api.bank.WalletApiRegistration} 模式，
 * 集中管理任务书物品注册、配方、任务系统各子模块的初始化。
 */
public class QuestApiRegistration {

    // ==============================================
    // 注册
    // ==============================================

    /** 任务书物品。 */
    public static final ItemEntry<ComponentItem> QUEST_BOOK = REGISTRY
            .componentItem("quest_book")
            .langCn("任务书")
            .lang("Quest Book")
            .attach(new QuestBookAttachment())
            .addTab(TAB_BANK.getKey())
            .register();

    // ==============================================
    // 配方
    // ==============================================

    private static void addRecipe() {
        REGISTRY.addRecipeData(prov -> {
            prov.shaped(RecipeCategory.MISC, QUEST_BOOK.get())
                    .pattern("ABA")
                    .pattern("BCB")
                    .pattern("ABA")
                    .define('A', Items.GREEN_DYE)
                    .define('B', Items.PAPER)
                    .define('C', Items.BOOK)
                    .unlockedBy("unlocked", UNCONDITIONAL_CRITERION)
                    .save(prov, "make_quest_book");
        });
    }

    // ==============================================
    // 初始化
    // ==============================================

    /**
     * 注册任务书物品、配方，并初始化任务系统的所有子模块。
     */
    public static void init() {
        addRecipe();
        QuestDataManager.init();
        QuestTriggerHandler.init();
        QuestUIRegistration.init();
        TaskTypeRegistry.init();
        RewardRegistry.init();
    }
}

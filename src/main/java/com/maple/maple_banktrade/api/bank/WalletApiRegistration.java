package com.maple.maple_banktrade.api.bank;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.util.entry.ItemEntry;
import com.maple.maple_banktrade.api.bank.command.MBTBankCommands;
import com.maple.maple_banktrade.api.bank.item.BankPermissionsCardAttachment;
import com.maple.maple_banktrade.api.bank.item.BankPermissionsCardBuilderAttachment;
import com.maple.maple_banktrade.api.bank.item.WalletAttachment;
import com.maple.maple_banktrade.api.bank.ui.BankCardDetailUIRegistration;
import com.maple.maple_banktrade.api.bank.ui.BankPermissionsCardUIRegistration;
import com.maple.maple_banktrade.api.bank.ui.ParameterizedPlayerUIMenuType;
import com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration;
import com.maple.maple_banktrade.common.MBTDataComponent;

import java.util.HashSet;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;
import static com.maple.maple_banktrade.common.MBTRecipe.UNCONDITIONAL_CRITERION;
import static com.maple.maple_banktrade.common.MBTTab.TAB_BANK;

/**
 * 钱包物品与相关 UI 的 API 注册入口。
 */
public class WalletApiRegistration {

    // ==============================================
    // 注册
    // ==============================================

    /** 钱包物品。 */
    public static final ItemEntry<ComponentItem> WALLET = REGISTRY
            .componentItem("wallet")
            .langCn("钱包")
            .attach(new WalletAttachment())
            .addTab(TAB_BANK.getKey())
            .register();

    /**
     * 银行权限卡构造器：使用打开编辑 UI，可输出权限卡。
     * 不携带 {@link MBTDataComponent#CARD_PERMISSIONS}。
     */
    public static final ItemEntry<ComponentItem> BANK_PERMISSIONS_CARD_BUILDER = REGISTRY
            .componentItem("bank_permissions_card_builder")
            .langCn("银行权限卡构造器")
            .lang("Bank Permissions Card Builder")
            .properties(p -> p.stacksTo(1))
            .attach(new BankPermissionsCardBuilderAttachment())
            .addTab(TAB_BANK.getKey())
            .register();

    /**
     * 银行权限卡：保存 {@link MBTDataComponent#CARD_PERMISSIONS}（卡 UUID 集合）。
     * 由构造器 UI「输出物品」生成；Tooltip 由 {@link BankPermissionsCardAttachment} 提供。
     */
    public static final ItemEntry<ComponentItem> BANK_PERMISSIONS_CARD = REGISTRY
            .componentItem("bank_permissions_card")
            .langCn("银行权限卡")
            .lang("Bank Permissions Card")
            .properties(p -> p.component(MBTDataComponent.CARD_PERMISSIONS, new HashSet<>()))
            .attach(new BankPermissionsCardAttachment())
            .addTab(TAB_BANK.getKey())
            .register();

    private static void addRecipe() {
        REGISTRY.addRecipeData(prov -> prov.shaped(RecipeCategory.COMBAT, WALLET.get())
                .pattern("AAA").pattern("ABA").pattern("AAA")
                .define('A', Items.BLUE_DYE)
                .define('B', Items.PAPER)
                .unlockedBy("unlocked", UNCONDITIONAL_CRITERION)
                .save(prov, "make_wallet"));

        REGISTRY.addRecipeData(prov -> prov.shaped(RecipeCategory.COMBAT, BANK_PERMISSIONS_CARD_BUILDER.get())
                .pattern("BBB").pattern("BOB").pattern("BAB")
                .define('A', Items.ORANGE_DYE)
                .define('B', Items.BLUE_DYE)
                .define('O', Items.BLUE_STAINED_GLASS_PANE)
                .unlockedBy("unlocked", UNCONDITIONAL_CRITERION)
                .save(prov, "make_bank_permissions_card_builder"));
    }

    /** 注册参数化菜单、卡详情 UI、权限卡 UI 与钱包 UI 世界数据注册入口。 */
    public static void init(IEventBus modBus) {
        addRecipe();
        ParameterizedPlayerUIMenuType.init(modBus);
        BankCardDetailUIRegistration.init();
        BankPermissionsCardUIRegistration.init();
        WalletUIRegistration.init();
        MBTBankStates.init();
        // 初始化银行命令
        MBTBankCommands.init();
    }
}

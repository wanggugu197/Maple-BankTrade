package com.maple.maple_banktrade.data.lang;

import net.minecraft.network.chat.Component;

import com.mapleutillib.api.registry.ModLangProvider;

import static com.maple.maple_banktrade.MapleBankTrade.REGISTRY;

/**
 * 中英文语言键注册与翻译组件工厂。
 */
public class MBTLangHandler {

    // ==============================================
    // 语言键注册
    // ==============================================

    /** 数据生成时注册中英文，并返回可翻译组件。 */
    public static Component addLang(String key, String cn, String en) {
        if (REGISTRY.doDatagen()) {
            REGISTRY.lang(key, en);
            REGISTRY.lang(ModLangProvider.LANG_ZH_CN, key, cn);
        }
        return Component.translatable(key);
    }

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册 UI / 命令 / 交易相关语言键（仅数据生成时）。 */
    public static void init() {
        if (!REGISTRY.doDatagen()) return;

        addLang("tooltip.bank.card_count", "持有此银行银行卡%s张", "Owned bank cards: %s");
        addLang("ui.maple_banktrade.wallet.open", "点击以进入", "Click to open");
        addLang("ui.bank.card.list.empty", "此银行没有可用银行卡", "No usable bank cards in this bank");
        addLang("ui.bank.card.list_entry", "%s | %s | %s", "%s | %s | %s");
        addLang("ui.bank.card.detail.uuid", "UUID:%s", "UUID:%s");
        addLang("ui.bank.card.detail.currency", "货币: %s", "Currency: %s");
        addLang("ui.bank.card.detail.balance", "余额: %s", "Balance: %s");
        addLang("ui.bank.card.detail.currency_balance", "%s: %s", "%s: %s");
        addLang("ui.bank.card.detail.tags.empty", "标签: 无", "Tags: none");
        addLang("ui.bank.card.detail.tags", "标签: %s", "Tags: %s");
        addLang("ui.bank.card.detail.info_list.summary", "%s 完成 %s / %s 项", "%s completed %s/%s items");
        addLang("ui.bank.card.detail.info_list.missing", "信息列表未找到", "Info list not found");
        addLang("ui.bank.card.detail.info_list.header", "信息列表：%s", "Info list: %s");
        addLang("ui.bank.card.detail.info_list.empty", "该列表没有条目", "This list has no entries");
        addLang("ui.bank.card.detail.complete", "已完成", "Completed");
        addLang("ui.bank.card.detail.incomplete", "未完成", "Incomplete");

        addLang("ui.bank.card.manage.open", "\uD83D\uDC64", "\uD83D\uDC64");
        addLang("ui.bank.card.manage.title", "权限管理", "Permissions");
        addLang("ui.bank.card.manage.grant", "授予权限", "Grant");
        addLang("ui.bank.card.manage.holders", "当前权限", "Holders");
        addLang("ui.bank.card.manage.grant_admin", "管理员", "Admin");
        addLang("ui.bank.card.manage.grant_usable", "使用者", "User");
        addLang("ui.bank.card.manage.revoke", "移除", "Remove");
        addLang("ui.bank.card.manage.delete", "删除卡", "Delete");
        addLang("ui.bank.card.manage.delete_confirm_1", "确认删除?(%s/%s)", "Confirm?(%s/%s)");
        addLang("ui.bank.card.manage.delete_confirm_2", "再次确认!(%s/%s)", "Confirm again!(%s/%s)");
        addLang("ui.bank.card.manage.delete_confirm_3", "正在删除…(%s/%s)", "Deleting…(%s/%s)");
        addLang("ui.bank.card.manage.none", "无", "None");

        addLang("bank_card_permission.maple_banktrade.owner", "拥有者", "Owner");
        addLang("bank_card_permission.maple_banktrade.admin", "管理员", "Admin");
        addLang("bank_card_permission.maple_banktrade.usable", "可使用", "Usable");
        addLang("bank_card_permission.maple_banktrade.unusable", "不可使用", "Unusable");

        addLang("currency.maple_banktrade.trade_price", "交易价格: %s %s", "Trade price: %s %s");
        addLang("currency.maple_banktrade.trade_buy_only", "仅购买", "Buy only");

        addLang("trade.maple_banktrade.fail.no_card", "无法访问银行卡。", "Cannot access bank card.");
        addLang("trade.maple_banktrade.fail.no_player", "需要玩家才能购买。", "A player is required to buy.");
        addLang("trade.maple_banktrade.fail.unknown_trade", "未知交易项。", "Unknown trade entry.");
        addLang("trade.maple_banktrade.fail.invalid_amount", "交易数量无效。", "Invalid trade amount.");
        addLang("trade.maple_banktrade.fail.not_enough_currency", "余额不足。", "Not enough currency.");
        addLang("trade.maple_banktrade.fail.inventory_full", "背包空间不足。", "Not enough inventory space.");
        addLang("trade.maple_banktrade.fail.not_sellable", "该物品不可出售。", "This item cannot be sold.");
        addLang("trade.maple_banktrade.fail.not_enough_items", "物品数量不足。", "Not enough items.");
        addLang("trade.maple_banktrade.fail.currency_insert", "无法存入货币。", "Failed to deposit currency.");
        addLang("trade.maple_banktrade.fail.item_extract", "无法取出物品。", "Failed to extract items.");
        addLang("trade.maple_banktrade.fail.machine_not_visible", "交易不可见，已阻断交易", "The transaction is not visible, the transaction has been blocked.");
        addLang("trade.maple_banktrade.fail.machine_check_failed", "交易前检查返回失败，不满足交易条件", "The pre-trade check returned a failure because the trading conditions were not met.");
        addLang("trade.maple_banktrade.fail.machine_insufficient", "资源不足，无法执行交易。", "Insufficient resources to run this trade.");
        addLang("trade.maple_banktrade.fail.machine_transfer", "交易资源转移失败。", "Failed to transfer trade resources.");

        // 银行权限卡 UI
        addLang("ui.maple_banktrade.perm_card.title", "权限卡编辑", "Permission Card Editor");
        addLang("ui.maple_banktrade.perm_card.manageable_list", "可管理的卡", "Manageable Cards");
        addLang("ui.maple_banktrade.perm_card.selected_list", "已选卡", "Selected Cards");
        addLang("ui.maple_banktrade.perm_card.empty_manageable", "没有可管理的银行卡", "No manageable bank cards");
        addLang("ui.maple_banktrade.perm_card.empty_selected", "尚未选择", "None selected");
        addLang("ui.maple_banktrade.perm_card.add", "添加", "Add");
        addLang("ui.maple_banktrade.perm_card.remove", "移除", "Remove");
        addLang("ui.maple_banktrade.perm_card.clear", "全部清除", "Clear All");
        addLang("ui.maple_banktrade.perm_card.export", "输出物品", "Export Item");
        addLang("ui.maple_banktrade.perm_card.unknown_card", "未知卡", "Unknown Card");
        addLang("ui.maple_banktrade.perm_card.unknown_bank", "未知银行", "Unknown Bank");
        addLang("ui.maple_banktrade.perm_card.tip.bank", "银行: %s", "Bank: %s");
        addLang("ui.maple_banktrade.perm_card.tip.card", "卡: %s", "Card: %s");
        addLang("ui.maple_banktrade.perm_card.tip.perm", "权限: %s", "Permission: %s");

        // 银行权限卡物品 Tooltip（ItemAttachment.collectTooltipNodes）
        addLang("tooltip.maple_banktrade.perm_card.count", "授权卡: %s 张", "Authorized cards: %s");
        addLang("tooltip.maple_banktrade.perm_card.empty", "（未写入任何卡）", "(No cards stored)");
        addLang("tooltip.maple_banktrade.perm_card.entry", " - %s · %s (%s)", " - %s · %s (%s)");
        addLang("tooltip.maple_banktrade.perm_card.entry_unknown", " - %s", " - %s");

        // 银行卡认证密钥 Tooltip / 使用反馈
        addLang("item.maple_banktrade.bank_card_authentication_key.named",
                "%s认证密钥",
                "%s Authentication Key");
        addLang("tooltip.maple_banktrade.auth_key.creates",
                "创建银行卡: %s",
                "Creates bank card: %s");
        addLang("tooltip.maple_banktrade.auth_key.no_card",
                "未指定银行卡",
                "No bank card selected");
        addLang("message.maple_banktrade.auth_key.missing_card",
                "此认证密钥未绑定银行卡。",
                "This authentication key is not bound to a bank card.");
        addLang("message.maple_banktrade.auth_key.unknown_card",
                "未知银行卡: %s",
                "Unknown bank card: %s");
        addLang("message.maple_banktrade.auth_key.already_owned",
                "你已经拥有 %s。",
                "You already own %s.");
        addLang("message.maple_banktrade.auth_key.create_failed",
                "无法创建 %s。",
                "Failed to create %s.");
        addLang("message.maple_banktrade.auth_key.created",
                "已创建 %s。",
                "Created %s.");

        // 权限卡右键贸易站（BaseTradingStationBlockEntity）
        addLang("message.maple_banktrade.perm_card.apply.empty",
                "权限卡为空，未写入任何银行卡。",
                "Permission card is empty; no bank cards to apply.");
        addLang("message.maple_banktrade.perm_card.apply.success",
                "已绑定 %s 张新卡（跳过已有 %s 张），方块现共 %s 张。",
                "Bound %s new card(s) (skipped %s already present); block now has %s.");
        addLang("message.maple_banktrade.perm_card.apply.none_new",
                "卡上 UUID 均已绑定（当前 %s 张）。",
                "All card UUIDs already bound (currently %s).");

        addLang("command.mbt_bank.factories.empty", "当前没有已注册的银行卡创建定义。", "No registered bank card factories.");
        addLang("command.mbt_bank.factories.header", "可创建的银行卡：", "Creatable bank cards:");
        addLang("command.mbt_bank.factories.entry", "- %s | id=%s | bank=%s", "- %s | id=%s | bank=%s");
        addLang("command.mbt_bank.create.unknown_factory", "未知银行卡创建定义：%s", "Unknown bank card factory: %s");
        addLang("command.mbt_bank.create.failed", "银行卡创建失败：%s", "Failed to create bank card: %s");
        addLang("command.mbt_bank.create.success", "已创建银行卡：%s", "Created bank card: %s");
        addLang("command.mbt_bank.card.summary", "%s | bank=%s | type=%s | name=%s | permission=%s", "%s | bank=%s | type=%s | name=%s | permission=%s");
        addLang("command.mbt_bank.list.empty", "你还没有可用的银行卡。使用 /mbt_bank factories 查看可创建类型。", "You do not have any usable bank cards. Use /mbt_bank factories to view creatable types.");
        addLang("command.mbt_bank.list.header", "你的银行卡：", "Your bank cards:");
        addLang("command.mbt_bank.list.entry", "- %s", "- %s");
        addLang("command.mbt_bank.wallet.opened", "已打开钱包。", "Opened wallet.");
        addLang("command.mbt_bank.info.not_found", "银行卡不存在：%s", "Bank card does not exist: %s");
        addLang("command.mbt_bank.info.no_permission", "你没有使用此银行卡的权限：%s", "You do not have permission to use this bank card: %s");
        addLang("command.mbt_bank.info.header", "银行卡信息：", "Bank card info:");
        addLang("command.mbt_bank.info.bank", "银行: %s", "Bank: %s");
        addLang("command.mbt_bank.info.card_type", "卡类型: %s", "Card type: %s");
        addLang("command.mbt_bank.info.name_index", "名称索引: %s", "Name index: %s");
        addLang("command.mbt_bank.info.permission", "权限: %s", "Permission: %s");
        addLang("command.mbt_bank.balance.empty", "余额: 无支持货币", "Balance: no supported currencies");
        addLang("command.mbt_bank.balance.header", "余额:", "Balance:");
        addLang("command.mbt_bank.balance.entry", "- %s: %s", "- %s: %s");

        // 交易站 UI（交易类型名称/描述见 TradableTypeRegistration）
        addLang("ui.maple_banktrade.trading_station.tab.inventory", "库存", "Inventory");
        addLang("ui.maple_banktrade.trading_station.input", "输入", "Input");
        addLang("ui.maple_banktrade.trading_station.output", "输出", "Output");
        addLang("ui.maple_banktrade.trading_station.energy", "FE: %s/%s  (%s)", "FE: %s/%s  (%s)");
        addLang("ui.maple_banktrade.trading_station.no_recipes", "暂无配方", "No recipes");
        addLang("ui.maple_banktrade.trading_station.bound_cards.title", "绑定银行卡 (%s)", "Bound bank cards (%s)");
        addLang("ui.maple_banktrade.trading_station.bound_cards.empty",
                "（尚未绑定。用手持权限卡右键本站写入）",
                "(None bound. Right-click this station with a permission card.)");
        addLang("ui.maple_banktrade.trading_station.bound_cards.entry",
                "· %s · %s (%s)",
                "· %s · %s (%s)");
        addLang("ui.maple_banktrade.trading_station.bound_cards.entry_unknown",
                "· %s",
                "· %s");
        addLang("ui.maple_banktrade.trading_station.bound_cards.unbind", "解绑", "Unbind");
        addLang("ui.maple_banktrade.trading_station.auto_trade", "自动交易", "Auto-Trade");
        addLang("ui.maple_banktrade.trading_station.auto_trade.tooltip",
                "开启后按输入与周期自动出售匹配配方。",
                "When on, matching recipes sell automatically from inputs.");

        // 机器交易配方 Tooltip
        addLang("trade.maple_banktrade.machine.tooltip.invisible", "不满足可见条件", "Visibility condition not met");
        addLang("trade.maple_banktrade.machine.tooltip.inputs", "输入:", "Inputs:");
        addLang("trade.maple_banktrade.machine.tooltip.outputs", "输出:", "Outputs:");
        addLang("trade.maple_banktrade.machine.tooltip.item_entry", " - %sx ", " - %sx ");
        addLang("trade.maple_banktrade.machine.tooltip.fluid_entry", " - %s mB ", " - %s mB ");
        addLang("trade.maple_banktrade.machine.tooltip.energy_extract", " - 消耗 %s FE", " - Extract %s FE");
        addLang("trade.maple_banktrade.machine.tooltip.energy_insert", " - 产出 %s FE", " - Insert %s FE");
        addLang("trade.maple_banktrade.machine.tooltip.currency_entry", " - %s ", " - %s ");

        // 交易符令 Tooltip
        addLang("tooltip.maple_banktrade.transaction_talisman", "可打开 - %s", "Openable - %s");

        // FTBQ 兼容
        // Task type display names (used in quest editor)
        addLang("ftbquests.task.maple_banktrade.tagged", "银行卡标记", "Bank Card Tag");
        addLang("ftbquests.task.maple_banktrade.tagged_progress", "银行卡进度标记", "Bank Card Progress Tag");

        // TaggedTask configuration fields
        addLang("maple_banktrade.task.tagged.name_index", "银行卡", "Bank Card");
        addLang("maple_banktrade.task.tagged.entry_id", "标记条目", "Tag Entry");
        addLang("maple_banktrade.task.tagged.use_progress", "使用进度阈值", "Use Progress Threshold");
        addLang("maple_banktrade.task.tagged.required_progress", "所需进度", "Required Progress");

        // TaggedProgressTask configuration fields
        addLang("maple_banktrade.task.tagged_progress.name_index", "银行卡", "Bank Card");
        addLang("maple_banktrade.task.tagged_progress.mode", "检测模式", "Detection Mode");
        addLang("maple_banktrade.task.tagged_progress.tier", "等级", "Tier");
        addLang("maple_banktrade.task.tagged_progress.threshold", "阈值（数量）", "Threshold (number)");
        addLang("maple_banktrade.task.tagged_progress.percent", "百分比（0~100）", "Percentage (0~100)");
        addLang("maple_banktrade.task.tagged_progress.target_value", "目标值", "Target Value");

        // TaggedProgressTask mode enum display names
        addLang("maple_banktrade.task.tagged_progress.mode.total_count", "完成指定数量条目", "Complete N Entries");
        addLang("maple_banktrade.task.tagged_progress.mode.total_ratio", "完成指定百分比条目", "Complete % of Entries");
        addLang("maple_banktrade.task.tagged_progress.mode.tier_count", "完成指定等级指定数量条目", "Complete N Entries in Tier");
        addLang("maple_banktrade.task.tagged_progress.mode.tier_ratio", "完成指定等级指定百分比条目", "Complete % of Entries in Tier");
        addLang("maple_banktrade.task.tagged_progress.mode.all", "完成所有条目", "Complete All Entries");

        // Reward type display names (used in quest editor)
        addLang("ftbquests.reward.maple_banktrade.tagged_increase", "标记卡增加进度", "Tagged Card Increase Progress");
        addLang("ftbquests.reward.maple_banktrade.tagged_reset", "标记卡重置进度", "Tagged Card Reset Progress");

        // TaggedIncreaseReward configuration fields
        addLang("maple_banktrade.reward.tagged_increase.name_index", "银行卡", "Bank Card");
        addLang("maple_banktrade.reward.tagged_increase.entry_id", "标记条目", "Tag Entry");
        addLang("maple_banktrade.reward.tagged_increase.amount", "增加数量", "Increase Amount");
        addLang("maple_banktrade.reward.tagged_increase.limit", "限制上限", "Limit to Required");

        // TaggedResetReward configuration fields
        addLang("maple_banktrade.reward.tagged_reset.name_index", "银行卡", "Bank Card");
        addLang("maple_banktrade.reward.tagged_reset.entry_id", "标记条目", "Tag Entry");
        addLang("maple_banktrade.reward.tagged_reset.reset", "(重置)", "(Reset)");

        addLang("ftbquests.task.maple_banktrade.currency", "银行卡货币检测", "Bank Card Currency Check");
        addLang("maple_banktrade.task.currency.currency", "货币类型", "Currency Type");
        addLang("maple_banktrade.task.currency.amount", "目标数量", "Target Amount");
        addLang("maple_banktrade.task.currency.consume", "提交时消耗", "Consume on Submit");
        addLang("maple_banktrade.task.currency.consume_suffix", " (消耗)", " (Consume)");
        addLang("maple_banktrade.task.currency.detect_only_suffix", " (仅检测)", " (Detect Only)");

        addLang("ftbquests.reward.maple_banktrade.bank_currency", "银行卡货币奖励", "Bank Card Currency Reward");
        addLang("maple_banktrade.reward.bank_currency.currency", "货币类型", "Currency Type");
        addLang("maple_banktrade.reward.bank_currency.amount", "数量", "Amount");
    }
}

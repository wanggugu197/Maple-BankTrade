package com.maple.maple_banktrade.bank.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardPermission;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration;
import com.maple.maple_banktrade.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 银行系统玩家自助命令（/mbt_bank）。
 */
public final class MBTBankCommands {

    // ==============================================
    // 注册
    // ==============================================

    private static boolean listenerRegistered;

    /** 注册命令事件监听。 */
    public static void init() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        NeoForge.EVENT_BUS.addListener(MBTBankCommands::register);
    }

    /** 注册 /mbt_bank 命令树。 */
    private static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mbt_bank")
                .requires(CommandSourceStack::isPlayer)
                .executes(MBTBankCommands::listCards)
                .then(Commands.literal("factories")
                        .executes(MBTBankCommands::listFactories))
                .then(Commands.literal("create")
                        .then(Commands.argument("factory", IdentifierArgument.id())
                                .suggests((_, builder) -> SharedSuggestionProvider.suggestResource(
                                        BankCardFactory.values().stream().map(BankCardFactory::nameIndex),
                                        builder))
                                .executes(MBTBankCommands::createCard)))
                .then(Commands.literal("list")
                        .executes(MBTBankCommands::listCards))
                .then(Commands.literal("wallet")
                        .executes(MBTBankCommands::openWallet))
                .then(Commands.literal("info")
                        .then(Commands.argument("cardUuid", UuidArgument.uuid())
                                .suggests(MBTBankCommands::suggestOwnedCards)
                                .executes(MBTBankCommands::showCardInfo))));
    }

    // ==============================================
    // 命令实现
    // ==============================================

    /** 列出可用银行卡创建工厂。 */
    private static int listFactories(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (BankCardFactory.values().isEmpty()) {
            source.sendFailure(Component.translatable("command.mbt_bank.factories.empty"));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.factories.header"));
        BankCardFactory.values().forEach(factory -> source.sendSystemMessage(Component.translatable(
                "command.mbt_bank.factories.entry", nameIndexName(factory), formatArg(factory.nameIndex()), formatArg(factory.bankType().id()))));
        return BankCardFactory.values().size();
    }

    /** 为当前玩家创建银行卡。 */
    private static int createCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Identifier nameIndex = IdentifierArgument.getId(context, "factory");
        BankCardFactory factory = BankCardFactory.requireByNameIndex(nameIndex);
        if (factory == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.create.unknown_factory", formatArg(nameIndex)));
            return 0;
        }

        BankCard[] created = new BankCard[1];
        MBTBankStates.modifyBankCards(source.getServer(), data -> created[0] = data.createCard(factory, player.getUUID()));
        if (created[0] == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.create.failed", formatArg(nameIndex)));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.create.success", formatArg(created[0].getCardUuid())));
        source.sendSystemMessage(formatCardSummary(created[0], BankCardPermission.OWNER));
        return 1;
    }

    /** 列出当前玩家可用银行卡。 */
    private static int listCards(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        BankCardsWorldData data = MBTBankStates.getBankCards(source.getServer());
        Map<UUID, BankCardPermission> permissions = data.getPermissionsForPlayer(player.getUUID());
        Map<UUID, BankCardPermission> usablePermissions = permissions.entrySet().stream()
                .filter(entry -> isUsable(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (usablePermissions.isEmpty()) {
            source.sendSystemMessage(Component.translatable("command.mbt_bank.list.empty"));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.list.header"));
        usablePermissions.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> {
                    BankCard card = data.getCard(entry.getKey());
                    if (card != null) {
                        source.sendSystemMessage(Component.translatable("command.mbt_bank.list.entry", formatCardSummary(card, entry.getValue())));
                    }
                });
        return usablePermissions.size();
    }

    /** 打开钱包 UI。 */
    private static int openWallet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerUIMenuType.openUI(player, WalletUIRegistration.WALLET_UI);
        return 1;
    }

    /** 显示指定银行卡详情。 */
    private static int showCardInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        UUID cardUuid = UuidArgument.getUuid(context, "cardUuid");
        BankCardsWorldData data = MBTBankStates.getBankCards(source.getServer());
        BankCard card = data.getCard(cardUuid);
        if (card == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.info.not_found", formatArg(cardUuid)));
            return 0;
        }
        BankCardPermission permission = data.getPermission(player.getUUID(), cardUuid);
        if (!isUsable(permission)) {
            source.sendFailure(Component.translatable("command.mbt_bank.info.no_permission", formatArg(cardUuid)));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.header"));
        source.sendSystemMessage(Component.translatable("ui.bank.card.detail.uuid", formatArg(card.getCardUuid())));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.bank", formatArg(card.getBankTypeId())));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.card_type", formatArg(card.getCardTypeId())));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.name_index", nameIndexName(card)));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.permission", permissionName(permission)));
        sendCardContent(source, card);
        return 1;
    }

    // ==============================================
    // 输出 / 工具
    // ==============================================

    /** 输出卡余额或标签等具体内容。 */
    private static void sendCardContent(CommandSourceStack source, BankCard card) {
        if (card instanceof CurrencyStorageBankCard currencyCard) {
            Set<Identifier> currencyIds = currencyCard.getSupportedCurrencyIds();
            if (currencyIds.isEmpty()) {
                source.sendSystemMessage(Component.translatable("command.mbt_bank.balance.empty"));
            } else {
                source.sendSystemMessage(Component.translatable("command.mbt_bank.balance.header"));
                currencyIds.stream()
                        .sorted(Comparator.comparing(Identifier::toString))
                        .forEach(currencyId -> source.sendSystemMessage(Component.translatable(
                                "command.mbt_bank.balance.entry", formatArg(currencyId), currencyCard.getCurrencyBalanceAsString(currencyId))));
            }
        }

        if (card instanceof TaggedBankCard taggedCard) {
            if (taggedCard.getTags().isEmpty()) {
                source.sendSystemMessage(Component.translatable("ui.bank.card.detail.tags.empty"));
            } else {
                String tags = taggedCard.getTags().stream()
                        .map(Identifier::toString)
                        .sorted()
                        .collect(Collectors.joining(", "));
                source.sendSystemMessage(Component.translatable("ui.bank.card.detail.tags", tags));
            }
        }
    }

    /** 格式化卡摘要行。 */
    private static Component formatCardSummary(BankCard card, BankCardPermission permission) {
        return Component.translatable("command.mbt_bank.card.summary",
                formatArg(card.getCardUuid()), formatArg(card.getBankTypeId()), formatArg(card.getCardTypeId()),
                nameIndexName(card), permissionName(permission));
    }

    /** 将参数转为显示字符串。 */
    private static String formatArg(Object value) {
        return String.valueOf(value);
    }

    /** 获取银行卡名称索引翻译。 */
    private static Component nameIndexName(BankCard card) {
        return Component.translatable(BankCardFactory.getTranslationKey(card.getNameIndex()));
    }

    /** 获取工厂名称索引翻译。 */
    private static Component nameIndexName(BankCardFactory factory) {
        return Component.translatable(BankCardFactory.getTranslationKey(factory.nameIndex()));
    }

    /** 获取权限翻译。 */
    private static Component permissionName(BankCardPermission permission) {
        return Component.translatable(permission.getTranslationKey());
    }

    /** 判断权限是否可用。 */
    private static boolean isUsable(BankCardPermission permission) {
        return permission != null && permission.canUse(); // OWNER / ADMIN / USABLE
    }

    /** 为 info 补全当前玩家可用卡 UUID。 */
    private static CompletableFuture<Suggestions> suggestOwnedCards(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BankCardsWorldData data = MBTBankStates.getBankCards(context.getSource().getServer());
        return SharedSuggestionProvider.suggest(data.getPermissionsForPlayer(player.getUUID()).entrySet().stream()
                .filter(entry -> isUsable(entry.getValue()))
                .map(entry -> entry.getKey().toString()), builder);
    }

    private MBTBankCommands() {}
}

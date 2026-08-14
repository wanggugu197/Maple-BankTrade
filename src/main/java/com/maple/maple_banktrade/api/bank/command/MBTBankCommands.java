package com.maple.maple_banktrade.api.bank.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardPermission;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.capability.CurrencyStorageBankCard;
import com.maple.maple_banktrade.api.bank.ui.BankPermissionsCardUIRegistration;
import com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration;
import com.maple.maple_banktrade.bank.cards.TaggedBankCard;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 银行系统命令 {@code /mbt_bank}。
 * <ul>
 * <li>{@code /mbt_bank} / {@code list} — 列出可用卡</li>
 * <li>{@code factories} — 可创建工厂</li>
 * <li>{@code create <factory>} — 创建卡（需 OWNERS / 权限等级 4）</li>
 * <li>{@code wallet} — 打开钱包 UI</li>
 * <li>{@code perm_builder} — 打开权限卡构造器 UI</li>
 * <li>{@code info <uuid>} — 卡详情</li>
 * </ul>
 */
public final class MBTBankCommands {

    private static boolean listenerRegistered;

    private MBTBankCommands() {}

    /** 注册命令事件监听（幂等）。 */
    public static void init() {
        if (listenerRegistered) return;
        listenerRegistered = true;
        NeoForge.EVENT_BUS.addListener(MBTBankCommands::register);
    }

    private static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("mbt_bank")
                .requires(CommandSourceStack::isPlayer)
                .executes(MBTBankCommands::listCards)
                .then(Commands.literal("list").executes(MBTBankCommands::listCards))
                .then(Commands.literal("factories").executes(MBTBankCommands::listFactories))
                .then(Commands.literal("create")
                        // MC 26：等级 4 = OWNERS（旧 hasPermission(4)）
                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                        .then(Commands.argument("factory", IdentifierArgument.id())
                                .suggests((_, b) -> SharedSuggestionProvider.suggestResource(
                                        BankCardFactory.values().stream().map(BankCardFactory::nameIndex), b))
                                .executes(MBTBankCommands::createCard)))
                .then(Commands.literal("wallet").executes(MBTBankCommands::openWallet))
                .then(Commands.literal("perm_builder").executes(MBTBankCommands::openPermBuilder))
                .then(Commands.literal("info")
                        .then(Commands.argument("cardUuid", UuidArgument.uuid())
                                .suggests(MBTBankCommands::suggestUsableCards)
                                .executes(MBTBankCommands::showCardInfo)));
    }

    // ==============================================
    // 命令
    // ==============================================

    private static int listFactories(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var factories = BankCardFactory.values();
        if (factories.isEmpty()) {
            source.sendFailure(Component.translatable("command.mbt_bank.factories.empty"));
            return 0;
        }
        source.sendSystemMessage(Component.translatable("command.mbt_bank.factories.header"));
        for (BankCardFactory factory : factories) {
            source.sendSystemMessage(Component.translatable(
                    "command.mbt_bank.factories.entry",
                    nameOf(factory.nameIndex()),
                    factory.nameIndex().toString(),
                    factory.bankType().id().toString()));
        }
        return factories.size();
    }

    private static int createCard(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Identifier nameIndex = IdentifierArgument.getId(ctx, "factory");
        BankCardFactory factory = BankCardFactory.requireByNameIndex(nameIndex);
        if (factory == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.create.unknown_factory", nameIndex.toString()));
            return 0;
        }

        BankCard[] created = { null };
        MBTBankStates.modifyBankCards(source.getServer(), data -> created[0] = data.createCard(factory, BankHelper.getUuid(player)));
        if (created[0] == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.create.failed", nameIndex.toString()));
            return 0;
        }

        source.sendSystemMessage(Component.translatable(
                "command.mbt_bank.create.success", created[0].getCardUuid().toString()));
        source.sendSystemMessage(cardSummary(created[0], BankCardPermission.OWNER));
        return 1;
    }

    private static int listCards(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        BankCardsWorldData data = MBTBankStates.getBankCards(source.getServer());
        List<BankCard> cards = data.getUsableCardsForPlayer(BankHelper.getUuid(player));
        if (cards.isEmpty()) {
            source.sendSystemMessage(Component.translatable("command.mbt_bank.list.empty"));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.list.header"));
        cards.stream()
                .sorted(Comparator.comparing(c -> c.getCardUuid().toString()))
                .forEach(card -> {
                    BankCardPermission perm = data.getPermission(BankHelper.getUuid(player), card.getCardUuid());
                    source.sendSystemMessage(Component.translatable(
                            "command.mbt_bank.list.entry", cardSummary(card, perm)));
                });
        return cards.size();
    }

    private static int openWallet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return openUi(ctx, WalletUIRegistration.WALLET_UI);
    }

    /** 打开权限卡构造器 UI。 */
    private static int openPermBuilder(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return openUi(ctx, BankPermissionsCardUIRegistration.BANK_PERMISSIONS_CARD_UI);
    }

    private static int openUi(CommandContext<CommandSourceStack> ctx, Identifier uiId) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerUIMenuType.openUI(player, uiId);
        return 1;
    }

    private static int showCardInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        UUID cardUuid = UuidArgument.getUuid(ctx, "cardUuid");
        BankCardsWorldData data = MBTBankStates.getBankCards(source.getServer());
        BankCard card = data.getCard(cardUuid);
        if (card == null) {
            source.sendFailure(Component.translatable("command.mbt_bank.info.not_found", cardUuid.toString()));
            return 0;
        }
        BankCardPermission permission = data.getPermission(BankHelper.getUuid(player), cardUuid);
        if (permission == null || !permission.canUse()) {
            source.sendFailure(Component.translatable("command.mbt_bank.info.no_permission", cardUuid.toString()));
            return 0;
        }

        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.header"));
        source.sendSystemMessage(Component.translatable("ui.bank.card.detail.uuid", card.getCardUuid().toString()));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.bank", card.getBankTypeId().toString()));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.card_type", card.getCardTypeId().toString()));
        source.sendSystemMessage(Component.translatable("command.mbt_bank.info.name_index", nameOf(card.getNameIndex())));
        source.sendSystemMessage(Component.translatable(
                "command.mbt_bank.info.permission", Component.translatable(permission.getTranslationKey())));
        source.sendSystemMessage(Component.nullToEmpty(PersistedParser.serializeNBT(card, source.getServer().registryAccess()).toString()));
        return 1;
    }

    // ==============================================
    // 工具
    // ==============================================

    private static Component cardSummary(BankCard card, BankCardPermission permission) {
        return Component.translatable(
                "command.mbt_bank.card.summary",
                card.getCardUuid().toString(),
                card.getBankTypeId().toString(),
                card.getCardTypeId().toString(),
                nameOf(card.getNameIndex()),
                Component.translatable(permission.getTranslationKey()));
    }

    private static Component nameOf(Identifier nameIndex) {
        return Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(nameIndex));
    }

    private static CompletableFuture<Suggestions> suggestUsableCards(
                                                                     CommandContext<CommandSourceStack> ctx,
                                                                     SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BankCardsWorldData data = MBTBankStates.getBankCards(ctx.getSource().getServer());
        return SharedSuggestionProvider.suggest(
                data.getUsableCardsForPlayer(BankHelper.getUuid(player)).stream()
                        .map(c -> c.getCardUuid().toString()),
                builder);
    }
}

package com.maple.maple_banktrade.api.bank.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.composite.ItemAttachment;
import com.gto.registrylib.tooltip.SubNode;
import com.gto.registrylib.tooltip.TooltipNodeCollector;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.base.BankType;
import com.maple.maple_banktrade.api.bank.data.BankInfo;
import com.maple.maple_banktrade.api.machineTrade.station.BaseTradingStationBlockEntity;

import java.util.Set;
import java.util.UUID;

/**
 * 银行权限卡附件。
 * <p>
 * Tooltip：展示 {@link BankDataComponent#CARD_PERMISSIONS}。<br>
 * 右键贸易站：若 BE 为 {@link BaseTradingStationBlockEntity}，将卡上 UUID 全部并入该站。
 * </p>
 */
public class BankPermissionsCardAttachment extends ItemAttachment<ComponentItem> {

    public BankPermissionsCardAttachment() {}

    @Override
    public InteractionResult useOn(ComponentItem item, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(context.getClickedPos());
            return be instanceof BaseTradingStationBlockEntity ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BaseTradingStationBlockEntity station)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Set<UUID> fromCard = BankDataComponent.CARD_PERMISSIONS.getOrDefault(stack, Set.of());
        if (fromCard == null || fromCard.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.maple_banktrade.perm_card.apply.empty"));
            }
            return InteractionResult.FAIL;
        }

        int before = station.getCardUuids().size();
        int added = station.addCardUuids(fromCard);
        int total = station.getCardUuids().size();
        int skipped = fromCard.size() - added;

        if (player instanceof ServerPlayer serverPlayer) {
            if (added > 0) {
                serverPlayer.sendSystemMessage(
                        Component.translatable(
                                "message.maple_banktrade.perm_card.apply.success",
                                added, skipped, total));
            } else {
                serverPlayer.sendSystemMessage(
                        Component.translatable(
                                "message.maple_banktrade.perm_card.apply.none_new",
                                before));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void collectTooltipNodes(ComponentItem item, ItemStack stack, TooltipNodeCollector collector) {
        Set<UUID> cards = BankDataComponent.CARD_PERMISSIONS.getOrDefault(stack, Set.of());
        if (cards == null) cards = Set.of();

        int priority = 0;
        collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.perm_card.count", cards.size()), priority++));

        if (cards.isEmpty()) {
            collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.perm_card.empty"), priority));
            return;
        }

        BankCardsWorldData data = tryWorldData();
        for (UUID uuid : cards) {
            collector.node(new SubNode.Basic(lineFor(uuid, data), priority++));
        }
    }

    private static Component lineFor(UUID uuid, BankCardsWorldData data) {
        BankCard card = data == null ? null : data.getCard(uuid);
        if (card == null) {
            return Component.translatable("tooltip.maple_banktrade.perm_card.entry_unknown", uuid.toString());
        }
        return Component.translatable("tooltip.maple_banktrade.perm_card.entry", bankName(card), cardName(card), shortUuid(uuid));
    }

    private static Component cardName(BankCard card) {
        return Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(card.getNameIndex()));
    }

    private static Component bankName(BankCard card) {
        BankInfo info = BankInfo.of(BankType.requireById(card.getBankTypeId()));
        return info == null ? Component.literal(card.getBankTypeId().toString()) : Component.translatable(BankInfo.getBankInfoTranslationKey(info.type()));
    }

    private static String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.length() <= 8 ? s : s.substring(0, 8);
    }

    private static BankCardsWorldData tryWorldData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return MBTBankStates.getBankCards(server);
    }
}

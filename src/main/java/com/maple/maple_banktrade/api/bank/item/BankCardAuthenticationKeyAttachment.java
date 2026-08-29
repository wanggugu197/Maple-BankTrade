package com.maple.maple_banktrade.api.bank.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.composite.ItemAttachment;
import com.gto.registrylib.tooltip.SubNode;
import com.gto.registrylib.tooltip.TooltipNodeCollector;
import com.maple.maple_banktrade.api.bank.BankHelper;
import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardFactory;

/**
 * 银行卡认证密钥附件：按 {@link BankDataComponent#CARD_NAME_INDEX} 创建玩家尚未持有的银行卡。
 */
public class BankCardAuthenticationKeyAttachment extends ItemAttachment<ComponentItem> {

    public BankCardAuthenticationKeyAttachment() {}

    @Override
    public InteractionResult use(ComponentItem item, Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ResourceLocation nameIndex = BankDataComponent.CARD_NAME_INDEX.get(stack);
        if (nameIndex == null) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.maple_banktrade.auth_key.missing_card"));
            }
            return InteractionResult.FAIL;
        }

        BankCardFactory factory = BankCardFactory.requireByNameIndex(nameIndex);
        if (factory == null) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.maple_banktrade.auth_key.unknown_card", nameIndex.toString()));
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean alreadyOwned = !MBTBankStates.getBankCards(level.getServer())
                .getUsableCardsForPlayerByNameIndex(BankHelper.getUuid(player), nameIndex)
                .isEmpty();
        if (alreadyOwned) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.maple_banktrade.auth_key.already_owned", cardName(nameIndex)));
            }
            return InteractionResult.FAIL;
        }

        BankCard[] created = { null };
        MBTBankStates.modifyBankCards(level.getServer(), data -> created[0] = data.createCard(factory, BankHelper.getUuid(player)));
        if (created[0] == null) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.maple_banktrade.auth_key.create_failed", cardName(nameIndex)));
            }
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.translatable("message.maple_banktrade.auth_key.created", cardName(nameIndex)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void collectTooltipNodes(ComponentItem item, ItemStack stack, TooltipNodeCollector collector) {
        ResourceLocation nameIndex = BankDataComponent.CARD_NAME_INDEX.get(stack);
        Component cardName = nameIndex == null ? Component.translatable("tooltip.maple_banktrade.auth_key.no_card") : cardName(nameIndex);
        collector.node(new SubNode.Basic(Component.translatable("tooltip.maple_banktrade.auth_key.creates", cardName), 0));
    }

    private static Component cardName(ResourceLocation nameIndex) {
        return Component.translatable(BankCardFactory.getBankCardFactoryTranslationKey(nameIndex));
    }
}

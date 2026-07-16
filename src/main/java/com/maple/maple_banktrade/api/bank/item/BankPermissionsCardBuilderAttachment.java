package com.maple.maple_banktrade.api.bank.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.gto.registrylib.composite.ComponentItem;
import com.gto.registrylib.composite.ItemAttachment;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;

import static com.maple.maple_banktrade.api.bank.ui.BankPermissionsCardUIRegistration.BANK_PERMISSIONS_CARD_UI;

/**
 * 银行权限卡构造器附件：使用时打开编辑 UI，可导出权限卡。
 */
public class BankPermissionsCardBuilderAttachment extends ItemAttachment<ComponentItem> {

    public BankPermissionsCardBuilderAttachment() {}

    @Override
    public InteractionResult useOn(ComponentItem item, UseOnContext context) {
        Player player = context.getPlayer();
        if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerUIMenuType.openUI(serverPlayer, BANK_PERMISSIONS_CARD_UI);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(ComponentItem item, Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerUIMenuType.openUI(serverPlayer, BANK_PERMISSIONS_CARD_UI);
        }
        return InteractionResult.SUCCESS;
    }
}

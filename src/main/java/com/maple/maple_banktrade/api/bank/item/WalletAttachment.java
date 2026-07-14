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

import static com.maple.maple_banktrade.api.bank.ui.WalletUIRegistration.WALLET_UI;

/**
 * 钱包物品附件：使用时打开钱包 UI。
 */
public class WalletAttachment extends ItemAttachment<ComponentItem> {

    // ==============================================
    // 构造
    // ==============================================

    /** 创建钱包物品附件。 */
    public WalletAttachment() {}

    // ==============================================
    // 物品交互
    // ==============================================

    /** 方块上使用时打开钱包 UI。 */
    @Override
    public InteractionResult useOn(ComponentItem item, UseOnContext context) {
        Player player = context.getPlayer();
        if (!context.getLevel().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerUIMenuType.openUI(serverPlayer, WALLET_UI);
        }
        return InteractionResult.SUCCESS;
    }

    /** 空中使用时打开钱包 UI。 */
    @Override
    public InteractionResult use(ComponentItem item, Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerUIMenuType.openUI(serverPlayer, WALLET_UI);
        }
        return InteractionResult.SUCCESS;
    }
}

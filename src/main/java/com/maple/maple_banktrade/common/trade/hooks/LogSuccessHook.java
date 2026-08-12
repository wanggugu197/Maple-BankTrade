package com.maple.maple_banktrade.common.trade.hooks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class LogSuccessHook extends MachineTradeHooks.SuccessHook {

    @Persisted
    private Component logMessage;

    @Override
    public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                             MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {
        MapleBankTrade.LOGGER.info("{}  {}  {}", logMessage.tryCollapseToString(), request.tradeId(), plan.tradeCount());
        if (context.entity() instanceof Player player) {
            player.sendSystemMessage(Component.literal("[Trade] ").append(logMessage));
        }
    }
}

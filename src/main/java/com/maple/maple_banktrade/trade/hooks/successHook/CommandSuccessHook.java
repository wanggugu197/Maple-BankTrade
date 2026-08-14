package com.maple.maple_banktrade.trade.hooks.successHook;

import net.minecraft.server.MinecraftServer;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeDetail;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradePlan;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;

/**
 * 命令回调钩子：交易成功后以服务器控制台权限执行指令 {@link #command}。
 * <p>
 * 注意：以 op（权限等级 4）身份执行，仅应配置可信指令，避免配置注入风险。
 * </p>
 */
public final class CommandSuccessHook extends MachineTradeHooks.SuccessHook {

    @Persisted
    private String command;

    public CommandSuccessHook(String command) {
        this.command = command;
    }

    @Override
    public void afterSuccess(MachineTradeContext context, MachineTradeRequest request,
                             MachineTradePlan plan, TradeExecuteResult<MachineTradeDetail> result) {
        if (command == null || command.isBlank()) {
            return;
        }
        MinecraftServer server = context.server();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }
}

package com.maple.maple_banktrade.trade.hooks.checkHook;

import net.minecraft.core.BlockPos;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 矩形区域钩子：当所在位置在由 {@link #minPos} 和 {@link #maxPos} 定义的轴对齐区域内时返回 true。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class AABBCheckHook extends MachineTradeHooks.CheckHook {

    @Persisted
    private BlockPos minPos;
    @Persisted
    private BlockPos maxPos;

    @Override
    public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return false;

        // 确保 min 和 max 有效
        if (minPos == null || maxPos == null) return false;
        int minX = Math.min(minPos.getX(), maxPos.getX());
        int minY = Math.min(minPos.getY(), maxPos.getY());
        int minZ = Math.min(minPos.getZ(), maxPos.getZ());
        int maxX = Math.max(minPos.getX(), maxPos.getX());
        int maxY = Math.max(minPos.getY(), maxPos.getY());
        int maxZ = Math.max(minPos.getZ(), maxPos.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX &&
                pos.getY() >= minY && pos.getY() <= maxY &&
                pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}

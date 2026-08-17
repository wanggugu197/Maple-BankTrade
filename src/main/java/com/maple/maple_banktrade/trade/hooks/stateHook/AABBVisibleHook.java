package com.maple.maple_banktrade.trade.hooks.stateHook;

import net.minecraft.core.BlockPos;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import static com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.FLAG_VISIBLE;

/**
 * 矩形区域钩子：当所在位置在由 {@link #minPos} 和 {@link #maxPos} 定义的轴对齐区域内时可见。
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class AABBVisibleHook extends MachineTradeHooks.StateHook {

    @Persisted
    private BlockPos minPos;
    @Persisted
    private BlockPos maxPos;

    @Override
    public int getState(MachineTradeContext context, MachineTrade trade) {
        BlockPos pos = context.getPos();
        if (pos == null) return 0;

        // 确保 min 和 max 有效
        if (minPos == null || maxPos == null) return 0;
        int minX = Math.min(minPos.getX(), maxPos.getX());
        int minY = Math.min(minPos.getY(), maxPos.getY());
        int minZ = Math.min(minPos.getZ(), maxPos.getZ());
        int maxX = Math.max(minPos.getX(), maxPos.getX());
        int maxY = Math.max(minPos.getY(), maxPos.getY());
        int maxZ = Math.max(minPos.getZ(), maxPos.getZ());

        boolean inside = pos.getX() >= minX && pos.getX() <= maxX &&
                pos.getY() >= minY && pos.getY() <= maxY &&
                pos.getZ() >= minZ && pos.getZ() <= maxZ;
        return inside ? FLAG_VISIBLE : 0;
    }
}

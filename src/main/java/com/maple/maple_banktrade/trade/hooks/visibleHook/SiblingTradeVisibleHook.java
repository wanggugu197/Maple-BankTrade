package com.maple.maple_banktrade.trade.hooks.visibleHook;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * 关联交易可见性钩子：当本 storage 中另一交易条目 {@link #siblingTradeId} 当前可见时返回 true，
 * 用于构建"完成 A 后才解锁 B"的连锁任务。
 * {@link #flip} 用于反转逻辑；关联条目不存在时返回 {@link #flip}。
 * <p>
 * 注意：请勿让两条交易互相引用形成循环（会无限递归）。
 * </p>
 */
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class SiblingTradeVisibleHook extends MachineTradeHooks.VisibilityHook {

    @Persisted
    private Identifier siblingTradeId;
    @Persisted
    private boolean flip;

    public SiblingTradeVisibleHook(Identifier siblingTradeId) {
        this(siblingTradeId, false);
    }

    @Override
    public boolean isVisible(MachineTradeContext context, MachineTrade trade) {
        MachineTrade sibling = context.storage().require(siblingTradeId);
        if (sibling == null) {
            return flip;
        }
        boolean condition = sibling.visibilityHook().isVisible(context, sibling);
        return flip != condition;
    }
}

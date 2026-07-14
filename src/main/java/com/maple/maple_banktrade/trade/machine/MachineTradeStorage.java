package com.maple.maple_banktrade.trade.machine;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.registry.AbstractTradeEntryStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 机器多资源交易条目存储器。
 */
public final class MachineTradeStorage extends AbstractTradeEntryStorage<MachineTrade> {

    /** 绑定交易类型 ID 创建空存储器。 */
    public MachineTradeStorage(Identifier tradeTypeId) {
        super(tradeTypeId);
    }

    /** 校验条目有效性。 */
    @Override
    protected boolean isValidEntry(MachineTrade entry) {
        return entry != null && entry.isValid();
    }

    /** 在本类型路径下注册条目：typePath/entryPath。 */
    public MachineTrade register(String entryPath, MachineTrade trade) {
        return register(MapleBankTrade.id(tradeTypeId().getPath() + "/" + entryPath), trade);
    }

    /** 返回当前上下文下可见的条目（保持注册顺序）。 */
    public List<Map.Entry<Identifier, MachineTrade>> listVisible(MachineTradeContext context) {
        if (context == null) return List.of();
        List<Map.Entry<Identifier, MachineTrade>> visible = new ArrayList<>();
        for (Map.Entry<Identifier, MachineTrade> entry : entries().entrySet()) {
            MachineTrade trade = entry.getValue();
            if (trade.visibility().isVisible(context, trade)) {
                visible.add(entry);
            }
        }
        return visible;
    }
}

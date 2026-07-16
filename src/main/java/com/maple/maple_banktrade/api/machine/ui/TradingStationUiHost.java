package com.maple.maple_banktrade.api.machine.ui;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.trade.machine.MachineTrade;

import java.util.List;
import java.util.Map;

/**
 * 贸易站 UI 所需的最小宿主契约（方块实体实现）。
 * <p>
 * 只描述「有哪些交易类型 / 列出配方 / 执行配方」，与物品/流体/能量是否齐全无关。
 * </p>
 */
public interface TradingStationUiHost {

    /** 本站绑定的交易类型 ID（决定配方标签页顺序）。 */
    List<Identifier> tradeTypeIds();

    /**
     * UI 用配方列表：优先可见项，可回退到已注册全部条目。
     * 实现可委托 {@code listVisibleTrades} / {@code listRegisteredTrades}。
     */
    List<Map.Entry<Identifier, MachineTrade>> listTradesForUi(Identifier tradeTypeId);

    /**
     * UI 点击配方后在服务端执行。
     *
     * @param desiredCount 由修饰键解码的期望次数（≥1）
     */
    void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount);
}

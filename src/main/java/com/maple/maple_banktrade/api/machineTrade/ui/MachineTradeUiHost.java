package com.maple.maple_banktrade.api.machineTrade.ui;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.machine.MachineTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeContext;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHandler;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeStorage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 贸易站 UI 所需的最小宿主契约（方块实体实现）。
 * <p>
 * 描述交易类型 / 配方列表 / 执行 / 绑定卡解绑，与物品/流体/能量规格无关。
 * </p>
 */
public interface MachineTradeUiHost {

    /** 本站绑定的交易类型 ID（决定配方标签页顺序）。 */
    List<Identifier> tradeTypeIds();

    /**
     * UI 点击配方后在服务端执行。
     *
     * @param desiredCount 由修饰键解码的期望次数（≥1）
     */
    void runTradeFromUi(Identifier tradeTypeId, Identifier tradeId, int desiredCount);

    /** 当前绑定的银行卡 UUID（库存页列表用，顺序尽量与插入顺序一致）。 */
    Set<UUID> getBoundCardUuids();

    /**
     * UI 解绑按钮：从本站移除一张卡的绑定（仅服务端应产生效果）。
     *
     * @return 是否实际移除
     */
    boolean unbindCardFromUi(UUID cardUuid);

    /** 创建运行交易的上下文 */
    MachineTradeContext createTradeContext(Identifier tradeTypeId);

    /**
     * 本站硬件/类型是否支持自动交易（对应 BE 构造时的 allowAutoTrade）。
     * 为 false 时不显示自动交易开关。
     */
    default boolean supportsAutoTrade() {
        return false;
    }

    /** 玩家配置的自动交易开关（仅 {@link #supportsAutoTrade()} 时有意义）。 */
    default boolean isAutoTradeEnabled() {
        return false;
    }

    /** UI Switch 写入配置；仅服务端应改世界状态。 */
    default void setAutoTradeEnabled(boolean enabled) {}

    /** 获取交易存储器 */
    default MachineTradeStorage tradeStorage(Identifier tradeTypeId) {
        return TradeRegistry.requireStorage(tradeTypeId, MachineTradeStorage.class);
    }

    /**
     * UI 用配方可见项列表。
     */
    default List<Map.Entry<Identifier, MachineTrade>> listTradesForUi(Identifier tradeTypeId) {
        MachineTradeContext context = createTradeContext(tradeTypeId);
        if (context != null) return MachineTradeHandler.listVisible(context);
        return List.of();
    }

    /**
     * UI 配方全部列表。
     */
    default Map<Identifier, MachineTrade> listAllTrades(Identifier tradeTypeId) {
        MachineTradeStorage storage = tradeStorage(tradeTypeId);
        if (storage != null) return storage.entries();
        return Map.of();
    }
}

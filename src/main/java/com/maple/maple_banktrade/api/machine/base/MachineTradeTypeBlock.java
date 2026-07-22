package com.maple.maple_banktrade.api.machine.base;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 贸易站方块：提供本站默认/配置的机器交易类型 ID 列表。
 */
public interface MachineTradeTypeBlock {

    /** 非空时优先于 BE 的 fallback 列表。 */
    List<Identifier> machineTradeTypes();
}

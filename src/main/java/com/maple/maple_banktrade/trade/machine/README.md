# machine — 方块实体多资源交易

包：`com.maple.maple_banktrade.trade.machine`

面向方块实体：物品 + 流体 + 能量 + 银行货币。

## 贸易站 BE

- `BaseTradingStationBlockEntity`：统一存储（`TradingStationStorageSpec`）、同步、能力、交易、UI Host
- 子类只指定规格 + `fallbackTradeTypeIds` / `fallbackTradeTypeId`
- 能力：`Combined(output, InsertOnly(input))` 内联于基类；`registerCapabilities(event, type)`

## 流程

```text
BE 组装 MachineTradeContext
  → MachineTradeHandler.run
      → check → execute (Transaction) → afterSuccess
```

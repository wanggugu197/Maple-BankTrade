# machine — 方块实体多资源交易

包：`com.maple.maple_banktrade.api.trade.machine`

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

## 自动交易

| 层 | 含义 |
|----|------|
| `MachineTradeType.allowAutoTrade` | 类型是否允许 auto 条目（默认 false） |
| `MachineTrade.autoTrade` | 条目是否参与自动匹配；Builder 要求物+流输入数量之和为 1 |
| `MachineTradeHandler.autoRun(context)` | 扫 itemInput/fluidInput，按资源匹配并 run |
| `BaseTradingStationBlockEntity.autoRunTrades()` | 站上便捷调用 |

类型未开放时注册 `autoTrade=true` 条目会失败；条目未标 auto 不会被扫描。

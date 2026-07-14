# machine — 方块实体多资源交易

包：`com.maple.maple_banktrade.trade.machine`

面向方块实体的复杂交易：物品 + 流体 + NeoForge 能量 + 银行货币，并支持每条交易的可见性 / check / afterSuccess 钩子。

## 类职责

| 类 | 职责 |
|----|------|
| `MachineTrade` | 条目：单次配方 I/O + 可选 `machineTradeIcon` / `description` + 三个钩子（builder） |
| `MachineTradeIO` | `ItemIO` / `FluidIO` / `CurrencyIO` / `ScaledIO` |
| `MachineTradeHooks` | 可见性 / extraCheck / afterSuccess 接口与默认值 |
| `MachineTradeType` | 交易类型；`register()` 挂到 `TradeRegistry` |
| `MachineTradeStorage` | 按 tradeId 存条目；`listVisible` |
| `MachineTradeContext` | BE handlers、能量、银行卡 Set、storage（**无 Entity**） |
| `MachineTradeRequest` | tradeId + **desiredCount**（期望次数） |
| `MachineTradePlan` | desiredCount + **tradeCount**（实际）+ 放大 I/O |
| `MachineTradeDefinition` | check 降级次数；execute 提交；afterSuccess 调钩子 |
| `MachineTradeHandler` | `run` / `check` / `listVisible` |
| `MultiCardCurrencyHelper` | 多卡精确扣/加货币 |
| `MachineTradeDetail` | 执行明细 |

## 流程

```text
BE 组装 Context + desiredCount
  → Handler.run
      → check：查表 → extraCheck 硬拒 → 二分 maxFeasible ≤ desired
      → plan.tradeCount = actual（可 < desired）
      → execute：Transaction 按 plan 转移
      → afterSuccess：条目钩子
```

## 上下文字段

- `itemInput` / `itemOutput`：`ItemStacksResourceHandler`
- `fluidInput` / `fluidOutput`：`FluidStacksResourceHandler`
- `energy`：`EnergyHandler`（消耗 extract、产出 insert）
- `bankCards`：`Set<BankCard>`（建议 `LinkedHashSet` 控制扣款顺序）
- `blockEntity` / `level` / `server` / `storage`

## 注册示例

```java
MachineTradeStorage storage = MachineTradeType.of("my_machine").register();
storage.register("smelt_ore", MachineTrade.builder()
    .addItemInput(MachineTradeIO.ItemIO.of(Items.RAW_IRON, 1))
    .addItemOutput(MachineTradeIO.ItemIO.of(Items.IRON_INGOT, 1))
    .energyExtract(200)
    .build());
```

## 依赖

- `api.trade`：协议与 `TradeRunner`
- `bank.resource`：货币 ResourceHandler
- NeoForge transfer：item / fluid / energy + `Transaction`

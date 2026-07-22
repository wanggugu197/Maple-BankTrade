# currency_item — 货币-物品交易核心

包：`com.maple.maple_banktrade.api.trade.currency_item`

容器与运行时实现，**不含**内置价目表（价目见 `trade.registration`）。

## 类职责

| 类 | 职责 |
|----|------|
| `CurrencyItemTrade` | 条目：物品、货币、单价、买卖模式 |
| `CurrencyItemTradeType` | 交易类型；`register()` 向 `TradeRegistry` 挂空存储器 |
| `CurrencyItemTradeStorage` | 按 tradeId 存条目；支持按物品查可卖项 |
| `CurrencyItemTradeContext` | 玩家 / 服务器 / 卡 UUID / storage |
| `CurrencyItemTradeRequest` | buy(tradeId, count) / sell(handler, slot, count) |
| `CurrencyItemTradePlan` | 计划：买卖方向、数量、金额 |
| `CurrencyItemTradeDefinition` | check 仅查表+算量；execute 事务提交并校验 |
| `CurrencyItemTradeHandler` | 对外入口（`TradeRunner`） |
| `CurrencyItemTradeDetail` | 执行明细 |

## 流程

```text
Handler.buy / sell / sellAll
  → TradeRunner.run(Definition)
      → check：解析价目 + 算量 → plan（无余额/槽位预检）
      → execute：Transaction 内扣加；不足则回滚
      → afterSuccess：空
```

依赖：

- `api.trade`：协议与 Runner
- `api.bank`：`MBTBankStates` 取卡
- `bank.resource`：`CurrencyResource` / `BankCurrencyResourceHandler` 扣加货币

## 与 UI

钱包 `TradableUI`：

- 左键价目购买（Shift/Ctrl/Alt 调整批量 4/8/64）
- 卖出槽 `onChanged` 触发 `sellAll`
- 仅校验权限与可交易卡；**不展示** result 中的失败 messages

## 价目注册

内置类型与条目 → `trade.registration.CurrencyItemTradeRegistration`  
显示名 / 背景 → `bank.registration.TradableTypeRegistration`（同一 type id）

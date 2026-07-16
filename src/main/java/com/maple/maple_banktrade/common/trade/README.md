# trade.registration

包：`com.maple.maple_banktrade.common.trade`

只负责**交易项目**注册：类型存储器 + 价目条目。

## CurrencyItemTradeRegistration

- 声明内置 `CurrencyItemTradeType`：
  - `STONE_AND_ORES` → `maple_banktrade:trade_type/stone_and_ores`
  - `PLANTS_AND_FOOD` → `maple_banktrade:trade_type/plants_and_food`
  - `MOB_DROPS` → `maple_banktrade:trade_type/mob_drops`
- `init()`：`type.register()` 后按条目写入 `CurrencyItemTradeStorage`
- 计价货币使用 `CurrencyRegistration.COINS` 的 id
- 由 `CommonInit.contentInit()` 在 `enableModContent && enableBuiltInTrades` 时调用

## MachineTradeRegistration

- 声明内置 `MachineTradeType`：
  - `MACHINE_BENCH` → `maple_banktrade:trade_type/machine_bench`
- `init()`：注册类型并向 `MachineTradeStorage` 写入示例条目（冶炼、水洗、熔岩充能、铸币、采购、精炼等）
- 涉及货币的条目使用 `CurrencyRegistration.COINS`
- 同样由 `CommonInit.contentInit()` 在内容+价目开关均为 true 时调用
- 条目 ID 形如 `maple_banktrade:trade_type/machine_bench/<path>`

## 与 bank 的分工

| 侧 | 负责 |
|----|------|
| 本包 | 价目条目与存储器 |
| `TradableTypeRegistration` | 名称、描述、panel 背景（currency_item 展示用） |
| `CardRegistration` | 可交易卡工厂绑定 trade type id |

currency_item 的显示 ID 必须与 trade 侧 type id 一致。机器交易由方块实体侧组装 `MachineTradeContext` 调用。

## 配置开关

| 配置 | 效果 |
|------|------|
| `enableModContent: false` | 不调用 `contentInit()`：无内置银行/卡/货币/交易站，也无本包价目（仅 API） |
| `enableBuiltInTrades: false`（内容仍开启） | 不调用上述 `init()`：`TradeRegistry` 中无内置条目；银行/卡仍可注册 |

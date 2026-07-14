# 枫糖银贸 / Maple Banktrade

银行、银行卡、钱包与货币-物品交易。

| 项 | 值 |
|----|-----|
| 中文名 | 枫糖银贸 |
| 英文名 | Maple Banktrade |
| mod_id | `maple_banktrade` |
| 包名 | `com.maple.maple_banktrade` |
| MC / NeoForge | 26.1.2 |

## 模块结构

```text
com.maple.maple_banktrade
├── MapleBankTrade.java          # 模组入口
├── client/                      # 客户端
├── common/                      # CommonInit 启动装配、创造页
├── config/                      # YAML 配置
├── data/lang/                   # 双语语言注册（datagen）
├── api/
│   ├── bank/                    # 银行卡模型、世界数据、钱包 UI API
│   └── trade/                   # 三段式交易协议与 TradeRegistry
├── bank/                        # 银行实现、内置内容、卡 UI、命令
└── trade/
    ├── currency_item/           # 货币-物品交易运行时
    └── registration/            # 内置价目表
```

详细文档：

| 文档 | 内容 |
|------|------|
| [`api/trade/README.md`](src/main/java/com/maple/maple_banktrade/api/trade/README.md) | 交易框架协议 |
| [`bank/README.md`](src/main/java/com/maple/maple_banktrade/bank/README.md) | 银行 / 卡 / 存档 / 扩展 |
| [`trade/currency_item/README.md`](src/main/java/com/maple/maple_banktrade/trade/currency_item/README.md) | 货币-物品交易实现 |
| [`trade/registration/README.md`](src/main/java/com/maple/maple_banktrade/trade/registration/README.md) | 内置价目注册 |

## 启动顺序

### NeoForge 总线（简要）

| 总线 | 来源 | 典型用途 |
|------|------|----------|
| **Mod 事件总线** | `@Mod` 构造器注入的 `IEventBus` | DeferredRegister、`RegisterEvent`、`FMLCommonSetupEvent` |
| **游戏事件总线** | `NeoForge.EVENT_BUS` | 命令、玩家/世界等游玩期事件 |

### 注册时机注意

| 阶段 | 可以做 | 不可以做 |
|------|--------|----------|
| 构造期 | DeferredRegister 挂总线、静态 ItemEntry 登记 | 依赖已写入注册表的逻辑 |
| `FMLCommonSetupEvent` | 注册表已冻结；自定义银行/价目、命令监听 | **构造 `ItemStack` / `ItemStackTexture(Items.*)`**（`Holder.components()` 可能未绑定 → NPE） |
| 进世界 / 打开 UI | `getDefaultInstance()`、渲染物品图标 | — |

物品图标请用 `SpriteTexture`（贴图路径）；交易条目存 `Item` + 延迟 `Supplier&lt;ItemStack&gt;`。

### 本模组装配

`MapleBankTrade` → `CommonInit.init(modBus)`：

**构造期**

1. 配置 `MBTModConfig`、页签、语言
2. 菜单 DeferredRegister + 钱包/卡详情 UI 登记

**`FMLCommonSetupEvent` → `enqueueWork`**

3. `WalletRegistration`（货币 / TradableType / 银行 / 卡，图标用 SpriteTexture）
4. `MBTBankStates`、可选命令监听
5. 可选内置价目（`Items.*` 仅作引用，不立刻 `getDefaultInstance`）

## 玩家流程

1. 创造页取**钱包**，或 `/mbt_bank wallet` 打开钱包 UI。
2. `/mbt_bank factories` 查看可创建卡 → `/mbt_bank create <factory>` 开卡。
3. 在钱包中打开卡详情：可交易卡可点击价目购买，或把物品放入卖出槽出售。
4. 新卡余额为 **0**；内置经济下主要靠**出售物品**入账，再用余额购买。

买卖失败时事务回滚，当前 UI **不会**额外弹失败提示。

## 配置

路径：`config/maple_banktrade/maple_banktrade.yaml`

| 项 | 默认 | 含义 |
|----|------|------|
| `enableBuiltInTrades` | true | 是否注册内置货币-物品价目 |
| `enableBankCommands` | true | 是否注册 `/mbt_bank` |

关闭内置价目后，可交易卡仍可创建；价目为空时交易面板不显示。

## 命令 `/mbt_bank`

| 命令 | 作用 |
|------|------|
| `/mbt_bank` / `list` | 列出自己可用的卡 |
| `factories` | 列出可创建工厂（`nameIndex`） |
| `create <factory>` | 给自己创建一张卡 |
| `info <cardUuid>` | 卡详情与余额 |
| `wallet` | 打开钱包 UI（无需手持钱包） |

面向自助调试：**不校验 OP**，无余额增减、无授权/删卡子命令。

## 内置价目类型（trade type id）

| ID | 说明 |
|----|------|
| `maple_banktrade:trade_type/stone_and_ores` | 石料与矿石 |
| `maple_banktrade:trade_type/plants_and_food` | 植物与食物 |
| `maple_banktrade:trade_type/mob_drops` | 生物掉落 |

显示包装在 `TradableTypeRegistration`；价目在 `CurrencyItemTradeRegistration`。双方通过相同 Identifier 对齐。

## 开发

```bat
gradlew.bat compileJava
gradlew.bat runClient
gradlew.bat runData
```

`runData` 生成语言等到 `src/generated/resources`。

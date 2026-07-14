# 银行系统说明

| 层次 | 包路径 |
|------|--------|
| API | `com.maple.maple_banktrade.api.bank` |
| 实现 / 内置内容 | `com.maple.maple_banktrade.bank` |

在服务器**全局** SavedData 中保存银行卡与玩家权限，并提供钱包 UI 与货币资源操作。

## 系统目标

- 全服一份银行数据（不按维度拆分）。
- 银行卡有稳定唯一 `cardUuid`。
- `bank_type` / `card_type` / `name_index` 职责分离。
- 序列化由 `card_type` 选择具体 Codec。
- 权限与卡实体分表；删卡时清理关联权限。

## 包结构

```text
api.bank
├─ MBTBankStates.java                 # 全服数据入口
├─ WalletApiRegistration.java         # 钱包物品与 UI 注册
├─ base/
│  ├─ BankCard                        # 卡基类（身份字段）
│  ├─ BankCardType                    # card_type → Codec
│  ├─ BankCardFactory                 # nameIndex → 创建工厂
│  ├─ BankType / BankCardPermission
│  └─ BankCardsWorldData              # SavedData
├─ data/   BankInfo, CardInfo
├─ item/   WalletAttachment
└─ ui/     钱包页、卡详情菜单

bank
├─ capability/   CurrencyStorageBankCard, TradableWalletBankCard
├─ cards/        Single / Large / Multi / Tagged / Tradable 等实现
├─ resource/     CurrencyResource, BankCurrencyResourceHandler, CurrencyHelper
├─ data/         CurrencyType, TradableType（注册表 + 显示）
├─ registration/ 内置银行、货币、卡、TradableType
├─ ui/           卡详情 UI、TradableUI 交易面板
├─ command/      /mbt_bank
└─ WalletRegistration.java            # 内置内容 init
```

## 核心模型

### bank_type / card_type / name_index

| 字段 | 含义 |
|------|------|
| `bank_type` | 业务归属哪家银行 |
| `card_type` | 数据结构 / Codec 子类 |
| `name_index` | 显示名与创建工厂主键（可与 card_type 不同） |

同一 `card_type` 可对应多个 `name_index` 工厂。命令 `create` 使用的是 **nameIndex**，不是 card_type。

### BankCardsWorldData

```text
cards:           cardUuid → BankCard
cardPermissions: playerUuid → (cardUuid → Permission)
```

存档（全局 SavedData）：

```text
<世界>/data/maple_banktrade/bank_cards.dat
```

访问：

```java
MBTBankStates.getBankCards(server);
MBTBankStates.modifyBankCards(server, data -> { /* 修改 */ });
```

`modifyBankCards` 在回调结束后会 `setDirty`。货币资源 handler 在事务 root commit 时也会回调 dirty。

### 权限（四级）

```text
OWNER  > ADMIN > USABLE > UNUSABLE
拥有者   管理员   可使用   不可使用

canUse()    = OWNER | ADMIN | USABLE
canManage() = OWNER | ADMIN
isOwner()   = OWNER
```

创建卡时创建者写入 `OWNER`。  
详情页「管理」面板（`BankCardPermissionPanel`）供拥有者/管理员操作：

| 操作 | 拥有者 | 管理员 |
|------|--------|--------|
| 授予 ADMIN / USABLE | ✓（二选一） | 仅 USABLE |
| 撤销权限 | ADMIN/USABLE | 仅 USABLE |
| 删除卡（点 3 次确认） | ✓ | ✗ |

API：`grantPermission` / `revokeManagedPermission` / `deleteCardAsOwner`。

**钱包 UI 权限门禁（单点权威）：**

| 层 | 职责 |
|----|------|
| `BankCardsWorldData` 查询 API | `getCardsForPlayerInBank` / `canUse` 只返回可用卡 |
| `BankCardDetailUIRegistration.openUI` | 打开详情的唯一服务端校验 |
| 列表客户端 | 信任服务端下发；仅用 `canClientUse` 滤损坏快照 |
| 卡面 UI / 管理面板 | 展示与管理；写操作走服务端校验 |

### 货币

| 类型 | 作用 |
|------|------|
| `CurrencyType` | 已注册货币元数据（名称、图标） |
| `CurrencyStorageBankCard` | 卡上余额读写 |
| `CurrencyHelper` / `BankCurrencyResourceHandler` | 事务化增减 |

未知货币不会回退占位类型；解码时未知货币可能导致该卡或余额项被跳过。

### 可交易卡

`TradableWalletBankCard#getTradeTypeId()` 返回交易类型 ID，与 `TradeRegistry` 中存储器 ID 一致。  
钱包详情里 `TradableUI` 按该 ID 加载 `CurrencyItemTradeStorage` 渲染买卖面板。

当前 UI **不向玩家展示**交易失败文案；失败时 execute 侧 Transaction 回滚，状态不变。

## 内置内容（registration）

| 类 | 内容 |
|----|------|
| `BankRegistration` | 银行类型 + BankInfo |
| `CurrencyRegistration` | coins / gold / diamonds |
| `CardRegistration` | 卡类型 Codec + 工厂 + CardInfo |
| `TradableTypeRegistration` | 交易类型显示名 / 背景 |

价目条目在 `trade.registration.CurrencyItemTradeRegistration`（可用配置关闭）。

`WalletRegistration.init()` 顺序：

```text
CurrencyRegistration → TradableTypeRegistration → BankRegistration → CardRegistration
```

### 内置银行

| ID | 显示名（中） |
|----|----------------|
| `maple_banktrade:central` | 科技银行 |
| `maple_banktrade:farmers` | 农业银行 |
| `maple_banktrade:merchant` | 星空银行 |
| `maple_banktrade:nature` | 自然银行 |
| `maple_banktrade:magic` | 魔法银行 |

### 内置 card_type

```text
single_currency
tradable_single_currency
large_single_currency
multi_currency
tagged
```

### 主要创建工厂（nameIndex 示例）

| nameIndex | 卡形态 | 备注 |
|-----------|--------|------|
| `central_stone_ores_card` | 可交易单货币 | 石料矿石价目 |
| `farmers_plants_food_card` | 可交易单货币 | 植物食物价目 |
| `merchant_mob_drops_card` | 可交易单货币 | 生物掉落价目 |
| `central_single_currency_card` | 可交易单货币 | 兼容旧 ID → 石料矿石 |
| `central_large_single_currency_card` | 大额单货币 | 归属 farmers |
| `central_multi_currency_card` | 多货币 | coins/gold/diamonds |
| `central_tagged_card` / `magic_tagged_card` | 标记卡 | |
| `nature_single_currency_card` | 单货币 | |

完整列表以 `/mbt_bank factories` 与 `CardRegistration` 为准。

## 命令

见仓库根 [README.md](../../../../../../../README.md) 的 `/mbt_bank` 一节。  
创建与查询一律走 `MBTBankStates`，不绕过存档入口。

## 如何扩展

### 新银行类型

```java
public static final BankType EXAMPLE = BankType.BankTypeRegister(MapleBankTrade.id("example"));
// 再 BankInfo.registerBankInfo(...)
```

### 新货币

```java
CurrencyType.CurrencyTypeRegister(
    MapleBankTrade.id("copper"), "铜币", "Copper",
    List.of(), currencyTexture, backgroundTexture);
```

### 新卡类型

1. 子类声明 `CARD_TYPE_ID` 与 `CODEC`（字段含 `card_uuid` / `bank_type` / `card_type` / `name_index`）。
2. `BankCardType.register(CARD_TYPE_ID, Class, CODEC)`。
3. 若可创建：`BankCardFactory.register(nameIndex, 中文名, 英文名, bankType, factory)`。
4. 可选：`CardInfo.register` 绑定简化 / 详情 UI。
5. 需要余额则实现 `CurrencyStorageBankCard`；需要钱包交易则实现 `TradableWalletBankCard`。

### 新可交易价目类型

1. trade 侧：新建 `CurrencyItemTradeType` 并 `fill` 条目。
2. bank 侧：用**相同 Identifier** 注册 `TradableType` 显示信息。
3. 创建 `TradableSingleCurrencyBankCard` 工厂时写入该 trade type id。

内置内容里 bank 卡工厂会引用 `CurrencyItemTradeRegistration` 的静态 type 字段取 id；属于启动期静态装配，可接受。

## 货币操作示例

```java
CurrencyHelper.insertCurrency(server, cardUuid, CurrencyRegistration.COINS.id(), 100L);
CurrencyHelper.extractCurrency(server, cardUuid, CurrencyRegistration.COINS.id(), 50L);
```

大额使用 `BigInteger` 重载（大额卡实现支持超 long 余额）。

## 兼容注意

- 解码只信 `card_type`，不要用 `bank_type` 推断 Codec。
- 存档中未知 `card_type` / 未注册银行：该卡被跳过（宽松解码）。
- `name_index` 与 `card_type` 不要混用。
- 客户端卡快照上的 `clientPermission` 不参与世界存档。

## 验证

```powershell
.\gradlew.bat compileJava
```

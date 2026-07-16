# 枫糖银贸 / Maple BankTrade

**Minecraft NeoForge 模组**：钱包、银行卡、货币体系，以及「卖材料换钱 → 用钱买东西」的交易玩法。

---

## 这个模组能做什么？

| 功能 | 一句话说明 |
|------|------------|
| **钱包** | 随身打开，浏览你在各银行的银行卡 |
| **银行卡** | 账户存在服务器全局数据里，带余额、权限与（部分卡）交易价目 |
| **买卖交易** | 可交易卡：把物品卖成**金币**，或用金币从价目表购买物品 |
| **交易站** | 方块机器：物品 / 流体 / 能量 / 货币多资源配方加工 |
| **权限卡** | 把某几张卡的使用权做成物品，方便分享或配合机器 |

内置经济默认以 **金币（Coins）** 计价。新开的卡余额为 **0**，主要靠**出售物品**入账，再去购买。

---

## 安装

1. 安装对应版本的 **Minecraft 26.1.2** 与 **NeoForge**（约 `26.1.2.x`）。
2. 将本模组 jar 放入 `mods` 文件夹。
3. 同时放入依赖模组（开发/打包环境以项目依赖为准，常见包括 **MapleUtilLib**、**RegistryLib**、**LDLib2**、**Configuration** 等）。
4. 启动游戏即可。创造模式可在 **「枫糖银贸 / 银行」** 创造页签中取到钱包、权限相关物品与交易站。

> 单人与服务器均可使用；银行卡数据保存在**世界全局存档**中，不随维度切换丢失。

---

## 快速上手（推荐流程）

### 1. 打开钱包

任选其一：

- 创造页签取出 **钱包**，手持右键打开；或  
- 聊天输入：`/mbt_bank wallet`

点封面提示 **「点击以进入」** 后，左右页会列出你各银行下可用的卡。

### 2. 开一张卡

```text
/mbt_bank factories          # 查看可创建的卡类型
/mbt_bank create <工厂id>    # 给自己开一张卡
```

常用工厂 id（部分）：

| 工厂 id | 中文名 | 适合做什么 |
|---------|--------|------------|
| `maple_banktrade:central_stone_ores_card` | 中央银行石料矿石卡 | 买卖石头、矿石、锭类 |
| `maple_banktrade:farmers_plants_food_card` | 农业银行植物食物卡 | 买卖作物与食物 |
| `maple_banktrade:merchant_mob_drops_card` | 星空银行生物掉落卡 | 买卖生物掉落物 |

完整列表以游戏中 `factories` 输出为准。

### 3. 卖东西赚钱

1. 在钱包里点开对应**可交易卡**。  
2. 详情页会有交易面板与**卖出槽**。  
3. 把价目表里的物品放进卖出槽 → 自动按单价结算，金币进入卡余额。  

### 4. 用余额购买

- 在价目表物品上 **左键** 购买。  
- 按住修饰键可加大批量（组合生效）：

| 按键 | 数量加成 |
|------|----------|
| Shift | ×1 档（4） |
| Ctrl | ×2 档（8） |
| Alt | ×4 档（64） |

余额不足、背包满、物品不可卖等情况会导致交易失败并**回滚**；当前 UI **不会**弹出失败提示，请以余额与物品是否变化为准。

### 5. 管理权限（可选）

若你是卡的**拥有者**或**管理员**，详情页可打开 **权限管理**：

| 角色 | 能做什么 |
|------|----------|
| **拥有者** | 授予管理员 / 使用者；撤销；**删除卡**（需连点确认 3 次） |
| **管理员** | 授予 / 撤销「使用者」 |
| **使用者** | 查看余额、买卖（视卡类型） |
| **不可使用** | 无法使用该卡 |

权限是**按玩家绑定到卡**的，不是绑在钱包物品上。删卡后相关权限会一并清理。

---

## 核心概念（玩家向）

### 钱包物品

| 物品 | 作用 |
|------|------|
| **钱包** | 打开钱包 UI，浏览各银行下你有权使用的卡 |
| **银行权限卡构造器** | 选择自己可管理的卡，导出成「权限卡」物品 |
| **银行权限卡** | 携带若干卡 UUID；可用于分享使用权或给交易站等场景识别 |

银行卡本身**不是**背包里的实体物品账户，而是存在服务器的**虚拟账户**（有 UUID）。钱包只是查看入口。

### 五家内置银行

| 银行 | 定位（简介） |
|------|----------------|
| **科技银行** | 自动化账户、标准银行卡 |
| **农业银行** | 农业贸易与日常储蓄 |
| **星空银行** | 商贸流通、多货币 |
| **自然银行** | 自然资源与基础储蓄 |
| **魔法银行** | 特殊资产、标记账户 |

不同卡归属不同银行，钱包里会按银行分页展示。

### 卡的种类（玩法差异）

| 类型 | 玩家体感 |
|------|----------|
| **可交易单货币卡** | 有价目表：可买卖对应分类物品（最常用） |
| **单货币 / 大额单货币卡** | 有余额，但未必带买卖面板 |
| **多货币卡** | 同时持有 **金币 / 黄金 / 钻石** 等 |
| **标记卡** | 偏标签/特殊用途，不是标准买卖账户 |

### 内置货币

| 货币 | 说明 |
|------|------|
| **金币 (Coins)** | 内置物品买卖的主货币 |
| **黄金 (Gold)** | 多货币卡等场景使用 |
| **钻石 (Diamonds)** | 多货币卡等场景使用 |

### 内置「物品 ↔ 金币」价目分类

| 分类 | 举例（价格为单价金币，买卖同一价） |
|------|--------------------------------------|
| **石料与矿石** | 圆石 1 · 铁矿 12 · 铁锭 30 · 钻石 150 · 远古残骸 250 … |
| **植物与食物** | 小麦种子 1 · 小麦 2 · 面包 6 · 熟牛肉 10 · 金胡萝卜 40 … |
| **生物掉落** | 腐肉 1 · 骨头 3 · 火药 8 · 末影珍珠 40 · 潜影壳 80 … |

关闭 `enableBuiltInTrades` 后，这些价目不会注册；可交易卡仍可开，但面板可能为空。  
关闭 `enableModContent` 后，内置银行/卡/货币/交易站与价目均不注册，仅保留钱包等 API。

---

## 交易站

**交易站**是一个可放置方块（创造页签可取）。右键打开界面，内含多个加工标签页，例如：

| 标签 | 大致用途 |
|------|----------|
| **电冶台** | 原矿 + 能量 → 金属锭 |
| **水洗台** | 物品 + 流体（如水）→ 产物 |
| **锻压台** | 压缩、成型、精炼类 |
| **银行台** | 铸币、与货币相关的配方 |
| **能量台** | 燃料 / 流体 → 能量 (FE) |

界面中有输入、输出与能量显示。涉及扣款的配方需要机器能识别到有权限的银行卡（例如配合权限卡 / 多卡逻辑，以实际 UI 为准）。

---

## 命令 `/mbt_bank`

默认**不需要 OP**，面向自助调试与游玩。可在配置中关闭整组命令。

| 命令 | 作用 |
|------|------|
| `/mbt_bank` 或 `/mbt_bank list` | 列出你当前可用的卡 |
| `/mbt_bank factories` | 列出可创建工厂（含 id） |
| `/mbt_bank create <factory>` | 给自己创建一张卡 |
| `/mbt_bank info <cardUuid>` | 查看卡详情与余额 |
| `/mbt_bank wallet` | 打开钱包 UI（无需手持钱包） |

**没有**通过命令直接加减余额、授权或删卡的子命令；管理权限请在卡详情 UI 中操作。

---

## 配置（服主 / 整合包）

路径：

```text
config/maple_banktrade/maple_banktrade.yaml
```

| 项 | 默认 | 含义 |
|----|------|------|
| `general.enableModContent` | `true` | 是否注册模组内置内容（银行/卡/货币/交易站等）。**关闭后仅保留 API**（钱包物品、UI、银行数据、命令） |
| `general.enableBuiltInTrades` | `true` | 是否注册内置物品价目与机器示例配方（仅在 `enableModContent=true` 时生效） |

修改后通常需要**重启**游戏或服务端。

---

## 常见问题

**Q：开卡后为什么买不了东西？**  
A：新卡余额为 0。请先把价目表里的物品放进卖出槽入账。

**Q：卖了 / 买了没反应？**  
A：失败会静默回滚。请检查：是否有权使用该卡、物品是否在该卡价目内、余额是否足够、背包是否有空位。

**Q：钱包里看不到卡？**  
A：只有权限为「可用」及以上的卡会列出。可用 `/mbt_bank list` 核对，或重新 `create`。

**Q：多人联机卡会丢吗？**  
A：数据在服务器全局 SavedData（约 `世界/data/maple_banktrade/bank_cards.dat`）。换维度不丢；删档 / 换世界会丢。

**Q：怎么把卡给别人用？**  
A：由拥有者在卡详情「权限管理」里授予 **使用者** 或 **管理员**；或使用权限卡物品做携带式授权（视服务器玩法约定）。

---

## 链接

- 仓库：https://github.com/wanggugu197/Maple-BankTrade  
- 问题反馈：https://github.com/wanggugu197/Maple-BankTrade/issues  
- 许可证：LGPL v3（见 `TEMPLATE_LICENSE.txt`）  
- 作者：maple197  

---

## 给开发者

以下内容面向二次开发与贡献；玩家可跳过。

### 技术栈与标识

| 项 | 值 |
|----|-----|
| 包名 | `com.maple.maple_banktrade` |
| mod_id | `maple_banktrade` |
| MC / NeoForge | `26.1.2` / `26.1.2.x` |

### 源码地图（精简）

```text
com.maple.maple_banktrade
├── MapleBankTrade.java       # 入口
├── client/                   # 客户端
├── common/                   # 启动装配、创造页、交易站、内置注册
├── config/                   # YAML 配置
├── data/lang/                # 双语语言（datagen）
├── api/
│   ├── bank/                 # 卡模型、世界数据、钱包 / 权限 UI API
│   └── trade/                # 三段式交易协议与 TradeRegistry
├── bank/                     # 银行实现、卡类型、命令
└── trade/
    ├── currency_item/        # 货币-物品交易运行时
    └── machine/              # 方块实体多资源交易运行时
```

### 模块文档

| 文档 | 内容 |
|------|------|
| [`api/trade/README.md`](src/main/java/com/maple/maple_banktrade/api/trade/README.md) | 交易框架协议（check → execute → afterSuccess） |
| [`bank/README.md`](src/main/java/com/maple/maple_banktrade/bank/README.md) | 银行 / 卡 / 存档 / 扩展 |
| [`trade/currency_item/README.md`](src/main/java/com/maple/maple_banktrade/trade/currency_item/README.md) | 货币-物品交易实现 |
| [`trade/machine/README.md`](src/main/java/com/maple/maple_banktrade/trade/machine/README.md) | 机器多资源交易 |
| [`common/trade/README.md`](src/main/java/com/maple/maple_banktrade/common/trade/README.md) | 内置价目与机器配方注册 |

### 本地构建

```bat
gradlew.bat compileJava
gradlew.bat runClient
gradlew.bat runData
```

`runData` 会把语言等生成资源写到 `src/generated/resources`。

### 扩展提示（极简）

- **新银行 / 货币 / 卡工厂**：见 `bank/README.md` 与 `common.bank.*Registration`。  
- **新物品价目类型**：trade 侧注册 `CurrencyItemTradeType` + 条目；bank 侧用**同一 Identifier** 注册 `TradableType` 显示；卡工厂写入该 trade type id。  
- **新机器配方**：`MachineTradeType` + `MachineTradeStorage`，由方块实体组装 `MachineTradeContext` 调用。  
- **启动注意**：`FMLCommonSetupEvent` 前勿对原版 `Items.*` 立刻 `getDefaultInstance()` / `ItemStackTexture`（可能 NPE）；图标优先 `SpriteTexture` 或延迟 `IGuiTexture.dynamic`。

更细的注册时机与包结构说明以各子目录 README 为准。

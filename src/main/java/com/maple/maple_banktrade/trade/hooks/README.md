# Maple-BankTrade 交易钩子（Trade Hooks）使用文档

本目录包含机器交易（`MachineTrade`）的全部自定义钩子实现，用于在**可见性**、**执行前检查**、**成功回调**三个环节注入逻辑。

- 钩子基类定义在 `api/trade/machine/MachineTradeHooks.java`
- 全部实现均实现 `IPersistedSerializable`，字段用 `@Persisted` 标注，可由 LDLib2 自动持久化
- 交易通过 `MachineTrade.builder(id)` 构建，并用 `.visibilityHook(...)` / `.checkHook(...)` / `.successHook(...)` 挂载

---

## 1. 三种钩子与执行流程

每次交易（`MachineTradeHandler.run / check` → `TradeRunner`）按以下顺序执行：

```
check 阶段：
  1. trade.visibilityHook().isVisible(context, trade)   ← 不可见 → 拒绝
  2. trade.checkHook().check(context, request, trade)   ← 返回 false → 拒绝；
                                                          可通过 request.setDesiredCount(int) 降级次数
  3. 水桶法 maxFeasibleCount(降级后次数) → MachineTradePlan
execute 阶段：
  4. 事务内按 plan 转移物品/流体/能量/货币 → MachineTradeDetail
afterSuccess 阶段：
  5. trade.successHook().afterSuccess(context, request, plan, result)
```

> 同一 `MachineTradeRequest` 实例贯穿 check → execute → afterSuccess，因此 CheckHook 对次数的修改会传导到后续阶段。

### 可见性 vs 检查钩子如何选择

| | 可见性钩子 `VisibilityHook` | 检查钩子 `CheckHook` |
|---|---|---|
| 方法 | `boolean isVisible(context, trade)` | `boolean check(context, request, trade)` |
| 影响 | 交易条目是否在 UI 显示、自动交易是否匹配 | 交易是否允许执行 |
| 典型用途 | “完成任务才解锁 / 仅在某地显示” | “雨天才能买 / 每次最多 1 次” |
| 共用 | 大部分逻辑成对出现（如 `WeatherVisibleHook` / `WeatherCheckHook`） | 同左 |

---

## 2. 基类与默认实现（`MachineTradeHooks`）

| 类 | 作用 |
|---|---|
| `MachineTradeHooks.VisibilityHook` | 可见性钩子基类，抽象方法 `isVisible` |
| `MachineTradeHooks.CheckHook` | 检查钩子基类，抽象方法 `check` |
| `MachineTradeHooks.SuccessHook` | 成功回调基类，抽象方法 `afterSuccess` |
| `AlwaysVisibleHook` | 默认可见性：始终返回 true |
| `PassCheckHook` | 默认检查：始终放行 |
| `NoopSuccessHook` | 默认回调：无操作 |

---

## 3. 可见性钩子（`visibleHook` 包，共 23 个）

> 通用约定：除特别说明外，带 `flip` 字段的钩子均为 `flip != 条件` 语义（`flip=false` 正常判断，`flip=true` 取反）；当判断目标缺失（如卡不存在、无玩家、位置为空）时通常返回 `flip`。

### 3.1 位置 / 环境类

| 钩子 | 作用 | 构造 |
|---|---|---|
| `AABBVisibleHook` | 所在位置在轴对齐区域内（含边界） | `(BlockPos minPos, BlockPos maxPos)` |
| `HeightVisibleHook` | Y 坐标在 `[minY, maxY]` 内；`(minY)` 表示 ≥ minY | `(int minY, int maxY)` / `(int minY)` |
| `DimensionVisibleHook` | 所在维度匹配 | `(Identifier targetDimension)` / `(…, boolean flip)` |
| `BiomeVisibleHook` | 所在位置群系匹配 | `(Identifier biomeId)` / `(…, boolean flip)` |
| `BiomeTagVisibleHook` | 所在位置群系包含指定标签 | `(Identifier tagId)` / `(…, boolean flip)` |
| `StructureVisibleHook` | 所在位置在指定结构内 | `(Identifier structureId)` / `(…, boolean flip)` |
| `StructureTagVisibleHook` | 所在位置在属于指定标签的结构内 | `(Identifier tagId)` / `(…, boolean flip)` |
| `TimeWindowVisibleHook` | 当前时刻在 `[startTick, endTick]` 内（支持跨午夜：start>end 时取并集） | `(long startTick, long endTick)` |
| `MoonPhaseVisibleHook` | 当前月相 == phase（0~7，0=满月，4=新月，按世界天数 % 8） | `(int phase)` / `(…, boolean flip)` |
| `WeatherVisibleHook` | 当前天气匹配（state：0=晴，1=雨，2=雷暴） | `(short state)` / `(…, boolean flip)` |

示例：

```java
// 仅在下界显示/出售
.visibilityHook(new DimensionVisibleHook(Level.NETHER.identifier()))

// 仅在海滩群系（标签）可见
.visibilityHook(new BiomeTagVisibleHook(Identifier.parse("minecraft:is_beach")))

// 满月之夜才出现
.visibilityHook(new MoonPhaseVisibleHook(0))

// 仅在雷暴天可见（state：0=晴，1=雨，2=雷暴）
.visibilityHook(new WeatherVisibleHook((short) 2))

// 高空（Y ≥ 120）可见
.visibilityHook(new HeightVisibleHook(120))
```

### 3.2 玩家类

| 钩子 | 作用 | 构造 |
|---|---|---|
| `PlayerStateVisibleHook` | 触发者是指定 UUID / 玩家名的玩家；两者皆空时匹配任意玩家；非玩家触发返回 flip | `(UUID playerUuid)` / `(String playerName)` / `(UUID, String, boolean flip)` |
| `PlayerExperienceVisibleHook` | 玩家经验等级 ≥ level | `(int level)` / `(…, boolean flip)` |

示例：

```java
.visibilityHook(new PlayerStateVisibleHook(playerUuid))   // 绑定专属玩家
.visibilityHook(new PlayerStateVisibleHook("Steve"))      // 按名字
.visibilityHook(new PlayerExperienceVisibleHook(30))      // 30 级才可见
```

### 3.3 银行卡类

| 钩子 | 作用 | 构造 |
|---|---|---|
| `CardExistenceVisibleHook` | 上下文存在 nameIndex 匹配的卡 | `(Identifier cardNameIndex)` |
| `CurrencyAmountVisibleHook` | 存在一张卡其货币余额 **>** amount（任一卡匹配） | `(Identifier currencyTypeId, BigInteger amount)` / `(…, boolean flip)` |
| `TaggedCompletedVisibleHook` | 标记卡中条目 id 已完成 | `(Identifier nameIndex, String id)` / `(…, boolean flip)` |
| `TaggedProgressVisibleHook` | 标记卡中条目 id 进度 ≥ progress | `(Identifier nameIndex, String id, int progress)` / `(…, boolean flip)` |
| `TaggedMultiCompletedVisibleHook` | 标记卡中一组条目（Set）**全部**完成 | `(Identifier, Set<String> ids)` / `(…, boolean flip)` / `(Identifier, String... ids)` |
| `TaggedTierCompletedCountHook` | 指定 tier 已完成条目数 ≥ threshold | `(Identifier, short tier, int threshold)` / `(…, boolean flip)` |
| `TaggedTierCompletedRatioHook` | 指定 tier 完成比例（0~100）≥ percent | `(Identifier, short tier, int percent)` / `(…, boolean flip)` |
| `TaggedTotalCompletedCountHook` | 总完成条目数 ≥ threshold | `(Identifier, int threshold)` / `(…, boolean flip)` |
| `TaggedTotalCompletedRatioHook` | 总完成比例（0~100）≥ percent | `(Identifier, int percent)` / `(…, boolean flip)` |

示例：

```java
// 需要携带中央标记卡
.visibilityHook(new CardExistenceVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex()))

// 卡上金币 > 100
.visibilityHook(new CurrencyAmountVisibleHook(CurrencyRegistration.COINS.id(), BigInteger.valueOf(100)))

// “挖掘”任务组：石头与铁锭两条目都完成才解锁
.visibilityHook(new TaggedMultiCompletedVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(),
        Set.of("stone", "iron_ingot")))

// 所有条目 100% 完成（毕业解锁）
.visibilityHook(new TaggedTotalCompletedRatioHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), 100))
```

### 3.4 组合 / 跨交易类

| 钩子 | 作用 | 构造 |
|---|---|---|
| `CompositeVisibilityHook` | 至少 `requiredCount` 个子钩子可见即可见（≤0 或空列表时恒可见） | `(int requiredCount, VisibilityHook... hooks)` / `(List, int)` |
| `SiblingTradeVisibleHook` | 本 storage 中另一交易条目当前可见（连锁解锁）；条目不存在返回 flip | `(Identifier siblingTradeId)` / `(…, boolean flip)` |

示例：

```java
// 白天 且 携带标记卡 才可见
.visibilityHook(new CompositeVisibilityHook(2,
        new TimeWindowVisibleHook(0, 12000),
        new CardExistenceVisibleHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex())))

// 完成“任务一”之后才解锁“任务二”
.visibilityHook(new SiblingTradeVisibleHook(MapleBankTrade.id("quest_1")))
```

---

## 4. 检查钩子（`checkHook` 包，共 24 个）

检查钩子与可见性钩子**一一对应**（见第 3 节表格，构造器相同，`check` 签名多一个 `request` 参数），此外还有：

| 钩子 | 作用 | 构造 |
|---|---|---|
| `DimensionCheckHook` | 维度匹配（无 flip 的简化版） | `(Identifier targetDimension)` |
| `TimeWindowCheckHook` | 时刻在 `[startTick, endTick]` 内（**不支持**跨午夜回绕） | `(long startTick, long endTick)` |
| `LimitCountCheckHook` | 把本次交易期望次数降为 `min(请求次数, maxCount)` 后放行 | `()` 默认 1 次 / `(int maxCount)` |

> `DimensionVisibleHook` / `TimeWindowVisibleHook` 的完整版（含 flip / 回绕）见第 3 节。

示例：

```java
// 只有白天能执行
.checkHook(new TimeWindowCheckHook(0, 12000))

// 每次执行最多 1 次（手动点击与自动交易均受限）
.checkHook(new LimitCountCheckHook(1))

// 一次最多买 16 个
.checkHook(new LimitCountCheckHook(16))
```

### 限购 / 次数降级机制

1. `MachineTradeRequest` 是可变的，提供 `setDesiredCount(int)`（必须为正数，否则抛异常）；
2. `MachineTradeDefinition.check` 在 CheckHook 返回后**重新读取** `request.desiredCount()`；
3. 之后的水桶法计算、plan、execute 全部使用降级后的次数。

因此任意自定义 CheckHook 都可以降级次数，例如“库存少于 3 组才允许买一组”：

```java
public boolean check(MachineTradeContext context, MachineTradeRequest request, MachineTrade trade) {
    request.setDesiredCount(1);
    return true;
}
```

---

## 5. 成功回调钩子（`successHook` 包，共 6 个）

| 钩子 | 作用 | 构造 |
|---|---|---|
| `LogSuccessHook` | 成功后写日志，并向触发玩家发送消息 | `(Component logMessage)` |
| `TaggedIncreaseSuccessHook` | 标记卡条目 id 进度增加（按实际成交次数 `detail.tradeCount()`） | `(Identifier nameIndex, String id)` |
| `TaggedResetSuccessHook` | 标记卡条目 id 进度**重置为 0**（循环任务清空进度） | `(Identifier nameIndex, String id)` |
| `FireworkSuccessHook` | 成功后放烟花（1~3 档强度） | `(int intensity)` |
| `CommandSuccessHook` | 成功后以服务器控制台（op 权限 4）执行指令 | `(String command)` |
| `CompositeSuccessHook` | 依次执行所有子回调 | `(SuccessHook... hooks)` |

示例：

```java
// 交易成功后给玩家发奖励
.successHook(new CommandSuccessHook("give @p minecraft:diamond 1"))

// 完成一次就推进任务进度
.successHook(new TaggedIncreaseSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "stone"))

// 满足条件后清空该条目进度（重置循环任务）
.successHook(new TaggedResetSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "stone"))

// 又发消息又放烟花
.successHook(new CompositeSuccessHook(
        new LogSuccessHook(Component.literal("购买成功！")),
        new FireworkSuccessHook(2)))
```

---

## 6. 完整示例：一条“雨天限购的收购 + 任务链”交易

```java
// 任务一：雨天 20 级玩家才能出售黏土，每次最多 4 组，成交后推进标记卡进度
MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("rainy_clay_sale"))
        .addItemInput(Items.CLAY, 4)
        .addCurrencyInsert(CurrencyRegistration.COINS.id(), 2)
        .visibilityHook(new CompositeVisibilityHook(2,
                new WeatherVisibleHook((short) 1),
                new PlayerExperienceVisibleHook(20)))
        .checkHook(new LimitCountCheckHook(4))
        .successHook(new TaggedIncreaseSuccessHook(CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(), "clay"))
        .build());

// 任务二：任务一完成后才解锁（SiblingTradeVisibleHook 反查 storage）
MachineItemDesk.register(MachineTrade.builder(MapleBankTrade.id("clay_quest_reward"))
        .addCurrencyExtract(CurrencyRegistration.COINS.id(), 32)
        .addItemOutput(Items.DIAMOND, 1)
        .visibilityHook(new SiblingTradeVisibleHook(MapleBankTrade.id("rainy_clay_sale")))
        .build());
```

---

## 7. 注意事项

- **循环引用**：`SiblingTradeVisibleHook` / `SiblingTradeCheckHook` 若互相引用会无限递归，务必保证引用图无环。
- **op 权限**：`CommandSuccessHook` 以权限 4 执行指令，只能配置可信指令，防止配置注入。
- **Set 可变性**：`TaggedMultiCompleted*` 的 `Set<String>` 字段在构造时已复制为可变 `LinkedHashSet`；请勿自行改为 `Set.of(...)`（LDLib2 反序列化需要原地 clear + add）。
- **缺失目标时的语义**：找不到卡 / 非玩家 / 位置为空时，大部分带 flip 的钩子返回 `flip`（即“条件不成立”）。组合钩子会跳过 null 子钩子。
- **限购对自动交易同样生效**：自动交易请求次数为 `1_000_000`，`LimitCountCheckHook` 会把它压到 maxCount。
- **时间回绕**：跨午夜的区间（如晚上 18:00–次日 6:00）用 `TimeWindowVisibleHook(start=18000, end=6000)`；`TimeWindowCheckHook` 不支持回绕，请用两个钩子组合或用 `CompositeCheckHook`。

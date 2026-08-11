# 树状+链式任务系统底层架构文档

**版本**: 3.6  
**最后更新**: 2026-08-11  
**状态**: 核心数据层 ✅ | 逻辑层 ✅ | 存储层 ✅ | 条件引擎 ✅ | 触发层 ✅ | 任务类型 ✅ | 奖励系统 ✅ | 依赖模式 ✅ | 定义/状态分离 ✅ | UI 层 ✅


## 一、概述

### 1.1 设计目标

本系统为《我的世界》模组提供一套**树状+链式混合**的任务底层架构，支持：

- 树状层级结构（父子节点）
- 链式顺序推进（兄弟节点顺序）
- 多条件依赖解锁（跨节点依赖）+ 灵活依赖模式（ALL_COMPLETED / ONE_COMPLETED / ALL_STARTED / ONE_STARTED）
- 多种任务行为模式（单次、多次、随机池、隐藏链、随机激活）
- 任务完成类型（确认完成、提交物品，可扩展）
- 奖励系统（物品奖励，可扩展）
- 完成历史记录（支持循环任务、重复完成）
- 动态条件门控（注册表条件 + 组合条件 AND/OR/NOT）

### 1.2 核心设计原则

| 原则 | 说明 |
| :--- | :--- |
| **定义与状态分离** | `QuestDefinitionRegistry`（全局静态，不可变）与 `PlayerQuestData`（按 UUID，可变）物理隔离 |
| **历史与状态分离** | `ICompletionRecord`（完成历史）独立存储，仅追加 |
| **接口与实现分离** | API层纯接口，Common层基础实现，逻辑层纯计算 |
| **不可变性优先** | 定义和记录不可变，状态通过接口方法变更 |
| **类型安全** | 条件使用 `Identifier` + `CompoundTag`，编译期验证 |
| **UUID 统一入口** | 存储层不绑定 Player 实例，通过 `QuestDataHelper` 统一 UUID 入口访问 |

### 1.3 包结构

```
com.maple.maple_banktrade.api.quests/          # API 层（按职责分子包，12 个）
├── README.md
├── QuestDefinitionRegistry.java               # 全局静态任务定义注册表（定义/状态分离）
├── enums/                                     # 枚举 (4)
│   ├── DependencyRequirement.java             # 依赖模式枚举（四种）
│   ├── TaskStatus.java                        # 状态枚举（四态）
│   ├── TaskType.java                          # 类型枚举（三类）
│   └── TaskBehavior.java                      # 行为枚举（六种）
├── core/                                      # 核心接口 (5)
│   ├── ITaskDefinition.java                   # 任务定义接口
│   ├── ITaskState.java                        # 任务状态接口
│   ├── ICompletionRecord.java                 # 完成历史接口
│   ├── ITaskInstance.java                     # 任务实例接口
│   └── IQuestRepository.java                  # 仓储接口
├── impl/                                      # 基础实现 (5)
│   ├── BaseTaskDefinition.java                # 基础定义实现
│   ├── BaseTaskState.java                     # 基础状态实现
│   ├── BaseCompletionRecord.java              # 基础历史实现
│   ├── BaseTaskInstance.java                  # 基础实例实现
│   └── BaseTaskInstanceFactory.java           # 实例工厂
├── repository/                                # 仓储实现 (2)
│   ├── InMemoryQuestRepository.java           # 内存仓储实现
│   └── PlayerQuestData.java                   # 玩家持久化数据（仅可变内容，Codec序列化）
├── condition/                                 # 条件系统 (12)
│   ├── IScriptEvaluator.java                  # 条件求值接口
│   ├── RegistryScriptEvaluator.java           # 注册表条件评估器
│   ├── QuestConditionRegistry.java            # 条件注册表（仿 MachineTradeHookRegistry）
│   ├── ResolutionContext.java                 # 计算上下文
│   ├── EvaluationContext.java                 # 类型化评估上下文
│   ├── BaseQuestCondition.java                # 条件基类 + ConditionFactory
│   ├── LevelCondition.java                    # 等级条件
│   ├── HealthCondition.java                   # 血量条件
│   ├── HasItemCondition.java                  # 持有物品条件
│   ├── HasPotionEffectCondition.java          # 药水效果条件
│   ├── IsRainingCondition.java                # 天气条件
│   └── CompositeCondition.java               # 组合条件（AND/OR/NOT）
├── calculator/                                # 计算引擎 (4)
│   ├── StateDelta.java                        # 状态变更记录
│   ├── VisibilityCalculator.java              # 可见性计算器
│   ├── TreeTraversalService.java              # 树链拓扑服务
│   └── StateTransitionOrchestrator.java       # 状态流转编排器
├── scheduler/                                 # 调度触发 (2)
│   ├── CooldownResetService.java              # 冷却/重置服务
│   └── QuestTriggerHandler.java               # 触发层（定时刷新 + 登录/登出）
├── storage/                                   # 存储层 (3)
│   ├── QuestSavedData.java                    # 世界持久化存储（SavedData 挂载）
│   ├── QuestDataHelper.java                   # 统一 UUID 入口（类似 BankHelper）
│   └── QuestDataManager.java                  # 任务数据管理器（委托存储层）
├── tasktype/                                  # 任务完成类型 (4)
│   ├── ITaskType.java                         # 任务类型接口
│   ├── TaskTypeRegistry.java                  # 任务类型注册表
│   ├── ConfirmTaskType.java                   # 确认完成类型
│   └── SubmitItemTaskType.java                # 提交物品类型
├── reward/                                    # 奖励系统 (4)
│   ├── RewardDef.java                          # 奖励定义数据类（Identifier + CompoundTag）
│   ├── IReward.java                           # 奖励接口
│   ├── RewardRegistry.java                    # 奖励注册表
│   └── ItemReward.java                        # 物品奖励实现
└── ui/                                        # 🆕 v3.6 UI 层 (7)
    ├── QuestUIRegistration.java                # 主 UI 注册（PlayerUIMenuType + TabView）
    ├── QuestUIStylesheets.java                 # 样式表加载工具
    ├── QuestUiHelper.java                      # S2C 数据快照构建 + 格式化
    ├── QuestTaskListPanel.java                 # 左栏 30% 任务列表（按类型分组）
    ├── QuestTaskDetailPanel.java               # 右栏 70% 任务详情（接取/完成）
    ├── QuestCompletedPanel.java                # 已完成任务标签页
    └── QuestTreePanel.java                     # 🆕 创造模式树状结构标签页

com.maple.maple_banktrade.common.quests/        # Common 层（具体蓝图）
├── QuestBlueprints.java                       # 蓝图注册中心（26个任务）
└── QuestRepositoryLoader.java                 # 仓储加载器（含拓扑验证）
```


## 二、已完成内容（Complete）

### 2.1 API层接口与枚举

#### 2.1.1 `TaskStatus` 枚举（四态）

| 状态 | 说明 | 可见性 | 可交互 |
| :--- | :--- | :--- | :--- |
| `HIDDEN` | 隐藏 | UI不可见 | 否 |
| `VISIBLE_LOCKED` | 可见但锁定 | UI可见（灰锁） | 否 |
| `ACTIVE` | 进行中 | UI可见（高亮） | 是（进度追踪） |
| `COMPLETED` | 已完成 | UI可见（完成标记） | 否（循环任务可重置） |

#### 2.1.2 `TaskType` 枚举（三类任务）

| 类型 | 用途 | 调度方式 |
| :--- | :--- | :--- |
| `MAIN` | 主线任务 | 按 `prevSiblingId` 顺序自动推进 |
| `SIDE` | 支线任务 | 依赖 `dependentNodes` 解锁，手动/自动接取 |
| `TEMPORARY` | 临时任务 | 随机触发，由逻辑层控制数量上限 |

#### 2.1.3 `TaskBehavior` 枚举（六种行为模式）

| 行为 | 说明 | 关键字段 |
| :--- | :--- | :--- |
| `SIMPLE` | 普通单次任务，完成一次即结束 | 默认行为 |
| `MULTI_COMPLETION` | 多次完成，需完成 `requiredCompletions` 次才触发后继 | `requiredCompletions` |
| `RANDOM_POOL` | 随机池，激活时从 `poolIds` 中随机选子任务执行 | `poolIds` |
| `MULTI_RANDOM_POOL` | 多次随机池，需完成 `requiredCompletions` 次，每次随机选子任务 | `requiredCompletions`, `poolIds` |
| `HIDDEN_CHAIN` | 隐藏链，完成后按 `nextChainTriggerChance` 概率触发后继 | `nextChainTriggerChance` |
| `RANDOM_ACTIVATE` | 随机激活，完成后自动变为 `HIDDEN`，可再次被随机触发 | `autoResetToHidden` |

#### 2.1.4 `DependencyRequirement` 枚举（四种依赖模式）

> 🆕 v3.4 新增

| 模式 | 说明 | 判断逻辑 |
| :--- | :--- | :--- |
| `ALL_COMPLETED` | 所有依赖任务必须完成（默认） | `deps.stream().allMatch(isEffectivelyFinished)` |
| `ONE_COMPLETED` | 至少一个依赖任务完成 | `deps.stream().anyMatch(isEffectivelyFinished)` |
| `ALL_STARTED` | 所有依赖任务必须已开始（ACTIVE 或 COMPLETED） | `deps.stream().allMatch(status == ACTIVE\|\|COMPLETED)` |
| `ONE_STARTED` | 至少一个依赖任务已开始 | `deps.stream().anyMatch(status == ACTIVE\|\|COMPLETED)` |

#### 2.1.5 核心接口

| 接口 | 职责 | 实现类 |
| :--- | :--- | :--- |
| `ITaskDefinition` | 静态蓝图（不可变） | `BaseTaskDefinition` |
| `ITaskState` | 动态状态（可变） | `BaseTaskState` |
| `ICompletionRecord` | 完成历史（不可变，仅追加） | `BaseCompletionRecord` |
| `ITaskInstance` | 运行时组合（定义+状态+历史） | `BaseTaskInstance` |
| `IQuestRepository` | 统一数据访问 | `InMemoryQuestRepository` |

#### 2.1.6 `ITaskDefinition` 完整字段列表

| 字段 | 类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | `String` | **必填** | 全局唯一标识符 |
| `type` | `TaskType` | `MAIN` | 任务大类 |
| `isGroup` | `boolean` | `false` | 纯分组节点（不算任务） |
| `parentId` | `String` | `null` | 父节点ID（构成树） |
| `prevSiblingId` | `String` | `null` | 同级链表前驱（构成顺序链） |
| `dependentNodes` | `List<String>` | `[]` | 额外依赖节点列表 |
| `nextTaskInChain` | `String` | `null` | 跨分支链式后继 |
| `repeatable` | `boolean` | `false` | 是否可循环 |
| `maxRepeatTimes` | `int` | `-1` | 最大循环次数（-1=无限） |
| `forceParentVisible` | `boolean` | `false` | 强制父节点可见 |
| `visibilityConditionId` | `Identifier` | `null` | 可见条件 ID（null=无条件） |
| `visibilityConditionParams` | `CompoundTag` | `{}` | 可见条件参数 |
| `unlockConditionId` | `Identifier` | `null` | 解锁条件 ID（null=无条件） |
| `unlockConditionParams` | `CompoundTag` | `{}` | 解锁条件参数 |
| `childrenIds` | `List<String>` | `[]` | 预计算子节点列表 |
| `behavior` | `TaskBehavior` | `SIMPLE` | 任务行为模式 |
| `requiredCompletions` | `int` | `1` | 需要完成的次数 |
| `poolIds` | `List<String>` | `[]` | 随机池子任务ID列表 |
| `nextChainTriggerChance` | `double` | `1.0` | 触发后继链概率 (0.0~1.0) |
| `autoResetToHidden` | `boolean` | `false` | 完成后重置为 `HIDDEN` |
| `taskTypeId` | `Identifier` | `null` | 🆕 任务完成类型 ID（null=确认完成） |
| `taskTypeParams` | `CompoundTag` | `{}` | 🆕 任务完成类型参数 |
| `rewardIds` | `List<RewardDef>` | `[]` | 🆕 奖励定义列表（Identifier + CompoundTag） |
| `dependencyRequirement` | `DependencyRequirement` | `ALL_COMPLETED` | 🆕 依赖满足模式 |

### 2.2 任务完成类型系统

> 🆕 v3.4 新增

通过 `ITaskType` 接口 + `TaskTypeRegistry` 注册表实现，与 `TaskType`（分类）不同，定义的是"完成方式"。

| 注册 ID | 实现类 | 说明 |
| :--- | :--- | :--- |
| `maple_banktrade:confirm` | `ConfirmTaskType` | 确认完成：无额外检查，点击即完成（默认行为） |
| `maple_banktrade:submit_item` | `SubmitItemTaskType` | 提交物品：检查背包中是否有足够物品，完成时扣除 |

参数格式（`taskTypeParams`）：
```json
{
  "item": "minecraft:diamond",
  "count": 3
}
```

### 2.3 奖励系统

> 🆕 v3.4 新增（v3.5 升级为 Identifier + CompoundTag 格式）

通过 `IReward` 接口 + `RewardRegistry` 注册表实现。任务完成时自动发放奖励。

奖励使用 `RewardDef`（Identifier + CompoundTag）存储，与任务类型、条件系统保持一致的类型安全设计。

| 注册 ID | 实现类 | 说明 |
| :--- | :--- | :--- |
| `maple_banktrade:item` | `ItemReward` | 物品奖励：发放指定物品到背包，满则掉落地面 |

奖励定义示例：
```java
new RewardDef(MapleBankTrade.id("item"), new CompoundTag() {{
    putString("item", "minecraft:diamond");
    putInt("count", 3);
}});
```

### 2.4 Common层具体蓝图

`QuestBlueprints.java` 已包含以下 26 个示例任务：

| 任务ID | 类型 | 行为 | 说明 |
| :--- | :--- | :--- | :--- |
| `main_root` | MAIN | SIMPLE | 分组节点（根） |
| `main_forest` | MAIN | SIMPLE | 第一个主线任务 |
| `main_cave` | MAIN | SIMPLE | 第二个主线（链式） |
| `main_castle` | MAIN | SIMPLE | 第三个主线（链式+脚本） |
| `side_village` | SIDE | SIMPLE | 支线（依赖+可见脚本） |
| `side_village_collect` | SIDE | SIMPLE | 支线后继（链式） |
| `side_hermit` | SIDE | SIMPLE | 支线（依赖+可见脚本） |
| `temp_fishing` | TEMPORARY | SIMPLE | 可循环5次 |
| `temp_hunting` | TEMPORARY | SIMPLE | 无限循环 |
| `daily_mining` | TEMPORARY | SIMPLE | 每日1次 |
| `multi_kill` | SIDE | MULTI_COMPLETION | 需完成5次 |
| `random_pool_task` | SIDE | RANDOM_POOL | 单次随机池 |
| `multi_random_pool` | SIDE | MULTI_RANDOM_POOL | 多次随机池 |
| `hidden_quest` | TEMPORARY | HIDDEN_CHAIN | 30%概率触发后继 |
| `random_activate` | TEMPORARY | RANDOM_ACTIVATE | 可重复随机触发 |
| `side_any_dep` | SIDE | SIMPLE | 🆕 ONE_COMPLETED 依赖模式 |
| `submit_diamonds` | TEMPORARY | SIMPLE | 🆕 提交3个钻石（submit_item） |
| `reward_demo` | TEMPORARY | SIMPLE | 🆕 完成获绿宝石+经验瓶（item奖励） |
| `side_after_kills` | SIDE | SIMPLE | 占位：multi_kill 后解锁 |
| `final_reward` | SIDE | SIMPLE | 占位：multi_random_pool 后解锁 |
| `secret_quest_2` | TEMPORARY | SIMPLE | 占位：hidden_quest 概率触发 |
| `sub_task_a` | SIDE | SIMPLE | 占位：random_pool_task 池子 |
| `sub_task_b` | SIDE | SIMPLE | 占位：random_pool_task 池子 |
| `sub_task_c` | SIDE | SIMPLE | 占位：random_pool_task 池子 |
| `sub_task_x` | SIDE | SIMPLE | 占位：multi_random_pool 池子 |
| `sub_task_y` | SIDE | SIMPLE | 占位：multi_random_pool 池子 |

### 2.5 仓储实现

#### 2.5.1 `InMemoryQuestRepository`

完整的内存存储实现，适用于开发调试：

- ✅ 定义加载（`loadDefinitions`）
- ✅ 定义查询（`getDefinition`, `getRoots`, `getChildren`, `getAllDefinitions`, `getDefinitionsByType`）
- ✅ 状态管理（`getOrCreateState`, `getAllStates`, `saveState`）
- ✅ 历史管理（`getCompletionRecords`, `addCompletionRecord`, `pruneRecords`）
- ✅ 预构建 children 索引（`buildIndex`），`getChildren()` O(1) 查询

#### 2.5.2 `QuestDefinitionRegistry`（全局静态定义注册表）

> 🆕 v3.5 新增

所有玩家共享的只读任务图，服务器启动时初始化一次。彻底分离可变内容与不可变内容：

- ✅ 持有所有 `ITaskDefinition`（从 `QuestBlueprints` 注入）
- ✅ 提供按 ID、类型、父子关系的查询方法
- ✅ 与 `PlayerQuestData` 物理隔离：定义是静态的（注册表），状态是动态的（PlayerQuestData）
- ✅ `PlayerQuestData` 的定义查询方法全部委托给注册表

```
┌─────────────────────────────────────────────────┐
│     QuestDefinitionRegistry（全局静态，不可变）   │
│     Map<String, ITaskDefinition>  — 26 个蓝图    │
│     所有玩家共享，服务器启动时初始化一次            │
└─────────────────────────────────────────────────┘
                    │
                    │ 引用（委托查询）
                    ▼
┌─────────────────────────────────────────────────┐
│     PlayerQuestData（每个 UUID 一份，可变）       │
│     states:  Map<String, BaseTaskState>          │
│     history: Map<String, List<Record>>           │
│     ❌ 不再持有 definitions                      │
└─────────────────────────────────────────────────┘
```

#### 2.5.3 `PlayerQuestData`

玩家持久化数据，**仅存储可变内容**（状态 + 完成历史），参考 `BankCardsWorldData` 的 Codec 序列化模式，实现 `IQuestRepository`：

- ✅ Codec 序列化（宽松读取 + 严格写入，保证存档兼容性）
- ✅ 脏标记回调（`dirtyCallback`），可对接 `SavedData::setDirty`
- ✅ 完成记录存储（`addCompletionRecord`）和裁剪（`pruneRecords`）
- ✅ 状态管理（`getOrCreateState`, `saveState`）
- ✅ 定义查询委托给 `QuestDefinitionRegistry`（不再持有 `definitions` Map）

#### 2.5.4 `QuestSavedData`（存储层挂载）

世界持久化任务存储，参考 `BankCardsWorldData` 模式挂载到 NeoForge `SavedData` 体系：

- ✅ 继承 `SavedData`，通过 `SavedDataType` 注册到 `ServerLevel#getDataStorage()`
- ✅ 内部存储 `Map<UUID, PlayerQuestData>`，每个 UUID 独立存储一份任务数据
- ✅ 宽松读取（跳过无法解析的 UUID 条目，保证存档兼容性）
- ✅ 脏标记自动传播：`PlayerQuestData` 修改 → `QuestSavedData` 标记脏 → 服务器自动保存
- ✅ 存储文件：`data/maple_banktrade/quest_data.dat`
- 🆕 v3.5：不再管理蓝图注入，任务定义由 `QuestDefinitionRegistry` 全局持有

#### 2.5.5 `QuestDataHelper`（统一 UUID 入口）

类似 `BankHelper` 的统一入口工具类，封装所有存储层操作。**不绑定 Player 实例**，支持任意 UUID 传入：

- ✅ `getOrCreateStorage(MinecraftServer)` — 获取或创建 `QuestSavedData`
- ✅ `getOrCreate(MinecraftServer, UUID)` — 按 UUID 获取或创建任务数据（不绑定 Player）
- ✅ `getOrCreate(ServerPlayer)` — 从 ServerPlayer 提取 UUID 后委托上述方法
- ✅ `modifyStorage(MinecraftServer, Consumer)` — 批量修改存储（自动标记脏）
- ✅ `remove(MinecraftServer, UUID)` — 移除指定 UUID 的任务数据
- 🆕 v3.5：不再需要 `initBlueprints()`，蓝图初始化由 `QuestDefinitionRegistry.init()` 处理

> **进度粒度控制**：当前默认使用玩家 UUID（个人进度）。后续如需改为团队进度，只需在调用处传入团队 UUID 即可：
> ```java
> // 当前（个人进度）：
> QuestDataHelper.getOrCreate(server, player.getUUID());
> // 后续（团队进度）：
> QuestDataHelper.getOrCreate(server, teamUuid);
> ```

### 2.6 各层组件（53 个类）

| 包 | 类 | 职责 | 关键 API |
|:---|:---|:---|:---|
| `api.quests` | `QuestDefinitionRegistry` | 🆕 v3.5 全局静态定义注册表 | `init(supplier)`, `getDefinition(id)`, `getRoots()`, `getChildren()` |
| `condition` | `IScriptEvaluator` | 条件求值接口 | `evaluate(conditionId, params, taskId)`, `noOp()` |
| `condition` | `RegistryScriptEvaluator` | 基于注册表的条件评估器 | `evaluate(conditionId, params, taskId)` |
| `condition` | `QuestConditionRegistry` | 条件注册表（仿 MachineTradeHookRegistry） | `register(id, factory)`, `getCondition(id, config)` |
| `condition` | `BaseQuestCondition` | 条件基类 + `ConditionFactory` 接口 | `evaluate(context)`, `evaluate(EvaluationContext)`, `fromTag(tag)` |
| `condition` | `EvaluationContext` | 🆕 类型化评估上下文 | `of(player)`, `of(level)`, `getPlayer()`, `getLevel()` |
| `condition` | `CompositeCondition` | 🆕 组合条件（AND/OR/NOT） | `AndCondition`, `OrCondition`, `NotCondition` |
| `condition` | `LevelCondition` | 等级条件 | `fromTag(tag)` → 存储 int 阈值 |
| `condition` | `HealthCondition` | 血量条件 | `fromTag(tag)` → 存储 float 阈值 |
| `condition` | `HasItemCondition` | 持有物品条件 | `fromTag(tag)` → 存储 Item 实例 |
| `condition` | `HasPotionEffectCondition` | 药水效果条件 | `fromTag(tag)` → 存储 Holder<MobEffect> |
| `condition` | `IsRainingCondition` | 天气条件 | `fromTag(tag)` → 无状态 |
| `condition` | `ResolutionContext` | 封装仓储快照 + 条件引擎 | `isStrictlyCompleted()`, `isEffectivelyFinished()`, `evaluateCondition()` |
| `calculator` | `StateDelta` | 状态变更记录 DTO | `isChanged()`, `of()`, `unchanged()` |
| `calculator` | `VisibilityCalculator` | 核心可见性算法（6 层条件短路） | `resolveStatus(taskId, context)` |
| `calculator` | `TreeTraversalService` | 拓扑排序、路径查询、兄弟链排序 | `topologicalSort()`, `getDescendants()`, `getSiblingChain()` |
| `calculator` | `StateTransitionOrchestrator` | 完成事件处理、按 6 种行为差异化流转 | `processComplete()`, `processActivate()`, `tryTriggerHiddenChain()` |
| `scheduler` | `CooldownResetService` | 冷却/每日重置、临时任务随机触发 | `processCooldownResets()`, `tryRandomTempTask()` |
| `scheduler` | `QuestTriggerHandler` | 触发层：定时刷新 + 登录/登出事件 | `init()`, `onServerTick()`, `onPlayerLogin()` |
| `storage` | `QuestSavedData` | 世界持久化存储（SavedData 挂载） | `getOrCreate(uuid)`, `remove(uuid)` |
| `storage` | `QuestDataHelper` | 统一 UUID 入口（类似 BankHelper） | `getOrCreateStorage(server)`, `getOrCreate(server, uuid)` |
| `storage` | `QuestDataManager` | 任务数据管理器（委托存储层） | `getOrCreate(player)`, `getEvaluator(player)`, `remove(uuid)` |
| `tasktype` | `ITaskType` | 🆕 任务完成类型接口 | `canComplete(def, repo, context)`, `onComplete(def, repo, context)` |
| `tasktype` | `TaskTypeRegistry` | 🆕 任务类型注册表 | `register(id, type)`, `get(id)` |
| `tasktype` | `ConfirmTaskType` | 🆕 确认完成类型 | 无额外检查，点击即完成 |
| `tasktype` | `SubmitItemTaskType` | 🆕 提交物品类型 | 检查背包物品数量，完成时扣除 |
| `reward` | `RewardDef` | 🆕 v3.5 奖励定义数据类 | `typeId()`, `params()`, `of(typeId)` |
| `reward` | `IReward` | 🆕 奖励接口 | `grant(params, context)` |
| `reward` | `RewardRegistry` | 🆕 奖励注册表 | `register(id, factory)`, `grantRewards(rewards, context)` |
| `reward` | `ItemReward` | 🆕 物品奖励 | 发放物品到背包，满则掉落 |
| `ui` | `QuestUIRegistration` | 🆕 v3.6 主 UI 注册 | `PlayerUIMenuType.register()` + TabView 组装 |
| `ui` | `QuestUIStylesheets` | 🆕 v3.6 样式表加载 | 加载 `quest.lss` 样式表 |
| `ui` | `QuestUiHelper` | 🆕 v3.6 S2C 数据工具 | `buildTaskListSnapshot()`, `buildTaskDetailSnapshot()`, 格式化 |
| `ui` | `QuestTaskListPanel` | 🆕 v3.6 左栏任务列表 | 按类型分组 + 选中回调 |
| `ui` | `QuestTaskDetailPanel` | 🆕 v3.6 右栏任务详情 | 完整信息 + 接取/完成按钮 |
| `ui` | `QuestCompletedPanel` | 🆕 v3.6 已完成标签页 | 显示完成记录和进度 |
| `ui` | `QuestTreePanel` | 🆕 v3.6 树状结构标签页 | 递归渲染完整任务树（创造模式） |

#### 2.6.1 条件系统设计

条件系统仿照 `MachineTradeHookRegistry` 模式，使用 `Identifier` + `CompoundTag` 替代字符串脚本：

- **条件注册**：`QuestConditionRegistry.register(id, ConditionFactory)`，`Identifier` 索引工厂
- **条件创建**：`ConditionFactory.create(CompoundTag)` 从 Tag 参数创建条件实例，存储实际 Minecraft 实例（Item、MobEffect 等）
- **条件评估**：`BaseQuestCondition.evaluate(Object context)` 接收 Player 或 ServerLevel；`evaluate(EvaluationContext ctx)` 提供类型化上下文
- **默认回退**：未注册的 ID 自动回退到 `ALWAYS_PASS`（无条件通过）

预注册条件及蓝图映射：

| 条件 ID | 条件类 | 类别 | 参数 | 蓝图使用 |
|:---|:---|:---|:---|:---|
| `maple_banktrade:level_ge` | LevelCondition | 原子 | `{level:1}` | `main_forest` unlock |
| `maple_banktrade:has_item` | HasItemCondition | 原子 | `{item:"minecraft:emerald"}` | `main_castle` unlock |
| `maple_banktrade:has_effect` | HasPotionEffectCondition | 原子 | `{effect:"regeneration"}` | `side_hermit` visibility |
| `maple_banktrade:is_raining` | IsRainingCondition | 原子 | (无) | `temp_fishing` visibility |
| `maple_banktrade:low_health` | HealthCondition | 原子 | `{health:10.0}` | `temp_hunting` visibility |
| `maple_banktrade:and` | CompositeCondition.AndCondition | 🆕 组合 | `{conditions:[{id,params},...]}` | 所有子条件满足 |
| `maple_banktrade:or` | CompositeCondition.OrCondition | 🆕 组合 | `{conditions:[{id,params},...]}` | 任一子条件满足 |
| `maple_banktrade:not` | CompositeCondition.NotCondition | 🆕 组合 | `{condition:{id,params}}` | 反转子条件 |

### 2.7 拓扑验证

`QuestRepositoryLoader` 加载时自动执行拓扑验证：

- ✅ 自引用检测（parentId / dependentNodes 指向自身）
- ✅ 孤儿节点检测（父节点、兄弟链、依赖、池引用）
- ✅ 三色 DFS 环检测
- ✅ 行为模式与配置一致性检查

### 2.8 UI 层（v3.6 新增）

> 🆕 v3.6 新增：基于 LDLib2 的任务 UI 系统

#### 2.8.1 布局设计

采用两栏布局，通过 `PlayerUIMenuType` 注册，样式表从 `assets/maple_banktrade/lss/quest.lss` 加载。

```
┌─────────────────────────────────────────────────────┐
│  [任务]  [已完成]  [🌳结构(创造)]                    │  ← TabView 标签
├──────────────┬──────────────────────────────────────┤
│  任务列表     │        任务详情                       │
│  (30%)       │        (70%)                         │
│              │                                      │
│  按类型分组   │  基本信息 / 任务链 / 依赖 / 条件      │
│  MAIN/支线/  │  奖励 / 操作按钮（接取/完成）         │
│  临时        │                                      │
└──────────────┴──────────────────────────────────────┘
```

#### 2.8.2 标签页

| 标签页 | 实现类 | 说明 |
|:---|:---|:---|
| 任务 | `QuestTaskListPanel` + `QuestTaskDetailPanel` | 左栏按类型分组列表 + 右栏详情与操作按钮 |
| 已完成 | `QuestCompletedPanel` | 显示所有有完成记录的任务及进度 |
| 🌳结构 | `QuestTreePanel` | 递归渲染完整树状结构（仅创造模式可见） |

#### 2.8.3 数据通道

| 通道 | 方向 | 实现 | 说明 |
|:---|:---|:---|:---|
| 任务列表同步 | S2C | `DataBindingBuilder.tagS2C()` | 服务端构建 `buildTaskListSnapshot()` |
| 任务详情同步 | S2C | `DataBindingBuilder.tagS2C()` | 选中任务后触发 `buildTaskDetailSnapshot()` |
| 已完成列表 | S2C | `DataBindingBuilder.tagS2C()` | 标签页切换时刷新 |
| 接取任务 | C2S | `Button.setOnServerClick()` | 调用 `StateTransitionOrchestrator.processActivate()` |
| 完成任务 | C2S | `Button.setOnServerClick()` | 调用 `StateTransitionOrchestrator.processComplete()` |

#### 2.8.4 物品触发

通过 `QuestBookAttachment`（仿 `WalletAttachment`）绑定到任务书物品，右键打开 UI：

```java
// 服务端打开
QuestUIRegistration.openUI(serverPlayer);
```

#### 2.8.5 样式表

`quest.lss` 资源位于 `assets/maple_banktrade/lss/quest.lss`，包含：
- 两栏布局（`.mbt-quest-content`）
- 任务列表项（`.mbt-quest-list-item` / `-selected` / `-completed` / `-locked`）
- 详情分区（`.mbt-quest-detail-section` / `-title` / `-row` / `-actions`）
- 树节点（`.mbt-quest-tree-node`）
- 已完成列表（`.mbt-quest-completed-panel` / `-row`）


## 三、任务系统核心概念

### 3.1 任务连接方式（6种）

| 连接方式 | 字段 | 作用域 | 检查方式 | 用途 |
| :--- | :--- | :--- | :--- | :--- |
| **父子层级** | `parentId` | 树结构 | 父隐藏→子隐藏 | 组织树状结构 |
| **兄弟链式** | `prevSiblingId` | 同父兄弟 | 严格检查当前 COMPLETED | 顺序推进 |
| **额外依赖** | `dependentNodes` | 任意节点 | 按 `dependencyRequirement` 模式判断 | 多条件解锁 |
| **跨链跳转** | `nextTaskInChain` | 任意节点 | 完成后触发 | 强制剧情推进 |
| **动态条件** | `visibilityConditionId`/`unlockConditionId` | 运行时 | 条件引擎评估 | 动态条件门控 |
| **循环重置** | `repeatable`+`maxRepeatTimes` | 自身 | 完成后判断 | 重复完成 |

### 3.2 状态流转图

```
                     ┌─────────────────────────────────────────────┐
                     │                 外部触发                    │
                     │  (随机抽取 / 逻辑层调度)                    │
                     ▼                                             │
┌────────┐ 依赖满足  ┌─────────────────┐  玩家接取   ┌─────────┐  │
│ HIDDEN │ ────────► │ VISIBLE_LOCKED │ ─────────► │ ACTIVE  │  │
└────────┘           └─────────────────┘            └─────────┘  │
     ▲                       │                           │        │
     │                       │                           │        │
     │              强制父可见穿透                      进度达标     │
     │                       │                           │        │
     │                       ▼                           ▼        │
     │              ┌─────────────────────────────────────────┐   │
     └──────────────│              COMPLETED                 │   │
                    │                                         │   │
                    │  循环任务未达上限 → 重置为 LOCKED       │   │
                    │  循环任务达上限   → 永久 COMPLETED      │   │
                    │  随机激活任务     → 重置为 HIDDEN ──────┘   │
                    │  隐藏链任务       → COMPLETED（不触发后继）│
                    └─────────────────────────────────────────┘
```

### 3.3 各行为模式的状态流转差异

| 行为 | 完成后状态 | 触发后继时机 | 特殊逻辑 |
| :--- | :--- | :--- | :--- |
| `SIMPLE` | COMPLETED | 立即触发 | 无 |
| `MULTI_COMPLETION` | VISIBLE_LOCKED | 记录数 ≥ requiredCompletions | 未达上限不触发后继 |
| `RANDOM_POOL` | COMPLETED | 子任务完成时 | 激活时随机选子任务 |
| `MULTI_RANDOM_POOL` | VISIBLE_LOCKED | 记录数 ≥ requiredCompletions | 每次激活随机选子任务 |
| `HIDDEN_CHAIN` | COMPLETED | 概率触发（不立即） | 外部调用触发 |
| `RANDOM_ACTIVATE` | HIDDEN | 不触发 | 不添加完成记录 |


## 四、未完成内容（待实现）

### 4.1 测试框架（优先级：低）

建议为逻辑层编写纯 JUnit 测试。


## 五、后续开发计划（Roadmap）

### 5.1~5.4 Phase 1~4（✅ 已完成）

核心数据层、逻辑层、条件引擎、存储层、触发层、UI 层均已实现。

### 5.5 Phase 5：UI集成（✅ 已完成 v3.6）

| 任务 | 描述 | 状态 |
| :--- | :--- | :--- |
| 实现任务UI树渲染 | 显示四态节点，左栏30%列表 + 右栏70%详情 | ✅ `QuestTaskListPanel` + `QuestTaskDetailPanel` |
| 实现任务详情面板 | 基本信息、任务链、依赖、条件、奖励 | ✅ `QuestTaskDetailPanel` |
| 实现任务接取交互 | LOCKED→ACTIVE | ✅ `Button.setOnServerClick()` → `processActivate()` |
| 实现任务完成交互 | ACTIVE→COMPLETED | ✅ `Button.setOnServerClick()` → `processComplete()` |
| 实现已完成标签页 | 显示完成记录和进度 | ✅ `QuestCompletedPanel` |
| 实现树状结构标签页 | 创造模式完整任务树 | ✅ `QuestTreePanel` |
| 实现样式表 | LSS 两栏布局 + 列表 + 详情 + 树 | ✅ `quest.lss` |

### 5.6 Phase 6：配置系统（1周）

| 任务 | 描述 | 优先级 |
| :--- | :--- | :--- |
| 实现 JSON 数据包加载 | 替代硬编码蓝图 | **P1** |
| 实现热重载 | 开发期调试便利 | **P2** |

### 5.7 Phase 7：任务类型扩展（后续）

| 任务 | 描述 | 优先级 |
| :--- | :--- | :--- |
| 实现 `KillTaskType` | 击杀实体任务类型 | **P2** |
| 实现 `CraftTaskType` | 合成物品任务类型 | **P2** |
| 实现 `DimensionTaskType` | 维度到达任务类型 | **P2** |

### 5.8 Phase 8：奖励系统扩展（后续）

| 任务 | 描述 | 优先级 |
| :--- | :--- | :--- |
| 实现 `XpReward` | 经验值奖励 | **P2** |
| 实现 `CommandReward` | 命令执行奖励 | **P2** |


## 六、扩展性保障

### 6.1 已有扩展点

| 扩展点 | 类型 | 用途 |
| :--- | :--- | :--- |
| `QuestDefinitionRegistry` | 静态注册表 | 🆕 v3.5 全局任务定义注册表（定义/状态分离） |
| `ITaskDefinition` | 接口 | 自定义任务定义（新增字段） |
| `ITaskState` | 接口 | 自定义状态存储（新增字段） |
| `ICompletionRecord` | 接口 | 自定义历史记录（新增快照数据） |
| `IQuestRepository` | 接口 | 自定义存储（数据库/Redis） |
| `TaskBehavior` | 枚举 | 新增行为模式 |
| `TaskType` | 枚举 | 新增任务类型 |
| `DependencyRequirement` | 枚举 | 新增依赖模式 |
| `IScriptEvaluator` | 函数式接口 | 自定义条件引擎 |
| `BaseQuestCondition` | 抽象类 | 新增条件类型（继承 + `fromTag`） |
| `QuestConditionRegistry` | 静态注册表 | 注册自定义条件工厂 |
| `ITaskType` | 接口 | 🆕 新增任务完成类型 |
| `TaskTypeRegistry` | 静态注册表 | 🆕 注册自定义任务完成类型 |
| `IReward` | 接口 | 🆕 新增奖励类型 |
| `RewardRegistry` | 静态注册表 | 🆕 注册自定义奖励工厂 |
| `RewardDef` | record | 🆕 v3.5 奖励定义数据类（Identifier + CompoundTag） |

### 6.2 向后兼容性保障

- 新增字段均有默认值（`SIMPLE`、`requiredCompletions=1`、`nextChainTriggerChance=1.0`、`ALL_COMPLETED`）
- 旧蓝图无需修改即可正常运行
- `ITaskInstance.isEffectivelyFinished()` 对 `RANDOM_ACTIVATE` 做了特殊处理，不影响旧逻辑


## 七、使用示例

### 7.1 定义自定义任务蓝图

```java
ITaskDefinition customQuest = new BaseTaskDefinition.Builder()
        .id("my_custom_quest")
        .type(TaskType.SIDE)
        .behavior(TaskBehavior.MULTI_COMPLETION)
        .requiredCompletions(10)
        .parentId("main_root")
        .dependentNodes(Arrays.asList("main_castle"))
        .dependencyRequirement(DependencyRequirement.ONE_COMPLETED)  // 🆕 依赖模式
        .nextTaskInChain("my_final_reward")
        .repeatable(false)
        .unlockConditionId(MapleBankTrade.id("level_ge"))
        .unlockConditionParams(new CompoundTag() {{ putInt("level", 20); }})
        .taskTypeId(MapleBankTrade.id("submit_item"))                 // 🆕 任务完成类型
        .taskTypeParams(new CompoundTag() {{ putString("item", "minecraft:diamond"); putInt("count", 5); }})
        .rewards(List.of(                                                // 🆕 奖励（Identifier + CompoundTag）
            new RewardDef(MapleBankTrade.id("item"), new CompoundTag() {{
                putString("item", "minecraft:emerald");
                putInt("count", 3);
            }})))
        .build();
```

### 7.2 完成一个任务（逻辑层调用）

```java
// 构造上下文（含条件引擎）
ResolutionContext context = new ResolutionContext(
    repository, IScriptEvaluator.noOp(), repository.getAllStates());

// 完成处理（传入运行时上下文用于任务类型检查和奖励发放）
List<StateDelta> deltas = StateTransitionOrchestrator.processComplete(
    "main_forest", context, gameTime, player);

// 应用变更
for (StateDelta delta : deltas) {
    if (delta.isChanged()) {
        ITaskState state = repository.getOrCreateState(delta.getTaskId());
        state.setStatus(delta.getNewStatus());
        repository.saveState(state);
    }
}
```

### 7.3 使用 EvaluationContext 评估条件

```java
// 创建类型化评估上下文
EvaluationContext ctx = EvaluationContext.of(player);

// 评估条件
boolean result = condition.evaluate(ctx);  // 类型化评估
boolean result = condition.evaluate((Object) player);  // 传统评估（兼容）
```

### 7.4 使用组合条件

```java
// AND 条件：等级 ≥ 5 且持有钻石
CompoundTag andParams = new CompoundTag();
ListTag conditions = new ListTag();
// 子条件 1: level_ge
CompoundTag cond1 = new CompoundTag();
cond1.putString("id", "maple_banktrade:level_ge");
cond1.put("params", new CompoundTag() {{ putInt("level", 5); }});
conditions.add(cond1);
// 子条件 2: has_item
CompoundTag cond2 = new CompoundTag();
cond2.putString("id", "maple_banktrade:has_item");
cond2.put("params", new CompoundTag() {{ putString("item", "minecraft:diamond"); }});
conditions.add(cond2);
andParams.put("conditions", conditions);

BaseQuestCondition andCondition = QuestConditionRegistry.getCondition(
    MapleBankTrade.id("and"), andParams);
boolean result = andCondition.evaluate(EvaluationContext.of(player));
```

### 7.5 使用 QuestDefinitionRegistry（全局定义注册）

```java
// 服务器启动时初始化任务定义注册表（在 CommonInit.onServerStarted 中调用）
QuestDefinitionRegistry.init(QuestBlueprints::getAllBlueprints);

// 任意位置查询定义
ITaskDefinition def = QuestDefinitionRegistry.getDefinition("main_forest");
List<ITaskDefinition> roots = QuestDefinitionRegistry.getRoots();
List<ITaskDefinition> children = QuestDefinitionRegistry.getChildren("main_root");
```

### 7.6 使用 QuestDataHelper（存储层挂载）

```java
// 按 UUID 获取任务数据（不绑定 Player 实例）
PlayerQuestData data = QuestDataHelper.getOrCreate(server, playerUUID);

// 从 ServerPlayer 获取
PlayerQuestData data = QuestDataHelper.getOrCreate(serverPlayer);

// 批量修改存储
QuestDataHelper.modifyStorage(server, storage -> {
    for (UUID uuid : storage.getAllUuids()) {
        PlayerQuestData pd = storage.get(uuid);
        // 执行批量操作...
    }
});
```


## 八、版本历史

| 版本 | 日期 | 变更内容 |
| :--- | :--- | :--- |
| 1.0 | 2026-08-08 | 初始架构：核心接口、基础实现、仓储、蓝图示例 |
| 1.1 | 2026-08-08 | 新增 `TaskBehavior` 枚举及6种行为模式；扩展 `ITaskDefinition`；更新蓝图示例 |
| 2.0 | 2026-08-08 | 生成完整架构文档，明确已完成/未完成内容及开发计划 |
| 2.1 | 2026-08-08 | API 层 + Common 层优化：Lombok 简化、便利方法、空安全、拓扑验证 |
| 3.0 | 2026-08-08 | 逻辑层全部实现（7 类）；存储层 `PlayerQuestData` 实现；包结构重组为 `core/`/`enums/`/`impl/`/`logic/`
| 3.1 | 2026-08-09 | 条件系统重构：`visibilityScript`/`unlockScript` 替换为 `Identifier`+`CompoundTag`；新增 6 种条件子类 + `QuestConditionRegistry` + `RegistryScriptEvaluator`；`IScriptEvaluator` 签名更新；条件存储实际 Minecraft 实例
| 3.2 | 2026-08-09 | 存储层挂载完成：`QuestSavedData` 继承 `SavedData` 挂载到服务器；`QuestDataHelper` 统一 UUID 入口；触发层完成：`QuestTriggerHandler` 定时刷新 + `QuestDataManager` 委托存储层；`CommonInit` 服务器启动时注入蓝图
| 3.3 | 2026-08-09 | 包结构优化：移除 `logic/` 父层；合并单文件子包（orchestrator/result/traversal → calculator，scheduler/trigger → scheduler）；`impl/` 分离出 `repository/`；共 8 个顶层包，34 个文件
| 3.4 | 2026-08-09 | 🆕 四大模块优化：①任务完成类型系统（`ITaskType` + `TaskTypeRegistry` + `ConfirmTaskType` + `SubmitItemTaskType`）；②奖励系统（`IReward` + `RewardRegistry` + `ItemReward`）；③依赖模式（`DependencyRequirement` 枚举，`VisibilityCalculator` 支持四种模式）；④条件系统优化（`EvaluationContext` 类型化上下文 + `CompositeCondition` AND/OR/NOT 组合条件）。共 10 个顶层包，44 个文件 |
| 3.5 | 2026-08-09 | 🆕 定义/状态分离：①新增 `QuestDefinitionRegistry` 全局静态定义注册表，所有玩家共享只读任务图；②`PlayerQuestData` 移除 `definitions` 字段，仅存储可变状态和历史；③`QuestSavedData` 不再管理蓝图注入；④`QuestDataHelper` 移除 `initBlueprints()`；⑤`CommonInit` 改用 `QuestDefinitionRegistry.init()`。共 11 个顶层包，45 个文件 |


## 九、附录

### 附录A：当前代码文件清单

```
com.maple.maple_banktrade.api.quests/         # 12 个顶层包，53 个文件
├── README.md
├── QuestDefinitionRegistry.java               🆕 v3.5
├── enums/
│   ├── DependencyRequirement.java           🆕 v3.4
│   ├── TaskStatus.java                      ✅
│   ├── TaskType.java                        ✅
│   └── TaskBehavior.java                    ✅
├── core/
│   ├── ICompletionRecord.java               ✅
│   ├── IQuestRepository.java                ✅
│   ├── ITaskDefinition.java                 ✅ (v3.4 新增 4 方法)
│   ├── ITaskInstance.java                   ✅
│   └── ITaskState.java                      ✅
├── impl/
│   ├── BaseCompletionRecord.java            ✅
│   ├── BaseTaskDefinition.java              ✅ (v3.4 新增 4 字段)
│   ├── BaseTaskInstance.java                ✅
│   ├── BaseTaskInstanceFactory.java         ✅
│   └── BaseTaskState.java                   ✅
├── repository/
│   ├── InMemoryQuestRepository.java         ✅
│   └── PlayerQuestData.java                 ✅ (v3.5 移除 definitions 字段)
├── condition/
│   ├── IScriptEvaluator.java                ✅
│   ├── RegistryScriptEvaluator.java         ✅ (v3.4 适配 EvaluationContext)
│   ├── QuestConditionRegistry.java          ✅ (v3.4 注册 3 个组合条件)
│   ├── ResolutionContext.java               ✅
│   ├── EvaluationContext.java               🆕 v3.4
│   ├── BaseQuestCondition.java              ✅ (v3.4 新增 evaluate(EvaluationContext))
│   ├── LevelCondition.java                  ✅
│   ├── HealthCondition.java                 ✅
│   ├── HasItemCondition.java                ✅
│   ├── HasPotionEffectCondition.java        ✅
│   ├── IsRainingCondition.java              ✅
│   └── CompositeCondition.java              🆕 v3.4
├── calculator/
│   ├── StateDelta.java                      ✅
│   ├── VisibilityCalculator.java            ✅ (v3.4 新增 areDependenciesSatisfied)
│   ├── TreeTraversalService.java            ✅
│   └── StateTransitionOrchestrator.java     ✅ (v3.4 集成任务类型+奖励)
├── scheduler/
│   ├── CooldownResetService.java            ✅
│   └── QuestTriggerHandler.java             ✅
├── storage/
│   ├── QuestSavedData.java                  ✅ (v3.5 移除蓝图注入)
│   ├── QuestDataHelper.java                 ✅ (v3.5 移除 initBlueprints)
│   └── QuestDataManager.java                ✅ (v3.4 适配 EvaluationContext)
├── tasktype/                                🆕 v3.4
│   ├── ITaskType.java                       🆕
│   ├── TaskTypeRegistry.java                🆕
│   ├── ConfirmTaskType.java                 🆕
│   └── SubmitItemTaskType.java              🆕
├── reward/                                  🆕 v3.4
│   ├── RewardDef.java                         🆕 v3.5
│   ├── IReward.java                         🆕
│   ├── RewardRegistry.java                  🆕 (v3.5 新增 grantRewards(List<RewardDef>))
│   └── ItemReward.java                      🆕
└── ui/                                      🆕 v3.6
    ├── QuestUIRegistration.java               🆕 v3.6
    ├── QuestUIStylesheets.java                🆕 v3.6
    ├── QuestUiHelper.java                     🆕 v3.6
    ├── QuestTaskListPanel.java                🆕 v3.6
    ├── QuestTaskDetailPanel.java              🆕 v3.6
    ├── QuestCompletedPanel.java               🆕 v3.6
    └── QuestTreePanel.java                    🆕 v3.6

com.maple.maple_banktrade.common.quests/
├── QuestBlueprints.java                     ✅ (26 个任务，含 3 个新功能示例 + 8 个占位)
└── QuestRepositoryLoader.java               ✅

resources/
└── assets/maple_banktrade/lss/
    └── quest.lss                              🆕 v3.6
```

### 附录B：关键算法伪代码

#### B.1 可见性计算（VisibilityCalculator）

```
function resolveStatus(taskId, context):
    def = context.getDefinition(taskId)
    state = context.getState(taskId)
    
    // 1. 已完成且非循环 → 永久完成
    if state.status == COMPLETED:
        if not def.repeatable or historyCount >= def.maxRepeatTimes:
            return COMPLETED
    
    // 2. 检查隐藏条件
    hidden = false
    
    // 2.1 父节点检查
    if def.parentId != null:
        parentStatus = context.getStatus(def.parentId)
        if parentStatus == HIDDEN:
            hidden = true
    
    // 2.2 兄弟链检查（严格完成）
    if not hidden and def.prevSiblingId != null:
        if not context.isStrictlyCompleted(def.prevSiblingId):
            hidden = true
    
    // 2.3 依赖检查（按 DependencyRequirement 模式）
    if not hidden and def.dependentNodes not empty:
        if not areDependenciesSatisfied(def, context):
            hidden = true
    
    // 2.4 解锁条件检查
    if not hidden and def.unlockConditionId != null:
        if not context.evaluateCondition(def.unlockConditionId, def.unlockConditionParams):
            hidden = true
    
    if hidden:
        return HIDDEN
    
    // 3. 可见性判断
    if state.status == ACTIVE:
        return ACTIVE
    
    // 4. 可见条件检查
    if def.visibilityConditionId != null:
        if not context.evaluateCondition(def.visibilityConditionId, def.visibilityConditionParams):
            return HIDDEN
    
    return VISIBLE_LOCKED

// 依赖检查（四种模式）
function areDependenciesSatisfied(def, context):
    deps = def.dependentNodes
    if deps is empty: return true
    switch def.dependencyRequirement:
        ALL_COMPLETED → deps.allMatch(isEffectivelyFinished)
        ONE_COMPLETED → deps.anyMatch(isEffectivelyFinished)
        ALL_STARTED   → deps.allMatch(status == ACTIVE || COMPLETED)
        ONE_STARTED   → deps.anyMatch(status == ACTIVE || COMPLETED)
```

#### B.2 完成处理（StateTransitionOrchestrator）

```
function processComplete(taskId, context, gameTime, runtimeContext):
    def = context.getDefinition(taskId)
    deltas = []
    
    // 0. 检查任务类型条件
    if def.taskTypeId != null:
        taskType = TaskTypeRegistry.get(def.taskTypeId)
        if taskType != null and not taskType.canComplete(def, repo, runtimeContext):
            return empty  // 不满足完成条件，拒绝
    
    // 1. 创建完成记录
    record = new CompletionRecord(taskId, historyCount + 1, currentTick)
    repository.addRecord(record)
    
    // 2. 根据行为更新状态
    switch def.behavior:
        case SIMPLE:
            state.status = COMPLETED
            triggerChain(taskId, context)
        case MULTI_COMPLETION:
            state.status = VISIBLE_LOCKED
            state.progress = 0
            if record.completionIndex >= def.requiredCompletions:
                triggerChain(taskId, context)
        case RANDOM_POOL:
            state.status = COMPLETED
        case MULTI_RANDOM_POOL:
            state.status = VISIBLE_LOCKED
            state.progress = 0
            if record.completionIndex >= def.requiredCompletions:
                triggerChain(taskId, context)
        case HIDDEN_CHAIN:
            state.status = COMPLETED
        case RANDOM_ACTIVATE:
            state.status = HIDDEN
            repository.removeRecord(record)
    
    // 3. 执行任务类型副作用（如扣除物品）
    if def.taskTypeId != null:
        taskType = TaskTypeRegistry.get(def.taskTypeId)
        if taskType != null:
            taskType.onComplete(def, repo, runtimeContext)
    
    // 4. 发放奖励
    if def.rewards not empty:
        RewardRegistry.grantRewards(def.rewards, runtimeContext)
    
    // 5. 递归重算子树
    recalculateSubtree(taskId, context)
    
    return deltas
```

### 附录C：变更日志

| 版本 | 日期 | 变更内容 |
| :--- | :--- | :--- |
| 3.6 | 2026-08-11 | 🆕 UI 层：①新增 `ui/` 包（7 个文件），基于 LDLib2 `PlayerUIMenuType` 实现两栏布局（30% 列表 + 70% 详情）；②`QuestTaskListPanel` 按 MAIN/SIDE/TEMPORARY 分组显示任务列表；③`QuestTaskDetailPanel` 显示完整任务信息 + 接取/完成按钮；④`QuestCompletedPanel` 已完成任务标签页；⑤`QuestTreePanel` 创造模式树状结构标签页；⑥`QuestUiHelper` S2C 数据快照构建 + 格式化；⑦`quest.lss` 样式表；⑧修复 8 个蓝图悬空引用；⑨`CommonInit` 注册 `QuestUIRegistration.init()`。共 12 个顶层包，53 个文件 |
| 3.5 | 2026-08-09 | 🆕 定义/状态分离：①新增 `QuestDefinitionRegistry` 全局静态定义注册表；②`PlayerQuestData` 移除 `definitions` 字段；③`QuestSavedData` 不再管理蓝图注入；④`QuestDataHelper` 移除 `initBlueprints()`；⑤`CommonInit` 改用 `QuestDefinitionRegistry.init()`。共 11 个顶层包，45 个文件 |
| 3.4 | 2026-08-09 | 🆕 四大模块优化：①任务完成类型系统（`ITaskType` + `TaskTypeRegistry` + `ConfirmTaskType` + `SubmitItemTaskType`）；②奖励系统（`IReward` + `RewardRegistry` + `ItemReward`）；③依赖模式（`DependencyRequirement` 枚举，`VisibilityCalculator` 支持四种模式）；④条件系统优化（`EvaluationContext` + `CompositeCondition` AND/OR/NOT）。共 10 个顶层包，44 个文件 |
| 3.3 | 2026-08-09 | 包结构优化：移除 `logic/` 父层；合并单文件子包（orchestrator/result/traversal → calculator，scheduler/trigger → scheduler）；`impl/` 分离出 `repository/` |
| 3.2 | 2026-08-09 | 完整实现 Phase 1~4 核心架构：任务定义、状态、历史、仓储、6 种行为、可见性计算、状态流转编排、树链拓扑、定期触发器、SavedData 存储、条件引擎 |
| 3.1 | 2026-08-08 | 初始文档：总体架构设计、包结构规划、核心类型定义、Roadmap 规划 |
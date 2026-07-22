# 交易基础框架（api.trade）

包路径：`com.maple.maple_banktrade.api.trade`

本包只定义复杂交易的**抽象协议**、阶段输入/结果与注册表，不绑定 UI、物品槽或具体价目。  
货币-物品的具体实现见 `com.maple.maple_banktrade.api.trade.currency_item`。

## 设计目标

交易拆成三个阶段：

1. **check**：预检查与计划组装，判断是否可执行。
2. **execute**：按计划提交主交易（真实资源变动）。
3. **afterSuccess**：仅在主交易成功后的副作用，不影响主结果。

把「能不能做」「提交」「成功后处理」分开，避免计算与事务混在一个方法里。

## 包结构

```text
api.trade
├─ context
│  ├─ TradeContext.java       # 运行时上下文根接口
│  ├─ TradePlan.java          # 检查阶段产出的计划
│  └─ TradeRequest.java       # 请求参数根接口
├─ input
│  ├─ TradeCheckInput.java    # context + request
│  ├─ TradeExecuteInput.java  # + plan
│  └─ TradeSuccessInput.java  # + executionResult
├─ result
│  ├─ TradeCheckResult.java   # plan + messages
│  └─ TradeExecuteResult.java # success + detail + messages
├─ stage
│  ├─ TradeChecker.java
│  ├─ TradeExecutor.java
│  └─ TradeSuccessHandler.java
├─ definition
│  ├─ TradeDefinition.java
│  ├─ FunctionalTradeDefinition.java
│  └─ TradeRunner.java        # check → execute → afterSuccess
└─ registry
   ├─ TradeType.java
   ├─ TradeStorage.java
   ├─ TradeEntryStorage.java
   ├─ AbstractTradeEntryStorage.java
   ├─ TradeInfo.java
   └─ TradeRegistry.java      # tradeTypeId → TradeStorage
```

本包**不依赖** `bank` 或内置价目。

## 交易类型与存储器

```text
tradeTypeId → TradeStorage（实例）
```

```java
// 注册类型并得到空存储器
S storage = TradeRegistry.registerType(myType);
storage.register(tradeId, entry);

// 查询（requireStorage 当前实现：找不到返回 null，不抛异常）
TradeRegistry.findStorage(typeId, MyTradeStorage.class);
TradeRegistry.requireStorage(typeId, MyTradeStorage.class);
```

同一容器实现可对应多个 `TradeType` 实例（多个 ID），各自独立存条目。

## 标准运行流程

```java
TradeExecuteResult<D> result = TradeRunner.run(definition, context, request);
```

等价于：

```java
var checkInput = TradeCheckInput.of(context, request);
var checkResult = definition.check(checkInput);
if (checkResult.denied()) {
    return TradeExecuteResult.failure(null, checkResult.messages());
}
var executeInput = TradeExecuteInput.from(checkInput, checkResult.plan());
var executeResult = definition.execute(executeInput);
if (executeResult.success()) {
    definition.afterSuccess(TradeSuccessInput.from(executeInput, executeResult));
}
return executeResult;
```

### 数据传递链

```text
context + request
    → check → plan + check messages

context + request + plan
    → execute → success/detail + execute messages

context + request + plan + execution result
    → afterSuccess
```

约束：

- `check` 宜只做预演与组计划，不提交主交易（具体实现可选择轻量 check）。
- `execute` 只在 `checkResult.allowed()` 时调用。
- `afterSuccess` 只在 `executeResult.success()` 时调用，且不否决主结果。
- `messages()` 供调用方选用；**框架与当前钱包 UI 均不强制向玩家展示**。

## 核心类型说明

| 类型 | 用途 |
|------|------|
| `TradeContext` | 玩家、服务器、卡 UUID、storage 等运行时对象 |
| `TradeRequest` | 买卖模式、数量、源槽等触发参数 |
| `TradePlan` | check 算出的可执行计划（含 `executable()`） |
| `TradeDefinition` | 三阶段接口组合 |
| `FunctionalTradeDefinition` | 用三个函数快速组装定义 |

## 实现建议

- 不可变预计算结果放 `TradePlan`。
- 运行时对象放 `TradeContext`。
- 触发参数放 `TradeRequest`。
- 执行阶段若用 Transaction：先验证再 commit。
- 新增交易形态：实现自己的 Type / Storage / Definition，再在 registration 灌条目；不必改本包。

## 与 bank 的关系

- 本包独立；银行侧只通过 **Identifier（trade type id）** 关联价目存储器。
- 可交易卡接口 `TradableWalletBankCard#getTradeTypeId()` 指向 `TradeRegistry` 键。
- 货币扣加属于 `currency_item` 实现，通过 `bank.resource` 完成，不在本包内。

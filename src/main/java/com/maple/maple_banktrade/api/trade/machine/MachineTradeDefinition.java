package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.resource.BankCurrencyResourceHandler;
import com.maple.maple_banktrade.api.bank.resource.CurrencyResource;
import com.maple.maple_banktrade.api.trade.base.definition.TradeDefinition;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ScaledIO;
import com.mapleutillib.api.resource.ObservableFluidResourceHandler;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 机器多资源交易：check 用水桶法估算最大次数（必要时纯读模拟校验），execute 按 plan 提交。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MachineTradeDefinition implements TradeDefinition<TradeCheckInput.Basic<MachineTradeContext, MachineTradeRequest>, MachineTradePlan, TradeExecuteInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan>, TradeExecuteResult<MachineTradeDetail>, TradeSuccessInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan, TradeExecuteResult<MachineTradeDetail>>> {

    public static final MachineTradeDefinition INSTANCE = new MachineTradeDefinition();

    // ==============================================
    // check
    // ==============================================

    /**
     * 查表 → extraCheck → 水桶法 max 与 desired 取 min → 至多一次纯读模拟校验 → plan。
     */
    @Override
    public TradeCheckResult<MachineTradePlan> check(
                                                    TradeCheckInput.Basic<MachineTradeContext, MachineTradeRequest> input) {
        MachineTradeContext context = input.context();
        MachineTradeRequest request = input.request();
        int requested = request.desiredCount();
        if (requested <= 0) {
            return TradeCheckResult.of(MachineTradePlan.denied(requested));
        }

        MachineTrade trade = context.storage().require(request.tradeId());
        if (trade == null || !trade.isValid()) {
            return TradeCheckResult.of(MachineTradePlan.denied(requested));
        }

        if (!trade.stateHook().isVisible(context, trade)) {
            return TradeCheckResult.of(
                    MachineTradePlan.denied(requested),
                    List.of(Component.translatable("trade.maple_banktrade.fail.machine_not_visible")));
        }

        if (!trade.checkHook().check(context, request, trade)) {
            return TradeCheckResult.of(
                    MachineTradePlan.denied(requested),
                    List.of(Component.translatable("trade.maple_banktrade.fail.machine_check_failed")));
        }

        // CheckHook 可能通过 request.setDesiredCount 降级次数（限购），钩子返回后重新读取
        int desired = request.desiredCount();
        if (desired <= 0) {
            return TradeCheckResult.of(MachineTradePlan.denied(requested));
        }

        int actual = maxFeasibleCount(context, trade, desired);
        if (actual <= 0) {
            return TradeCheckResult.of(
                    MachineTradePlan.denied(desired),
                    List.of(Component.translatable("trade.maple_banktrade.fail.machine_insufficient")));
        }

        ScaledIO scaled = ScaledIO.scale(trade, actual);
        if (scaled == null) {
            return TradeCheckResult.of(MachineTradePlan.denied(desired));
        }
        return TradeCheckResult.of(MachineTradePlan.of(trade, desired, actual, scaled));
    }

    // ==============================================
    // execute
    // ==============================================

    /** 严格按 plan 已放大 I/O 在事务内提交。 */
    @Override
    public TradeExecuteResult<MachineTradeDetail> execute(
                                                          TradeExecuteInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan> input) {
        MachineTradePlan plan = input.plan();
        if (!plan.executable() || plan.trade() == null || plan.tradeCount() <= 0) {
            return TradeExecuteResult.failure(null);
        }

        MachineTradeContext context = input.context();
        MachineTradeRequest request = input.request();
        // 写前纯读复检：check 与 execute 之间库存可能变化；先确认放得下/扣得了，
        // 避免 tryTransfer 中途失败时靠兼容层回滚（回滚写回仍受输出槽过滤器限制）残留产物。
        if (!simulateFeasible(context, plan.scaledIo())) {
            return TradeExecuteResult.failure(
                    null,
                    List.of(Component.translatable("trade.maple_banktrade.fail.machine_insufficient")));
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (!tryTransfer(context, plan.scaledIo(), tx)) {
                return TradeExecuteResult.failure(
                        null,
                        List.of(Component.translatable("trade.maple_banktrade.fail.machine_transfer")));
            }
            tx.commit();
            return TradeExecuteResult.success(new MachineTradeDetail(
                    request.tradeId(),
                    plan.desiredCount(),
                    plan.tradeCount(),
                    plan.scaledIo(),
                    cardUuids(context.bankCards())));
        }
    }

    // ==============================================
    // afterSuccess
    // ==============================================

    @Override
    public void afterSuccess(
                             TradeSuccessInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan, TradeExecuteResult<MachineTradeDetail>> input) {
        MachineTradePlan plan = input.plan();
        if (plan.trade() == null) {
            return;
        }
        try {
            MachineTradeHooks.SuccessHook successHook = plan.trade().successHook();
            successHook.afterSuccess(input.context(), input.request(), plan, input.executionResult());
        } catch (Exception e) {
            MapleBankTrade.LOGGER.error("MachineTrade afterSuccess hook failed for {}", input.request().tradeId(), e);
        }
    }

    // ==============================================
    // 次数：水桶法 + 纯读模拟
    // ==============================================

    /**
     * 最大可执行次数 = min(请求次数, 各资源允许次数的最小值)。
     * <p>
     * 先水桶估算，再对估算结果做一次纯读模拟 {@link #canRun} 校验；
     * 失败时在 {@code [1, target)} 上二分回退。整个过程不修改任何库存。
     * </p>
     */
    static int maxFeasibleCount(MachineTradeContext context, MachineTrade trade, int desired) {
        if (desired <= 0) {
            return 0;
        }
        int byBucket = maxCountByResources(context, trade);
        if (byBucket <= 0) {
            return 0;
        }
        int target = Math.min(desired, byBucket);
        // 乐观路径：一次纯读模拟确认水桶估计
        if (canRun(context, trade, target)) {
            return target;
        }
        // 估计偏乐观（输出碎片、卡限制等）：仅在缩小区间上二分
        if (target == 1) {
            return 0;
        }
        return binarySearchFeasible(context, trade, target - 1);
    }

    /**
     * 水桶法：对每条输入/输出/能量/货币约束，用「可用量 / 单次量」取最小值。
     *
     * @return 资源允许的最大次数；无法执行时为 0
     */
    static int maxCountByResources(MachineTradeContext context, MachineTrade trade) {
        int max = Integer.MAX_VALUE;
        boolean constrained = false;

        for (ItemIO io : trade.itemInputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, countItem(context.itemInput(), ItemResource.of(io.itemStack())) / io.amount());
        }
        for (FluidIO io : trade.fluidInputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, countFluid(context.fluidInput(), FluidResource.of(io.fluidStack())) / io.amount());
        }
        if (trade.energyExtract() > 0) {
            constrained = true;
            long per = trade.energyExtract();
            long have = context.energy().getAmountAsLong();
            max = minNonNeg(max, (int) Math.min(Integer.MAX_VALUE, have / per));
        }

        Set<BankCurrencyResourceHandler> currencyHandlers = context.currencyHandlers();
        for (CurrencyIO io : trade.currencyExtract()) {
            if (!io.isValid()) {
                return 0;
            }
            constrained = true;
            BigInteger per = io.amount();
            if (per.signum() <= 0) {
                return 0;
            }
            BigInteger have = sumCurrency(currencyHandlers, io.resource());
            max = minNonNeg(max, divideToInt(have, per));
        }

        for (ItemIO io : trade.itemOutputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, freeItemSpace(context.itemOutput(), ItemResource.of(io.itemStack())) / io.amount());
        }
        for (FluidIO io : trade.fluidOutputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, freeFluidSpace(context.fluidOutput(), FluidResource.of(io.fluidStack())) / io.amount());
        }
        if (trade.energyInsert() > 0) {
            constrained = true;
            long per = trade.energyInsert();
            long free = Math.max(0L, context.energy().getCapacityAsLong() - context.energy().getAmountAsLong());
            max = minNonNeg(max, (int) Math.min(Integer.MAX_VALUE, free / per));
        }
        // 货币入账：卡容量通常极大，不作为硬约束（失败则 silent / execute 失败）

        if (!constrained) {
            return 0;
        }
        return max == Integer.MAX_VALUE ? 0 : max;
    }

    /** 在 {@code 1..hiInclusive} 上二分最大可纯读模拟通过的次数（回退路径）。 */
    private static int binarySearchFeasible(MachineTradeContext context, MachineTrade trade, int hiInclusive) {
        if (hiInclusive <= 0) {
            return 0;
        }
        if (!canRun(context, trade, 1)) {
            return 0;
        }
        if (canRun(context, trade, hiInclusive)) {
            return hiInclusive;
        }
        int lo = 1;
        int hi = hiInclusive;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            if (canRun(context, trade, mid)) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    private static int minNonNeg(int a, int b) {
        if (b < 0) {
            return 0;
        }
        return Math.min(a, b);
    }

    private static int divideToInt(BigInteger num, BigInteger den) {
        if (den.signum() <= 0 || num.signum() <= 0) {
            return 0;
        }
        BigInteger q = num.divide(den);
        if (q.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            return Integer.MAX_VALUE;
        }
        return q.intValue();
    }

    private static int countItem(ObservableItemResourceHandler handler, ItemResource resource) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && resource.matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int countFluid(ObservableFluidResourceHandler handler, FluidResource resource) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidStack stack = handler.getFluidInSlot(i);
            if (!stack.isEmpty() && resource.matches(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /** 输出侧可再放入该资源的空位总量。 */
    private static int freeItemSpace(ObservableItemResourceHandler handler, ItemResource resource) {
        int free = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            int cap = handler.getSlotCapacity(i, resource.toStack(1));
            if (cap <= 0) {
                continue;
            }
            if (stack.isEmpty()) {
                free += cap;
            } else if (resource.matches(stack)) {
                free += Math.max(0, cap - stack.getCount());
            }
        }
        return free;
    }

    private static int freeFluidSpace(ObservableFluidResourceHandler handler, FluidResource resource) {
        int free = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidStack stack = handler.getFluidInSlot(i);
            int cap = handler.getTankCapacity(i);
            if (cap <= 0) {
                continue;
            }
            if (stack.isEmpty()) {
                free += cap;
            } else if (resource.matches(stack)) {
                free += Math.max(0, cap - stack.getAmount());
            }
        }
        return free;
    }

    private static BigInteger sumCurrency(Set<BankCurrencyResourceHandler> handlers, CurrencyResource resource) {
        BigInteger total = BigInteger.ZERO;
        if (handlers == null || resource == null || resource.isEmpty()) {
            return total;
        }
        for (BankCurrencyResourceHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            for (int i = 0; i < handler.size(); i++) {
                if (resource.equals(handler.getResource(i))) {
                    total = total.add(handler.getAmountAsBigInteger(i));
                }
            }
        }
        return total;
    }

    /**
     * 纯读模拟：核对 scaled I/O 在当前状态下能否一次完成（check 阶段可行性校验）。
     * <p>
     * 旧实现用「真实 dry-run」——直接对输入/输出槽执行一遍转移再靠事务回滚还原。
     * 1.21.1 兼容层里物品/流体回滚依赖 {@code setStackInSlot/setFluidInSlot}，
     * 而这些写回仍会经过槽位 IO 过滤（例如贸易站输出侧挂的 NoInsertFilter），
     * 导致回滚被吞、dry-run 插入的产物残留在输出槽里；随后 execute 再真实插入一次，
     * 于是出现「原料只扣 1 份、产物却多出 1 层」的重复输出。
     * 本方法只在内存模型上模拟转移顺序，不修改任何真实存储，因此 check 阶段零副作用。
     * </p>
     */
    static boolean canRun(MachineTradeContext context, MachineTrade trade, int count) {
        ScaledIO scaled = ScaledIO.scale(trade, count);
        if (scaled == null) {
            return false;
        }
        return simulateFeasible(context, scaled);
    }

    /**
     * 按 execute 的真实顺序（先提取全部输入、再插入全部输出）在纯内存槽位模型上
     * 验证 scaled I/O 能否完整执行，全程只读，不产生任何库存副作用。
     */
    static boolean simulateFeasible(MachineTradeContext context, ScaledIO io) {
        boolean sharedItems = context.itemOutput() == context.itemInput();
        ItemModel itemInput = new ItemModel(context.itemInput());
        ItemModel itemOutput = sharedItems ? itemInput : new ItemModel(context.itemOutput());
        boolean sharedFluids = context.fluidOutput() == context.fluidInput();
        FluidModel fluidInput = new FluidModel(context.fluidInput());
        FluidModel fluidOutput = sharedFluids ? fluidInput : new FluidModel(context.fluidOutput());

        // 1) 输入提取：可用量不足则整次不可行
        for (ItemIO entry : io.itemInputs()) {
            if (!itemInput.extract(entry.itemStack(), entry.amount())) {
                return false;
            }
        }
        for (FluidIO entry : io.fluidInputs()) {
            if (!fluidInput.extract(entry.fluidStack(), entry.amount())) {
                return false;
            }
        }
        if (io.energyExtract() > 0 && context.energy().getAmountAsLong() < io.energyExtract()) {
            return false;
        }
        if (!currencyExtractable(context.currencyHandlers(), io.currencyExtract())) {
            return false;
        }

        // 2) 输出插入：按槽位逐格模拟分配（与 execute 的 insert 语义一致）
        for (ItemIO entry : io.itemOutputs()) {
            if (!itemOutput.insert(entry.itemStack(), entry.amount())) {
                return false;
            }
        }
        for (FluidIO entry : io.fluidOutputs()) {
            if (!fluidOutput.insert(entry.fluidStack(), entry.amount())) {
                return false;
            }
        }
        if (io.energyInsert() > 0) {
            long free = context.energy().getCapacityAsLong() - context.energy().getAmountAsLong();
            if (free < io.energyInsert()) {
                return false;
            }
        }
        // 货币入账容量通常极大，不作为硬约束（execute 事务内失败会整体回滚兜底）
        return true;
    }

    /** 各绑定卡上该货币余额总和是否满足扣款要求（与 execute 的跨卡分摊一致）。 */
    private static boolean currencyExtractable(Set<BankCurrencyResourceHandler> handlers, List<CurrencyIO> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        for (CurrencyIO io : list) {
            if (!io.isValid() || sumCurrency(handlers, io.resource()).compareTo(io.amount()) < 0) {
                return false;
            }
        }
        return true;
    }

    /** 纯内存物品槽位模型：不写回 {@code live}，仅按 {@code live} 的容量/锁定规则模拟。 */
    private static final class ItemModel {

        private final ObservableItemResourceHandler live;
        private final ItemStack[] slots;

        private ItemModel(ObservableItemResourceHandler live) {
            this.live = live;
            int size = live.size();
            this.slots = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                slots[i] = live.getStackInSlot(i).copy();
            }
        }

        /** 模拟提取：从各可写槽位凑齐 amount 个 template（语义同 {@link #extractItemBypass}）。 */
        private boolean extract(ItemStack template, int amount) {
            if (amount <= 0) {
                return true;
            }
            if (template == null || template.isEmpty()) {
                return false;
            }
            int need = amount;
            for (int i = 0; i < slots.length && need > 0; i++) {
                if (live.isSlotLocked(i)) {
                    continue;
                }
                ItemStack stack = slots[i];
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) {
                    continue;
                }
                int take = Math.min(need, stack.getCount());
                if (take >= stack.getCount()) {
                    slots[i] = ItemStack.EMPTY;
                } else {
                    stack.shrink(take);
                }
                need -= take;
            }
            return need == 0;
        }

        /** 模拟插入：按槽位顺序放入 amount 个 template（语义同 {@link #insertItemBypass}）。 */
        private boolean insert(ItemStack template, int amount) {
            if (amount <= 0) {
                return true;
            }
            if (template == null || template.isEmpty()) {
                return false;
            }
            ItemStack probe = template.copyWithCount(1);
            int need = amount;
            for (int i = 0; i < slots.length && need > 0; i++) {
                if (live.isSlotLocked(i)) {
                    continue;
                }
                ItemStack stack = slots[i];
                if (!stack.isEmpty() && !ItemStack.isSameItemSameComponents(stack, probe)) {
                    continue;
                }
                int cap = live.getSlotCapacity(i, probe);
                int used = stack.isEmpty() ? 0 : stack.getCount();
                int space = cap - used;
                if (space <= 0) {
                    continue;
                }
                int put = Math.min(need, space);
                if (stack.isEmpty()) {
                    slots[i] = probe.copyWithCount(put);
                } else {
                    stack.grow(put);
                }
                need -= put;
            }
            return need == 0;
        }
    }

    /** 纯内存流体槽位模型：不写回 {@code live}，仅按 {@code live} 的容量/锁定规则模拟。 */
    private static final class FluidModel {

        private final ObservableFluidResourceHandler live;
        private final FluidStack[] slots;

        private FluidModel(ObservableFluidResourceHandler live) {
            this.live = live;
            int size = live.size();
            this.slots = new FluidStack[size];
            for (int i = 0; i < size; i++) {
                FluidStack stack = live.getFluidInSlot(i);
                slots[i] = stack.isEmpty() ? FluidStack.EMPTY : stack.copy();
            }
        }

        /** 模拟提取：从各可写槽位凑齐 amount mB（语义同 {@link #extractFluidBypass}）。 */
        private boolean extract(FluidStack template, int amount) {
            if (amount <= 0) {
                return true;
            }
            if (template == null || template.isEmpty()) {
                return false;
            }
            int need = amount;
            for (int i = 0; i < slots.length && need > 0; i++) {
                if (live.isSlotLocked(i)) {
                    continue;
                }
                FluidStack stack = slots[i];
                if (stack.isEmpty() || !FluidStack.isSameFluidSameComponents(stack, template)) {
                    continue;
                }
                int take = Math.min(need, stack.getAmount());
                if (take >= stack.getAmount()) {
                    slots[i] = FluidStack.EMPTY;
                } else {
                    slots[i] = stack.copyWithAmount(stack.getAmount() - take);
                }
                need -= take;
            }
            return need == 0;
        }

        /** 模拟插入：按槽位顺序放入 amount mB（语义同 {@link #insertFluidBypass}）。 */
        private boolean insert(FluidStack template, int amount) {
            if (amount <= 0) {
                return true;
            }
            if (template == null || template.isEmpty()) {
                return false;
            }
            FluidStack probe = template.copyWithAmount(1);
            int need = amount;
            for (int i = 0; i < slots.length && need > 0; i++) {
                if (live.isSlotLocked(i)) {
                    continue;
                }
                FluidStack stack = slots[i];
                if (!stack.isEmpty() && !FluidStack.isSameFluidSameComponents(stack, probe)) {
                    continue;
                }
                int cap = live.getTankCapacity(i);
                int used = stack.isEmpty() ? 0 : stack.getAmount();
                int space = cap - used;
                if (space <= 0) {
                    continue;
                }
                int put = Math.min(need, space);
                if (stack.isEmpty()) {
                    slots[i] = probe.copyWithAmount(put);
                } else {
                    slots[i] = stack.copyWithAmount(stack.getAmount() + put);
                }
                need -= put;
            }
            return need == 0;
        }
    }

    /**
     * 在事务内按顺序：先 extract 全部输入，再 insert 全部输出；任一步未达计划量则 false。
     */
    static boolean tryTransfer(MachineTradeContext context, ScaledIO io, TransactionContext tx) {
        if (!extractItems(context.itemInput(), io.itemInputs(), tx)) {
            return false;
        }
        if (!extractFluids(context.fluidInput(), io.fluidInputs(), tx)) {
            return false;
        }
        if (!extractEnergy(context.energy(), io.energyExtract(), tx)) {
            return false;
        }

        Set<BankCurrencyResourceHandler> currencyHandlers = context.currencyHandlers();
        if (!MultiCardCurrencyHelper.extractAll(currencyHandlers, io.currencyExtract(), tx)) {
            return false;
        }

        if (!insertItems(context.itemOutput(), io.itemOutputs(), tx)) {
            return false;
        }
        if (!insertFluids(context.fluidOutput(), io.fluidOutputs(), tx)) {
            return false;
        }
        if (!insertEnergy(context.energy(), io.energyInsert(), tx)) {
            return false;
        }
        return MultiCardCurrencyHelper.insertAll(currencyHandlers, io.currencyInsert(), tx);
    }

    private static boolean extractItems(ObservableItemResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int got = extractItemBypass(handler, io.itemStack(), io.amount(), tx);
            if (got != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertItems(ObservableItemResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int put = insertItemBypass(handler, io.itemStack(), io.amount(), tx);
            if (put != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean extractFluids(ObservableFluidResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int got = extractFluidBypass(handler, io.fluidStack(), io.amount(), tx);
            if (got != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertFluids(ObservableFluidResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int put = insertFluidBypass(handler, io.fluidStack(), io.amount(), tx);
            if (put != io.amount()) {
                return false;
            }
        }
        return true;
    }

    // ── 跳过过滤器的资源 I/O（1.21.1 无事务 API，借助兼容层 Transaction 记录回滚） ──

    /** 跨槽位提取指定物品（跳过过滤器，尊重槽位锁定），并在事务中登记回滚。 */
    private static int extractItemBypass(ObservableItemResourceHandler handler, ItemStack template, int amount, TransactionContext tx) {
        if (amount <= 0 || template == null || template.isEmpty()) {
            return 0;
        }
        int got = 0;
        for (int i = 0; i < handler.size() && got < amount; i++) {
            ItemStack current = handler.getStackInSlot(i);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, template)) {
                continue;
            }
            int take = Math.min(amount - got, current.getCount());
            ItemStack out = handler.extractBypassFilter(i, take, false);
            got += out.getCount();
            if (tx instanceof Transaction t && out.getCount() > 0) {
                int slotIndex = i;
                ItemStack rolled = out;
                // 反向 bypass 还原：直接塞回被取走的数量。
                // 不能用 setStackInSlot 还原：该写回仍受 IO 过滤（如输出侧 NoInsertFilter）拦截。
                t.addRollback(() -> handler.insertBypassFilter(slotIndex, rolled, false));
            }
        }
        return got;
    }

    /** 跨槽位插入指定物品（跳过过滤器，尊重槽位锁定），并在事务中登记回滚。 */
    private static int insertItemBypass(ObservableItemResourceHandler handler, ItemStack template, int amount, TransactionContext tx) {
        if (amount <= 0 || template == null || template.isEmpty()) {
            return 0;
        }
        ItemStack probe = template.copyWithCount(1);
        int inserted = 0;
        for (int i = 0; i < handler.size() && inserted < amount; i++) {
            ItemStack current = handler.getStackInSlot(i);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, probe)) {
                continue;
            }
            int cap = handler.getSlotCapacity(i, probe);
            int space = cap - current.getCount();
            if (space <= 0) {
                continue;
            }
            int put = Math.min(amount - inserted, space);
            ItemStack rem = handler.insertBypassFilter(i, template.copyWithCount(put), false);
            int done = put - rem.getCount();
            inserted += done;
            if (tx instanceof Transaction t && done > 0) {
                int slotIndex = i;
                // 反向 bypass 还原：直接取回刚插入的数量（输出侧 NoInsertFilter 不会放行 setStackInSlot 还原）。
                t.addRollback(() -> handler.extractBypassFilter(slotIndex, done, false));
            }
        }
        return inserted;
    }

    /** 跨槽位提取指定流体（跳过过滤器，尊重槽位锁定），并在事务中登记回滚。 */
    private static int extractFluidBypass(ObservableFluidResourceHandler handler, FluidStack template, int amount, TransactionContext tx) {
        if (amount <= 0 || template == null || template.isEmpty()) {
            return 0;
        }
        int got = 0;
        for (int i = 0; i < handler.size() && got < amount; i++) {
            FluidStack current = handler.getFluidInSlot(i);
            if (current.isEmpty() || !FluidStack.isSameFluidSameComponents(current, template)) {
                continue;
            }
            int take = Math.min(amount - got, current.getAmount());
            FluidStack out = handler.extractBypassFilter(i, take);
            got += out.getAmount();
            if (tx instanceof Transaction t && out.getAmount() > 0) {
                int slotIndex = i;
                FluidStack rolled = out;
                // 反向 bypass 还原（见物品注释；不能用 setFluidInSlot 还原）。
                t.addRollback(() -> handler.insertBypassFilter(slotIndex, rolled, rolled.getAmount()));
            }
        }
        return got;
    }

    /** 跨槽位插入指定流体（跳过过滤器，尊重槽位锁定），并在事务中登记回滚。 */
    private static int insertFluidBypass(ObservableFluidResourceHandler handler, FluidStack template, int amount, TransactionContext tx) {
        if (amount <= 0 || template == null || template.isEmpty()) {
            return 0;
        }
        FluidStack probe = template.copyWithAmount(1);
        int inserted = 0;
        for (int i = 0; i < handler.size() && inserted < amount; i++) {
            FluidStack current = handler.getFluidInSlot(i);
            if (!current.isEmpty() && !FluidStack.isSameFluidSameComponents(current, probe)) {
                continue;
            }
            int space = handler.getTankCapacity(i) - current.getAmount();
            if (space <= 0) {
                continue;
            }
            int put = Math.min(amount - inserted, space);
            int done = handler.insertBypassFilter(i, probe.copyWithAmount(put), put);
            inserted += done;
            if (tx instanceof Transaction t && done > 0) {
                int slotIndex = i;
                // 反向 bypass 还原（见物品注释）。
                t.addRollback(() -> handler.extractBypassFilter(slotIndex, done));
            }
        }
        return inserted;
    }

    private static boolean extractEnergy(EnergyHandler energy, int amount, TransactionContext tx) {
        return amount == 0 || energy.extract(amount, tx) == amount;
    }

    private static boolean insertEnergy(EnergyHandler energy, int amount, TransactionContext tx) {
        return amount == 0 || energy.insert(amount, tx) == amount;
    }

    private static List<UUID> cardUuids(Set<BankCard> cards) {
        List<UUID> ids = new ArrayList<>(cards.size());
        for (BankCard card : cards) {
            if (card != null) {
                ids.add(card.getCardUuid());
            }
        }
        return ids;
    }
}

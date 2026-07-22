package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
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
 * 机器多资源交易：check 用水桶法估算最大次数（必要时一次 dry-run 校验），execute 按 plan 提交。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MachineTradeDefinition implements TradeDefinition<TradeCheckInput.Basic<MachineTradeContext, MachineTradeRequest>, MachineTradePlan, TradeExecuteInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan>, TradeExecuteResult<MachineTradeDetail>, TradeSuccessInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan, TradeExecuteResult<MachineTradeDetail>>> {

    public static final MachineTradeDefinition INSTANCE = new MachineTradeDefinition();

    // ==============================================
    // check
    // ==============================================

    /**
     * 查表 → extraCheck → 水桶法 max 与 desired 取 min → 至多一次 dry-run 校验 → plan。
     */
    @Override
    public TradeCheckResult<MachineTradePlan> check(
                                                    TradeCheckInput.Basic<MachineTradeContext, MachineTradeRequest> input) {
        MachineTradeContext context = input.context();
        MachineTradeRequest request = input.request();
        int desired = request.desiredCount();
        if (desired <= 0) {
            return TradeCheckResult.of(MachineTradePlan.denied(desired));
        }

        MachineTrade trade = context.storage().require(request.tradeId());
        if (trade == null || !trade.isValid()) {
            return TradeCheckResult.of(MachineTradePlan.denied(desired));
        }

        List<Component> extraMessages = trade.extraCheck().check(context, request, trade);
        if (extraMessages != null && !extraMessages.isEmpty()) {
            return TradeCheckResult.of(MachineTradePlan.denied(desired), List.copyOf(extraMessages));
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
            plan.trade().afterSuccess().afterSuccess(
                    input.context(),
                    input.request(),
                    plan,
                    input.executionResult());
        } catch (Exception e) {
            MapleBankTrade.LOGGER.error("MachineTrade afterSuccess hook failed for {}", input.request().tradeId(), e);
        }
    }

    // ==============================================
    // 次数：水桶法 + 可选一次 dry-run
    // ==============================================

    /**
     * 最大可执行次数 = min(请求次数, 各资源允许次数的最小值)。
     * <p>
     * 默认不再对 {@code [1..desired]} 做完整二分 dry-run；仅对估算结果做至多一次
     * {@link #canRun} 校验，失败时再在 {@code [1, target)} 上二分回退。
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
        // 乐观路径：一次 dry-run 确认水桶估计
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
            max = minNonNeg(max, countItem(context.itemInput(), io.resource()) / io.amount());
        }
        for (FluidIO io : trade.fluidInputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, countFluid(context.fluidInput(), io.resource()) / io.amount());
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
            max = minNonNeg(max, freeItemSpace(context.itemOutput(), io.resource()) / io.amount());
        }
        for (FluidIO io : trade.fluidOutputs()) {
            if (!io.isResolvable() || io.amount() <= 0) {
                return 0;
            }
            constrained = true;
            max = minNonNeg(max, freeFluidSpace(context.fluidOutput(), io.resource()) / io.amount());
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

    /** 在 {@code 1..hiInclusive} 上二分最大可 dry-run 次数（回退路径）。 */
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

    private static int countItem(ItemStacksResourceHandler handler, ItemResource resource) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            if (resource.equals(handler.getResource(i))) {
                total += handler.getAmountAsInt(i);
            }
        }
        return total;
    }

    private static int countFluid(FluidStacksResourceHandler handler, FluidResource resource) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            if (resource.equals(handler.getResource(i))) {
                total += handler.getAmountAsInt(i);
            }
        }
        return total;
    }

    /** 输出侧可再放入该资源的空位总量。 */
    private static int freeItemSpace(ItemStacksResourceHandler handler, ItemResource resource) {
        int free = 0;
        for (int i = 0; i < handler.size(); i++) {
            ItemResource slot = handler.getResource(i);
            int cap = handler.getCapacityAsInt(i, resource);
            if (cap <= 0) {
                continue;
            }
            if (slot.isEmpty()) {
                free += cap;
            } else if (slot.equals(resource)) {
                free += Math.max(0, cap - handler.getAmountAsInt(i));
            }
        }
        return free;
    }

    private static int freeFluidSpace(FluidStacksResourceHandler handler, FluidResource resource) {
        int free = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidResource slot = handler.getResource(i);
            int cap = handler.getCapacityAsInt(i, resource);
            if (cap <= 0) {
                continue;
            }
            if (slot.isEmpty()) {
                free += cap;
            } else if (slot.equals(resource)) {
                free += Math.max(0, cap - handler.getAmountAsInt(i));
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

    /** dry-run：打开根事务执行转移且不 commit。 */
    static boolean canRun(MachineTradeContext context, MachineTrade trade, int count) {
        ScaledIO scaled = ScaledIO.scale(trade, count);
        if (scaled == null) {
            return false;
        }
        try (Transaction tx = Transaction.openRoot()) {
            return tryTransfer(context, scaled, tx);
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

    private static boolean extractItems(ItemStacksResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int got = handler instanceof ObservableItemResourceHandler obs ? obs.extractBypassFilter(io.resource(), io.amount(), tx) : handler.extract(io.resource(), io.amount(), tx);
            if (got != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertItems(ItemStacksResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int put = handler instanceof ObservableItemResourceHandler obs ? obs.insertBypassFilter(io.resource(), io.amount(), tx) : handler.insert(io.resource(), io.amount(), tx);
            if (put != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean extractFluids(FluidStacksResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int got = handler instanceof ObservableFluidResourceHandler obs ? obs.extractBypassFilter(io.resource(), io.amount(), tx) : handler.extract(io.resource(), io.amount(), tx);
            if (got != io.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertFluids(FluidStacksResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int put = handler instanceof ObservableFluidResourceHandler obs ? obs.insertBypassFilter(io.resource(), io.amount(), tx) : handler.insert(io.resource(), io.amount(), tx);
            if (put != io.amount()) {
                return false;
            }
        }
        return true;
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

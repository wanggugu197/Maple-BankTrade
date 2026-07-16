package com.maple.maple_banktrade.trade.machine;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.definition.TradeDefinition;
import com.maple.maple_banktrade.api.trade.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.result.TradeExecuteResult;
import com.maple.maple_banktrade.bank.resource.BankCurrencyResourceHandler;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ItemIO;
import com.maple.maple_banktrade.trade.machine.MachineTradeIO.ScaledIO;
import com.mapleutillib.api.resource.ObservableFluidResourceHandler;
import com.mapleutillib.api.resource.ObservableItemResourceHandler;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 机器多资源交易：check 算最大可执行次数（可降级），execute 按 plan 提交。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MachineTradeDefinition implements TradeDefinition<TradeCheckInput.Basic<MachineTradeContext, MachineTradeRequest>, MachineTradePlan, TradeExecuteInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan>, TradeExecuteResult<MachineTradeDetail>, TradeSuccessInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan, TradeExecuteResult<MachineTradeDetail>>> {

    public static final MachineTradeDefinition INSTANCE = new MachineTradeDefinition();

    // ==============================================
    // check
    // ==============================================

    /**
     * 查表 → extraCheck 硬拒 → 二分求最大可执行次数 → plan（tradeCount 可 &lt; desired）。
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

    /** 调用条目钩子；异常仅记日志。 */
    @Override
    public void afterSuccess(
                             TradeSuccessInput.Basic<MachineTradeContext, MachineTradeRequest, MachineTradePlan, TradeExecuteResult<MachineTradeDetail>> input) {
        MachineTradePlan plan = input.plan();
        if (plan.trade() == null) return;
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
    // 次数降级 / 转移
    // ==============================================

    /**
     * 求 {@code 1..desired} 中最大可 dry-run 成功的次数；全失败返回 0。
     */
    static int maxFeasibleCount(MachineTradeContext context, MachineTrade trade, int desired) {
        if (desired <= 0) return 0;
        if (!canRun(context, trade, 1)) return 0;
        if (canRun(context, trade, desired)) return desired;

        int lo = 1;
        int hi = desired;
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

    /** dry-run：打开根事务执行转移且不 commit。 */
    static boolean canRun(MachineTradeContext context, MachineTrade trade, int count) {
        ScaledIO scaled = ScaledIO.scale(trade, count);
        if (scaled == null) return false;
        try (Transaction tx = Transaction.openRoot()) {
            return tryTransfer(context, scaled, tx);
        }
    }

    /**
     * 在事务内按顺序：先 extract 全部输入，再 insert 全部输出；任一步未达计划量则 false。
     */
    static boolean tryTransfer(MachineTradeContext context, ScaledIO io, TransactionContext tx) {
        if (!extractItems(context.itemInput(), io.itemInputs(), tx)) return false;
        if (!extractFluids(context.fluidInput(), io.fluidInputs(), tx)) return false;
        if (!extractEnergy(context.energy(), io.energyExtract(), tx)) return false;

        Set<BankCurrencyResourceHandler> currencyHandlers = context.currencyHandlers();
        if (!MultiCardCurrencyHelper.extractAll(currencyHandlers, io.currencyExtract(), tx)) return false;

        if (!insertItems(context.itemOutput(), io.itemOutputs(), tx)) return false;
        if (!insertFluids(context.fluidOutput(), io.fluidOutputs(), tx)) return false;
        if (!insertEnergy(context.energy(), io.energyInsert(), tx)) return false;
        return MultiCardCurrencyHelper.insertAll(currencyHandlers, io.currencyInsert(), tx);
    }

    private static boolean extractItems(ItemStacksResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int got = handler instanceof ObservableItemResourceHandler obs ? obs.extractBypassFilter(io.resource(), io.amount(), tx) : handler.extract(io.resource(), io.amount(), tx);
            if (got != io.amount()) return false;
        }
        return true;
    }

    private static boolean insertItems(ItemStacksResourceHandler handler, List<ItemIO> list, TransactionContext tx) {
        for (ItemIO io : list) {
            int put = handler instanceof ObservableItemResourceHandler obs ? obs.insertBypassFilter(io.resource(), io.amount(), tx) : handler.insert(io.resource(), io.amount(), tx);
            if (put != io.amount()) return false;
        }
        return true;
    }

    private static boolean extractFluids(FluidStacksResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int got = handler instanceof ObservableFluidResourceHandler obs ? obs.extractBypassFilter(io.resource(), io.amount(), tx) : handler.extract(io.resource(), io.amount(), tx);
            if (got != io.amount()) return false;
        }
        return true;
    }

    private static boolean insertFluids(FluidStacksResourceHandler handler, List<FluidIO> list, TransactionContext tx) {
        for (FluidIO io : list) {
            int put = handler instanceof ObservableFluidResourceHandler obs ? obs.insertBypassFilter(io.resource(), io.amount(), tx) : handler.insert(io.resource(), io.amount(), tx);
            if (put != io.amount()) return false;
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

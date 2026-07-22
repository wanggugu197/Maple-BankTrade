package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.maple.maple_banktrade.api.trade.base.definition.TradeRunner;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器多资源交易对外入口（含自动交易扫描）。
 */
@UtilityClass
public class MachineTradeHandler {

    /**
     * 自动交易请求次数：取较大固定值，实际执行量由
     * {@link MachineTradeDefinition#maxFeasibleCount} 水桶法与资源上限取 min。
     */
    public static final int AUTO_TRADE_DESIRED_COUNT = 1_000_000;

    /**
     * 按期望次数执行；check 可能降级实际次数，成功时 detail.tradeCount 为实际值。
     */
    public static TradeExecuteResult<MachineTradeDetail> run(@NonNull MachineTradeContext context,
                                                             @NonNull Identifier tradeId,
                                                             int desiredCount) {
        if (desiredCount <= 0) {
            return TradeExecuteResult.failure(null);
        }
        return TradeRunner.run(
                MachineTradeDefinition.INSTANCE,
                context,
                MachineTradeRequest.of(tradeId, desiredCount));
    }

    /**
     * 仅 check，不提交；plan.tradeCount 为降级后的可执行次数。
     */
    public static TradeCheckResult<MachineTradePlan> check(@NonNull MachineTradeContext context,
                                                           @NonNull Identifier tradeId,
                                                           int desiredCount) {
        if (desiredCount <= 0) {
            return TradeCheckResult.of(MachineTradePlan.denied(desiredCount));
        }
        return MachineTradeDefinition.INSTANCE.check(
                TradeCheckInput.of(context, MachineTradeRequest.of(tradeId, desiredCount)));
    }

    /** 当前上下文下可见的交易条目。 */
    public static List<Map.Entry<Identifier, MachineTrade>> listVisible(@NonNull MachineTradeContext context) {
        return context.storage().listVisible(context);
    }

    /**
     * 自动交易：单次扫描输入槽聚合资源量，经 storage 索引 O(1) 匹配后 {@link #run}。
     * <p>
     * 要求 storage {@link MachineTradeStorage#allowAutoTrade()}；同资源只处理一次。
     * </p>
     *
     * @return 至少成功提交一次的条目数（非 tradeCount 累计）
     */
    public static int autoRun(@NonNull MachineTradeContext context) {
        MachineTradeStorage storage = context.storage();
        if (!storage.allowAutoTrade()) {
            return 0;
        }

        int successEntries = 0;
        successEntries += autoRunItems(context, storage);
        successEntries += autoRunFluids(context, storage);
        return successEntries;
    }

    /**
     * 一次扫完 item 输入 → {@code resource → total}，再按索引匹配。
     * 复杂度 O(S + R) 查找，不再 O(R·S) 重复 count、O(R·E) 线性 find。
     */
    private static int autoRunItems(MachineTradeContext context, MachineTradeStorage storage) {
        ItemStacksResourceHandler handler = context.itemInput();
        Map<ItemResource, Integer> totals = aggregateItemTotals(handler);
        if (totals.isEmpty()) {
            return 0;
        }

        int success = 0;
        for (Map.Entry<ItemResource, Integer> totalEntry : totals.entrySet()) {
            // 有货才查索引；实际次数由 check 水桶法对 AUTO_TRADE_DESIRED_COUNT 取 min
            if (totalEntry.getValue() <= 0) {
                continue;
            }
            Map.Entry<Identifier, MachineTrade> match = storage.findAutoTradeByItem(totalEntry.getKey(), context);
            if (match == null) {
                continue;
            }
            TradeExecuteResult<MachineTradeDetail> result = run(context, match.getKey(), AUTO_TRADE_DESIRED_COUNT);
            if (result != null && result.success()) {
                success++;
            }
        }
        return success;
    }

    private static int autoRunFluids(MachineTradeContext context, MachineTradeStorage storage) {
        FluidStacksResourceHandler handler = context.fluidInput();
        Map<FluidResource, Integer> totals = aggregateFluidTotals(handler);
        if (totals.isEmpty()) {
            return 0;
        }

        int success = 0;
        for (Map.Entry<FluidResource, Integer> totalEntry : totals.entrySet()) {
            if (totalEntry.getValue() <= 0) {
                continue;
            }
            Map.Entry<Identifier, MachineTrade> match = storage.findAutoTradeByFluid(totalEntry.getKey(), context);
            if (match == null) {
                continue;
            }
            TradeExecuteResult<MachineTradeDetail> result = run(context, match.getKey(), AUTO_TRADE_DESIRED_COUNT);
            if (result != null && result.success()) {
                success++;
            }
        }
        return success;
    }

    /** 单次遍历聚合各物品资源总量（保持首次出现顺序）。 */
    private static Map<ItemResource, Integer> aggregateItemTotals(ItemStacksResourceHandler handler) {
        Map<ItemResource, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < handler.size(); i++) {
            ItemResource resource = handler.getResource(i);
            if (resource.isEmpty()) {
                continue;
            }
            int amount = handler.getAmountAsInt(i);
            if (amount <= 0) {
                continue;
            }
            totals.merge(resource, amount, Integer::sum);
        }
        return totals;
    }

    private static Map<FluidResource, Integer> aggregateFluidTotals(FluidStacksResourceHandler handler) {
        Map<FluidResource, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < handler.size(); i++) {
            FluidResource resource = handler.getResource(i);
            if (resource.isEmpty()) {
                continue;
            }
            int amount = handler.getAmountAsInt(i);
            if (amount <= 0) {
                continue;
            }
            totals.merge(resource, amount, Integer::sum);
        }
        return totals;
    }

    /** 列出本 storage 中所有自动交易条目（调试/UI 用）。 */
    public static List<Map.Entry<Identifier, MachineTrade>> listAutoTrades(@NonNull MachineTradeStorage storage) {
        List<Map.Entry<Identifier, MachineTrade>> list = new ArrayList<>();
        for (Map.Entry<Identifier, MachineTrade> e : storage.entries().entrySet()) {
            if (e.getValue().autoTrade()) {
                list.add(e);
            }
        }
        return list;
    }
}

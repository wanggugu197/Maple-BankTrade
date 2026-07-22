package com.maple.maple_banktrade.api.trade.currency_item;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.maple.maple_banktrade.api.bank.MBTBankStates;
import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.bank.base.BankCardsWorldData;
import com.maple.maple_banktrade.api.bank.resource.BankCurrencyResourceHandler;
import com.maple.maple_banktrade.api.trade.base.definition.TradeDefinition;
import com.maple.maple_banktrade.api.trade.base.input.TradeCheckInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeExecuteInput;
import com.maple.maple_banktrade.api.trade.base.input.TradeSuccessInput;
import com.maple.maple_banktrade.api.trade.base.result.TradeCheckResult;
import com.maple.maple_banktrade.api.trade.base.result.TradeExecuteResult;

import java.math.BigInteger;
import java.util.List;

/**
 * 货币-物品交易：check 仅查表算量组 plan，execute 在 Transaction 内提交并校验。
 */
public final class CurrencyItemTradeDefinition implements TradeDefinition<TradeCheckInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest>, CurrencyItemTradePlan, TradeExecuteInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest, CurrencyItemTradePlan>, TradeExecuteResult<CurrencyItemTradeDetail>, TradeSuccessInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest, CurrencyItemTradePlan, TradeExecuteResult<CurrencyItemTradeDetail>>> {

    // ==============================================
    // 单例
    // ==============================================

    public static final CurrencyItemTradeDefinition INSTANCE = new CurrencyItemTradeDefinition();

    private CurrencyItemTradeDefinition() {}

    // ==============================================
    // 交易阶段
    // ==============================================

    /**
     * 轻量组计划：解析价目 + 算量。不做余额/背包/槽位预检（由 execute 事务兜底）。
     */
    @Override
    public TradeCheckResult<CurrencyItemTradePlan> check(
                                                         TradeCheckInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest> input) {
        CurrencyItemTradeRequest request = input.request();
        CurrencyItemTrade trade = resolveTrade(input.context(), request);
        Amounts amounts = Amounts.of(trade, request.tradeCount());
        if (trade == null || amounts == null) {
            return TradeCheckResult.of(CurrencyItemTradePlan.denied());
        }
        return TradeCheckResult.of(CurrencyItemTradePlan.of(
                trade, amounts.currency, amounts.items, request.tradeCount(), request.isBuy()));
    }

    /** 同一 Transaction 内扣加货币与物品；失败自动回滚。 */
    @Override
    public TradeExecuteResult<CurrencyItemTradeDetail> execute(
                                                               TradeExecuteInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest, CurrencyItemTradePlan> input) {
        CurrencyItemTradePlan plan = input.plan();
        if (!plan.executable() || plan.trade() == null) {
            return TradeExecuteResult.failure(null);
        }

        CurrencyItemTradeContext context = input.context();
        CurrencyItemTradeRequest request = input.request();
        BankCurrencyResourceHandler currencyHandler = currencyHandler(context);
        if (currencyHandler == null) {
            return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.no_card")));
        }

        CurrencyItemTrade trade = plan.trade();
        try (Transaction tx = Transaction.openRoot()) {
            if (plan.buy()) {
                if (context.player() == null) {
                    return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.no_player")));
                }
                BigInteger extracted = currencyHandler.extractBigInteger(trade.currency(), plan.currencyAmount(), tx);
                if (!extracted.equals(plan.currencyAmount())) {
                    return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.not_enough_currency")));
                }
                ResourceHandler<ItemResource> mainSlots = PlayerInventoryWrapper.of(context.player()).getMainSlots();
                int inserted = mainSlots.insert(ItemResource.of(trade.item()), plan.itemAmount(), tx);
                if (inserted != plan.itemAmount()) {
                    return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.inventory_full")));
                }
            } else {
                if (request.sourceHandler() == null) {
                    return TradeExecuteResult.failure(null);
                }
                BigInteger inserted = currencyHandler.insertBigInteger(trade.currency(), plan.currencyAmount(), tx);
                if (!inserted.equals(plan.currencyAmount())) {
                    return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.currency_insert")));
                }
                int extracted = request.sourceHandler().extract(
                        request.sourceSlot(),
                        ItemResource.of(trade.item()),
                        plan.itemAmount(),
                        tx);
                if (extracted != plan.itemAmount()) {
                    return TradeExecuteResult.failure(null, List.of(msg("trade.maple_banktrade.fail.item_extract")));
                }
            }
            tx.commit();
            return TradeExecuteResult.success(new CurrencyItemTradeDetail(
                    context.cardUuid(),
                    trade.currency(),
                    plan.currencyAmount(),
                    trade.item(),
                    plan.itemAmount(),
                    plan.tradeCount(),
                    plan.buy()));
        }
    }

    /** 无额外副作用。 */
    @Override
    public void afterSuccess(
                             TradeSuccessInput.Basic<CurrencyItemTradeContext, CurrencyItemTradeRequest, CurrencyItemTradePlan, TradeExecuteResult<CurrencyItemTradeDetail>> input) {}

    // ==============================================
    // 工具
    // ==============================================

    /** 买：按 tradeId；卖：按槽位物品匹配可卖价目。 */
    private static CurrencyItemTrade resolveTrade(CurrencyItemTradeContext context, CurrencyItemTradeRequest request) {
        if (request.isBuy()) {
            return context.storage().require(request.tradeId());
        }
        if (request.isSell() && request.sourceHandler() != null) {
            ItemResource resource = request.sourceHandler().getResource(request.sourceSlot());
            if (!resource.isEmpty()) {
                return context.storage().findSellableByItem(resource.toStack(1));
            }
        }
        return null;
    }

    /** 从上下文银行卡解析货币处理器。 */
    private static BankCurrencyResourceHandler currencyHandler(CurrencyItemTradeContext context) {
        BankCardsWorldData data = MBTBankStates.getBankCards(context.server());
        BankCard card = data.getCard(context.cardUuid());
        return card == null ? null : BankCurrencyResourceHandler.of(card, data::setDirty);
    }

    private static Component msg(String key) {
        return Component.translatable(key);
    }

    /** 次数 × 单价/单次物品数。 */
    private record Amounts(BigInteger currency, int items) {

        static Amounts of(CurrencyItemTrade trade, int tradeCount) {
            if (trade == null || tradeCount <= 0 || trade.item().isEmpty() || trade.currency().isEmpty()) {
                return null;
            }
            BigInteger currency = trade.pricePerTrade().multiply(BigInteger.valueOf(tradeCount));
            int items;
            try {
                items = Math.multiplyExact(trade.itemAmountPerTrade(), tradeCount);
            } catch (ArithmeticException ignored) {
                return null;
            }
            if (currency.signum() <= 0 || items <= 0) return null;
            return new Amounts(currency, items);
        }
    }
}

package com.maple.maple_banktrade.trade.machine;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import com.maple.maple_banktrade.api.bank.base.BankCard;
import com.maple.maple_banktrade.api.trade.context.TradeContext;
import com.maple.maple_banktrade.bank.resource.BankCurrencyResourceHandler;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * 机器多资源交易运行时上下文（由方块实体或调用方组装，不含触发实体）。
 */
public record MachineTradeContext(
                                  @Nullable BlockEntity blockEntity,
                                  Level level,
                                  MinecraftServer server,
                                  ItemStacksResourceHandler itemInput,
                                  ItemStacksResourceHandler itemOutput,
                                  FluidStacksResourceHandler fluidInput,
                                  FluidStacksResourceHandler fluidOutput,
                                  EnergyHandler energy,
                                  Set<BankCard> bankCards,
                                  MachineTradeStorage storage)
        implements TradeContext {

    public MachineTradeContext {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(itemInput, "itemInput");
        Objects.requireNonNull(itemOutput, "itemOutput");
        Objects.requireNonNull(fluidInput, "fluidInput");
        Objects.requireNonNull(fluidOutput, "fluidOutput");
        Objects.requireNonNull(energy, "energy");
        Objects.requireNonNull(storage, "storage");
        bankCards = bankCards == null ? Set.of() : Set.copyOf(bankCards);
    }

    /**
     * 将银行卡映射为货币处理器（保持 Set 顺序，跳过非货币卡）。
     */
    public Set<BankCurrencyResourceHandler> currencyHandlers() {
        LinkedHashSet<BankCurrencyResourceHandler> handlers = new LinkedHashSet<>();
        for (BankCard card : bankCards) {
            BankCurrencyResourceHandler handler = BankCurrencyResourceHandler.of(card);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }
}

package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.api.trade.base.registry.TradeInfo;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.CurrencyIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.FluidIO;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeIO.ItemIO;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器多资源交易条目：单次配方 I/O + 可选展示信息 + 三个自定义钩子（通过注册表 ID + 配置引用）。
 * <p>
 * 实现 {@link IPersistedSerializable} 以支持 LDLib2 持久化与同步。
 * </p>
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class MachineTrade implements TradeInfo {

    // ==============================================
    // 持久化字段
    // ==============================================

    @Persisted
    private ResourceLocation id;

    @Persisted
    private List<ItemIO> itemInputs;
    @Persisted
    private List<ItemIO> itemOutputs;
    @Persisted
    private List<FluidIO> fluidInputs;
    @Persisted
    private List<FluidIO> fluidOutputs;
    @Persisted
    private long energyExtract;
    @Persisted
    private long energyInsert;
    @Persisted
    private List<CurrencyIO> currencyExtract;
    @Persisted
    private List<CurrencyIO> currencyInsert;

    @Persisted
    private boolean autoTrade;

    @Persisted
    private ResourceLocation machineTradeIcon;
    @Persisted
    private List<List<Component>> description;

    @Persisted
    private MachineTradeHooks.StateHook stateHook;
    @Persisted
    private MachineTradeHooks.CheckHook checkHook;
    @Persisted
    private MachineTradeHooks.SuccessHook successHook;

    // ==============================================
    // 构造器
    // ==============================================

    /** 无参构造器（供 LDLib2 反序列化使用） */
    public MachineTrade() {
        this.itemInputs = new ArrayList<>();
        this.itemOutputs = new ArrayList<>();
        this.fluidInputs = new ArrayList<>();
        this.fluidOutputs = new ArrayList<>();
        this.energyExtract = 0;
        this.energyInsert = 0;
        this.currencyExtract = new ArrayList<>();
        this.currencyInsert = new ArrayList<>();
        this.autoTrade = false;
        this.machineTradeIcon = null;
        this.description = new ArrayList<>();
        this.stateHook = new MachineTradeHooks.AlwaysVisibleStateHook();
        this.checkHook = new MachineTradeHooks.PassCheckHook();
        this.successHook = new MachineTradeHooks.NoopSuccessHook();
    }

    // ==============================================
    // 业务方法
    // ==============================================
    public boolean hasValidAutoTradeInputs() {
        return itemInputs.size() + fluidInputs.size() == 1;
    }

    @Override
    public boolean isValid() {
        if (energyExtract < 0 || energyInsert < 0 || energyExtract > Integer.MAX_VALUE || energyInsert > Integer.MAX_VALUE) {
            return false;
        }
        if (!itemInputs.stream().allMatch(ItemIO::isValid) ||
                !itemOutputs.stream().allMatch(ItemIO::isValid) ||
                !fluidInputs.stream().allMatch(FluidIO::isValid) ||
                !fluidOutputs.stream().allMatch(FluidIO::isValid) ||
                !currencyExtract.stream().allMatch(CurrencyIO::isValid) ||
                !currencyInsert.stream().allMatch(CurrencyIO::isValid) ||
                !hasAnyIo()) {
            return false;
        }
        return !autoTrade || hasValidAutoTradeInputs();
    }

    private boolean hasAnyIo() {
        return !itemInputs.isEmpty() || !itemOutputs.isEmpty() ||
                !fluidInputs.isEmpty() || !fluidOutputs.isEmpty() ||
                energyExtract > 0 || energyInsert > 0 ||
                !currencyExtract.isEmpty() || !currencyInsert.isEmpty();
    }

    // ==============================================
    // Builder 工厂方法
    // ==============================================

    public static MachineTradeBuilder builder(ResourceLocation id) {
        return new MachineTradeBuilder(id);
    }
}

package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.resources.Identifier;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.base.registry.TradeRegistry;
import com.maple.maple_banktrade.api.trade.base.registry.TradeType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@Getter
@Setter
@Accessors(fluent = true)
public final class MachineTradeType implements TradeType<MachineTradeStorage>, IPersistedSerializable {

    @Persisted
    private Identifier id;

    @Persisted
    private boolean allowAutoTrade;

    // 无参构造器（反序列化必需）
    public MachineTradeType() {
        this.id = MapleBankTrade.id("default_machine");
        this.allowAutoTrade = false;
    }

    public MachineTradeType(Identifier id, boolean allowAutoTrade) {
        this.id = Objects.requireNonNull(id, "id");
        this.allowAutoTrade = allowAutoTrade;
    }

    public static MachineTradeType of(Identifier id) {
        return of(id, false);
    }

    public static MachineTradeType of(Identifier id, boolean allowAutoTrade) {
        return new MachineTradeType(id, allowAutoTrade);
    }

    @Override
    public MachineTradeStorage createStorage() {
        return new MachineTradeStorage(id, allowAutoTrade);
    }

    public MachineTradeStorage register() {
        MachineTradeStorage existing = TradeRegistry.requireStorage(id, MachineTradeStorage.class);
        return existing != null ? existing : TradeRegistry.registerType(this);
    }
}

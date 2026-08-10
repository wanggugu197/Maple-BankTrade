package com.maple.maple_banktrade.common.trade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHookRegistry;

public class CheckHasRegister {

    public static final Identifier VISIBILITY_FILTER_BY_NAME = MapleBankTrade.id("visibility_filter_by_name");

    public static void registerVisibilityFilterByName() {
        MachineTradeHookRegistry.registerVisibility(VISIBILITY_FILTER_BY_NAME, config -> {
            String nameIndexStr = config.getStringOr("name_index", "");
            Identifier target = Identifier.tryParse(nameIndexStr);
            if (target == null) return (_, _) -> true;
            return (context, _) -> context.bankCards().stream()
                    .anyMatch(card -> target.equals(card.getNameIndex()));
        });
    }

    public static CompoundTag createVisibilityFilterByNameCompoundTag(Identifier name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("name_index", name.toString());
        return tag;
    }
}

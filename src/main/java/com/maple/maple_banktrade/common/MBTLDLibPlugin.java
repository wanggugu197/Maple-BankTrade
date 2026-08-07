package com.maple.maple_banktrade.common;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.serialization.Codec;

import java.math.BigInteger;

@LDLibPlugin
public class MBTLDLibPlugin implements ILDLibPlugin {

    public void onLoad() {
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(BigInteger.class)
                .codec(Codec.STRING.xmap(
                        s -> {
                            try {
                                return new BigInteger(s);
                            } catch (NumberFormatException e) {
                                return BigInteger.ZERO;
                            }
                        }, BigInteger::toString))
                .streamCodec(
                        StreamCodec.composite(
                                ByteBufCodecs.STRING_UTF8,
                                BigInteger::toString,
                                BigInteger::new))
                .build());
    }
}

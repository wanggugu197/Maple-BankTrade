package com.maple.maple_banktrade;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import com.maple.maple_banktrade.common.CommonInit;
import com.mapleutillib.api.registry.ModRegistryCore;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 枫糖银贸 / Maple Banktrade 主入口。
 */
@Mod(MapleBankTrade.MODID)
public class MapleBankTrade {

    // ==============================================
    // 常量
    // ==============================================

    public static final String MODID = "maple_banktrade";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ModRegistryCore REGISTRY = ModRegistryCore.create(MODID);

    // ==============================================
    // 初始化
    // ==============================================

    /** 模组加载入口，触发通用初始化。 */
    public MapleBankTrade(IEventBus modEventBus) {
        LOGGER.info("{} start loading", MODID);
        CommonInit.init(modEventBus);
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 创建本模组命名空间下的 Identifier。 */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}

package com.maple.maple_banktrade.config;

import com.maple.maple_banktrade.MapleBankTrade;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;
import org.jetbrains.annotations.ApiStatus;

/**
 * YAML 配置持有器（toma.configuration）。
 */
@Config(id = MapleBankTrade.MODID, filename = "maple_banktrade/maple_banktrade")
public class MBTModConfig {

    // ==============================================
    // 实例
    // ==============================================

    public static MBTModConfig INSTANCE;

    private static final Object LOCK = new Object();

    @ApiStatus.Internal
    public static ConfigHolder<MBTModConfig> INTERNAL_INSTANCE;

    // ==============================================
    // 初始化
    // ==============================================

    /** 注册并加载配置（幂等）。 */
    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null || INTERNAL_INSTANCE == null) {
                INTERNAL_INSTANCE = Configuration.registerConfig(MBTModConfig.class, ConfigFormats.YAML);
                INSTANCE = INTERNAL_INSTANCE.getConfigInstance();
            }
        }
    }

    /** 获取配置实例，未初始化时自动 init。 */
    private static MBTModConfig config() {
        if (INSTANCE == null) init();
        return INSTANCE;
    }

    // ==============================================
    // 配置项
    // ==============================================

    @Configurable
    @Configurable.Comment({
            "通用配置 General configuration"
    })
    public GeneralConfigs general = new GeneralConfigs();

    /**
     * 通用开关配置。
     */
    public static class GeneralConfigs {

        @Configurable
        @Configurable.Comment({
                "Register built-in currency-item trade entries? Default: true",
                "是否注册内置货币-物品交易价目？默认 true"
        })
        public boolean enableBuiltInTrades = true;

        @Configurable
        @Configurable.Comment({
                "Register player bank self-service commands (/mbt_bank)? Default: true",
                "是否注册玩家银行命令 /mbt_bank？默认 true"
        })
        public boolean enableBankCommands = true;
    }

    // ==============================================
    // 访问
    // ==============================================

    /** 是否注册内置货币-物品交易价目。 */
    public static boolean enableBuiltInTrades() {
        return config().general.enableBuiltInTrades;
    }

    /** 是否注册玩家银行命令。 */
    public static boolean enableBankCommands() {
        return config().general.enableBankCommands;
    }
}

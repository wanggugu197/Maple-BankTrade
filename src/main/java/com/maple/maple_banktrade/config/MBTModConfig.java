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
                "Register built-in mod content (banks, cards, currencies, trading station, etc.)?",
                "When false, only API registration remains (wallet items, UI, bank data, commands).",
                "Default: true",
                "是否注册模组内置内容（银行/卡/货币/交易站等）？",
                "关闭后仅保留 API 注册（钱包物品、UI、银行数据、命令）。",
                "默认 true"
        })
        public boolean enableModContent = true;

        @Configurable
        @Configurable.Comment({
                "Register built-in trade entries (currency-item prices & machine recipes)?",
                "Only effective when enableModContent is true. Default: true",
                "是否注册内置交易价目（货币-物品与机器配方）？",
                "仅在 enableModContent 为 true 时生效。默认 true"
        })
        public boolean enableBuiltInTrades = true;
    }

    // ==============================================
    // 访问
    // ==============================================

    /** 是否注册模组内置内容（银行/卡/货币/交易站等）。关闭时仅 API。 */
    public static boolean enableModContent() {
        return config().general.enableModContent;
    }

    /** 是否注册内置交易价目；需同时开启 {@link #enableModContent()}。 */
    public static boolean enableBuiltInTrades() {
        return enableModContent() && config().general.enableBuiltInTrades;
    }
}

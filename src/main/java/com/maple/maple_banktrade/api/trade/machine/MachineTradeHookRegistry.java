package com.maple.maple_banktrade.api.trade.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHooks.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 机器交易钩子注册表：管理可见性检查、额外检查、成功后续三种钩子的工厂。
 * 每个钩子通过 Identifier 索引，工厂接收配置对象（可为 null）返回对应的钩子实例。
 */
public final class MachineTradeHookRegistry {

    public static void init() {}

    private static final Map<Identifier, VisibilityCheckFactory> VISIBILITY_FACTORIES = new LinkedHashMap<>();
    private static final Map<Identifier, CheckHookFactory> CHECK_FACTORIES = new LinkedHashMap<>();
    private static final Map<Identifier, SuccessHookFactory> SUCCESS_FACTORIES = new LinkedHashMap<>();

    // 默认钩子 ID
    public static final Identifier DEFAULT_VISIBILITY = MapleBankTrade.id("always_visible");
    public static final Identifier DEFAULT_CHECK = MapleBankTrade.id("pass");
    public static final Identifier DEFAULT_SUCCESS = MapleBankTrade.id("noop");

    static {
        // 注册默认钩子，配置被忽略
        registerVisibility(DEFAULT_VISIBILITY, _ -> MachineTradeHooks.ALWAYS_VISIBLE);
        registerCheck(DEFAULT_CHECK, _ -> MachineTradeHooks.PASS);
        registerSuccess(DEFAULT_SUCCESS, _ -> MachineTradeHooks.NOOP);
    }

    // ---------- 注册方法 ----------
    public static void registerVisibility(Identifier id, VisibilityCheckFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (VISIBILITY_FACTORIES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting visibility hook factory for id: {}", id);
        }
        VISIBILITY_FACTORIES.put(id, factory);
    }

    public static void registerCheck(Identifier id, CheckHookFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (CHECK_FACTORIES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting check hook factory for id: {}", id);
        }
        CHECK_FACTORIES.put(id, factory);
    }

    public static void registerSuccess(Identifier id, SuccessHookFactory factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (SUCCESS_FACTORIES.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("Overwriting success hook factory for id: {}", id);
        }
        SUCCESS_FACTORIES.put(id, factory);
    }

    // ---------- 获取钩子实例 ----------
    public static MachineTradeVisibilityCheck getVisibility(Identifier id, CompoundTag config) {
        if (id == null) id = DEFAULT_VISIBILITY;
        VisibilityCheckFactory factory = VISIBILITY_FACTORIES.get(id);
        if (factory == null) {
            MapleBankTrade.LOGGER.warn("Unknown visibility hook id: {}, using default", id);
            factory = VISIBILITY_FACTORIES.get(DEFAULT_VISIBILITY);
        }
        return factory.create(config);
    }

    public static MachineTradeCheckHook getCheck(Identifier id, CompoundTag config) {
        if (id == null) id = DEFAULT_CHECK;
        CheckHookFactory factory = CHECK_FACTORIES.get(id);
        if (factory == null) {
            MapleBankTrade.LOGGER.warn("Unknown check hook id: {}, using default", id);
            factory = CHECK_FACTORIES.get(DEFAULT_CHECK);
        }
        return factory.create(config);
    }

    public static MachineTradeSuccessHook getSuccess(Identifier id, CompoundTag config) {
        if (id == null) id = DEFAULT_SUCCESS;
        SuccessHookFactory factory = SUCCESS_FACTORIES.get(id);
        if (factory == null) {
            MapleBankTrade.LOGGER.warn("Unknown success hook id: {}, using default", id);
            factory = SUCCESS_FACTORIES.get(DEFAULT_SUCCESS);
        }
        return factory.create(config);
    }

    private MachineTradeHookRegistry() {}
}

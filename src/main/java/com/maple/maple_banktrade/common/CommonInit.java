package com.maple.maple_banktrade.common;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.WalletApiRegistration;
import com.maple.maple_banktrade.api.bank.item.BankDataComponent;
import com.maple.maple_banktrade.api.trade.base.registry.*;
import com.maple.maple_banktrade.api.trade.machine.MachineTradeHookRegistry;
import com.maple.maple_banktrade.common.bank.BankRegistration;
import com.maple.maple_banktrade.common.bank.CardRegistration;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.common.bank.TradableTypeRegistration;
import com.maple.maple_banktrade.common.trade.CheckHasRegister;
import com.maple.maple_banktrade.common.trade.CurrencyItemTradeRegistration;
import com.maple.maple_banktrade.common.trade.MachineTradeRegistration;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.maple.maple_banktrade.config.MBTModConfig;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.maple.maple_banktrade.api.trade.base.registry.TradeRpcHandlers.serializeEntryDataList;

public class CommonInit {

    public static void init(IEventBus modBus) {
        MBTModConfig.init();
        BankDataComponent.init();
        MBTLangHandler.init();
        MBTTab.init();

        // API：钱包物品、UI、银行世界数据、命令
        WalletApiRegistration.init(modBus);
        // 交易数据包注册
        TradeRpcHandlers.init();
        MachineTradeHookRegistry.init();

        // 内置内容：银行/卡/货币/交易站/价目（可整体关闭，仅保留 API）
        if (MBTModConfig.enableModContent()) {
            contentInit(modBus);
        } else {
            MapleBankTrade.LOGGER.info(
                    "[{}] enableModContent=false: skipped built-in content; API only",
                    MapleBankTrade.MODID);
        }
    }

    public static void contentInit(IEventBus modBus) {
        // 触发交易站等 DeferredRegister 静态登记
        MBTRegistration.init();
        // 初始化内置货币、交易类型显示、银行与银行卡
        CurrencyRegistration.init();
        TradableTypeRegistration.init();
        BankRegistration.init();
        CardRegistration.init();
        // 钩子注册
        CheckHasRegister.registerVisibilityFilterByName();
        // 各贸易站：物品 / 流体 / 能量能力（基类统一注册）
        modBus.addListener(MBTRegistration::registerTradingStationCapabilities);

        // 内置价目
        TradeTypeRegistration.init();
        NeoForge.EVENT_BUS.addListener(CommonInit::registerTradeServer);
        NeoForge.EVENT_BUS.addListener(CommonInit::onServerStopping);
        NeoForge.EVENT_BUS.addListener(CommonInit::onPlayerLoggedIn);
    }

    public static void registerTradeServer(ServerStartedEvent event) {
        if (MBTModConfig.enableBuiltInTrades()) {
            CurrencyItemTradeRegistration.register();
            MachineTradeRegistration.register();
        }
    }

    /**
     * 服务器启动完成后初始化交易数据。
     * 任务定义已改为双端初始化，不再需要此处的单独处理。
     */
    public static void onServerStarted(ServerStartedEvent event) {
        // 任务定义已改为双端初始化（在 init() 中），此方法保留用于未来扩展
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        for (TradeStorage storage : TradeRegistry.storages().values()) {
            if (storage instanceof AbstractTradeEntryStorage<?> entryStorage) {
                entryStorage.clearAllEntries();
            }
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().getServer().execute(() -> {
                MinecraftServer server = serverPlayer.level().getServer();
                HolderLookup.Provider provider = server.registryAccess();
                for (Identifier typeId : TradeRegistry.storages().keySet()) {
                    TradeStorage storage = TradeRegistry.storages().get(typeId);
                    if (storage instanceof AbstractTradeEntryStorage<?> entryStorage) {
                        List<TradeRpcHandlers.TradeEntryData> allData = new ArrayList<>();
                        for (Map.Entry<Identifier, ?> e : entryStorage.entries().entrySet()) {
                            Object value = e.getValue();
                            if (value instanceof TradeInfo) {
                                CompoundTag tag = PersistedParser.serializeNBT(value, provider);
                                allData.add(new TradeRpcHandlers.TradeEntryData(e.getKey(), tag));
                            }
                        }
                        CompoundTag data = serializeEntryDataList(allData);
                        RPCPacketDistributor.rpcToPlayer(serverPlayer, "trade_full_sync", typeId, data);
                        MapleBankTrade.LOGGER.info("Full sync sent for {} to player {}, entries: {}", typeId, serverPlayer.getName().getString(), allData.size());
                    }
                }
            });
        }
    }
}

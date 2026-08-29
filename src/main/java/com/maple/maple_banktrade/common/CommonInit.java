package com.maple.maple_banktrade.common;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
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
import com.maple.maple_banktrade.collaboration.ftbq.reward.ELFTRewardTypes;
import com.maple.maple_banktrade.collaboration.ftbq.task.ELFTTaskTypes;
import com.maple.maple_banktrade.common.bank.BankRegistration;
import com.maple.maple_banktrade.common.bank.CardRegistration;
import com.maple.maple_banktrade.common.bank.CurrencyRegistration;
import com.maple.maple_banktrade.common.bank.TradableTypeRegistration;
import com.maple.maple_banktrade.common.infoList.InfoListRegistration;
import com.maple.maple_banktrade.common.trade.CurrencyItemTradeRegistration;
import com.maple.maple_banktrade.common.trade.MachineTradeRegistration;
import com.maple.maple_banktrade.common.trade.TradeTypeRegistration;
import com.maple.maple_banktrade.config.MBTModConfig;
import com.maple.maple_banktrade.data.lang.MBTLangHandler;
import com.mapleutillib.utils.task.TaskHandler;
import com.mapleutillib.utils.task.TickableSubscription;

import java.util.*;

import static com.maple.maple_banktrade.common.MBTLevelTask.TASKS;

public class CommonInit {

    public static void init(IEventBus modBus) {
        MBTModConfig.init();
        BankDataComponent.init();
        MBTLangHandler.init();
        MBTTab.init();

        TaskHandler.registerAttachment(MBTLevelTask.LEVEL_TASK_DATA);

        // API：钱包物品、UI、银行世界数据、命令
        WalletApiRegistration.init(modBus);
        // 交易数据包注册
        TradeRpcHandlers.init();

        if (ModList.get().isLoaded("ftbquests")) {
            ELFTTaskTypes.init();
            ELFTRewardTypes.init();
        }

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
        InfoListRegistration.init();
        BankRegistration.init();
        CardRegistration.init();
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

    public static void onServerStopping(ServerStoppingEvent event) {
        for (TradeStorage storage : TradeRegistry.storages().values()) {
            if (storage instanceof AbstractTradeEntryStorage<?> entryStorage) {
                entryStorage.clearAllEntries();
            }
        }
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        ServerLevel level = serverPlayer.serverLevel();

        MinecraftServer server = serverPlayer.level().getServer();
        HolderLookup.Provider provider = server.registryAccess();

        List<ResourceLocation> typeIds = new ArrayList<>();
        for (ResourceLocation typeId : TradeRegistry.storages().keySet()) {
            TradeStorage storage = TradeRegistry.storages().get(typeId);
            if (storage instanceof AbstractTradeEntryStorage<?> entryStorage && !entryStorage.isEmpty()) {
                typeIds.add(typeId);
            }
        }
        if (typeIds.isEmpty()) return;

        final int[] index = { 0 };
        final TickableSubscription<?>[] subRef = new TickableSubscription<?>[1];

        Runnable sendTask = () -> {
            if (serverPlayer.hasDisconnected()) {
                if (subRef[0] != null) subRef[0].unsubscribe();
                return;
            }
            if (index[0] >= typeIds.size()) {
                if (subRef[0] != null) subRef[0].unsubscribe();
                return;
            }
            ResourceLocation typeId = typeIds.get(index[0]++);
            TradeStorage storage = TradeRegistry.storages().get(typeId);
            if (storage instanceof AbstractTradeEntryStorage<?> entryStorage) {
                ListTag listTag = new ListTag();
                for (TradeInfo value : entryStorage.entries().values()) {
                    listTag.add(PersistedParser.serializeNBT(value, provider));
                }
                RPCPacketDistributor.rpcToPlayer(serverPlayer, "trade_full_sync", typeId, listTag);
                MapleBankTrade.LOGGER.info("Full sync sent for {} to player {}, entries: {}", typeId, serverPlayer.getName().getString(), listTag.size());
            }
        };

        TickableSubscription<?> sub = TASKS.enqueueTick(level, () -> false, sendTask, 1, 0);
        subRef[0] = sub;
    }
}

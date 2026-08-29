package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TradeRpcHandlers {

    public static void init() {}

    // ==========================================================
    // 1. 全量同步 RPC：服务端 -> 客户端
    // 客户端收到后替换整个 clientEntries
    // ==========================================================
    @RPCPacket("trade_full_sync")
    public static void handleFullSync(RPCSender sender, ResourceLocation tradeTypeId, ListTag listTag) {
        AbstractTradeEntryStorage<?> storage = findStorage(tradeTypeId);
        if (storage == null) return;
        HolderLookup.Provider provider = getProvider(sender);
        Map<ResourceLocation, TradeInfo> newMap = new LinkedHashMap<>();
        for (Tag tag : listTag) {
            if (tag instanceof CompoundTag entryTag) {
                TradeInfo entry = storage.createEmptyEntry();
                PersistedParser.deserializeNBT(entryTag, entry, provider);
                newMap.put(entry.id(), entry);
            }
        }
        storage.replaceClientCache(newMap);
        MapleBankTrade.LOGGER.info("Trade full entries received {}, entry: {}", tradeTypeId, newMap.size());
        NeoForge.EVENT_BUS.post(new TradeSyncEvent(tradeTypeId));
    }

    // ==========================================================
    // 2. 增量同步 RPC：服务端 -> 客户端
    // 用于添加或更新单个/多个条目（客户端合并到 clientEntries）
    // ==========================================================
    @RPCPacket("trade_sync_entries")
    public static void handleSyncEntries(RPCSender sender, ResourceLocation tradeTypeId, ListTag listTag) {
        AbstractTradeEntryStorage<?> storage = findStorage(tradeTypeId);
        if (storage == null) return;
        HolderLookup.Provider provider = getProvider(sender);
        for (Tag tag : listTag) {
            if (tag instanceof CompoundTag entryTag)
                deserializeAndUpdate(storage, entryTag, provider);
        }
        MapleBankTrade.LOGGER.info("Trade sync entries received {}, entry: {}", tradeTypeId, listTag.size());
        NeoForge.EVENT_BUS.post(new TradeSyncEvent(tradeTypeId));
    }

    // ==========================================================
    // 辅助方法
    // ==========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void deserializeAndUpdate(
                                             AbstractTradeEntryStorage storage,
                                             CompoundTag nbt,
                                             HolderLookup.Provider provider) {
        TradeInfo entry = storage.createEmptyEntry();
        PersistedParser.deserializeNBT(nbt, entry, provider);
        storage.updateClientEntry(entry.id(), entry);
    }

    private static AbstractTradeEntryStorage<?> findStorage(ResourceLocation tradeTypeId) {
        if (TradeRegistry.findStorage(tradeTypeId).orElse(null) instanceof AbstractTradeEntryStorage<?> storage)
            return storage;
        return null;
    }

    private static HolderLookup.Provider getProvider(RPCSender sender) {
        ServerPlayer player = sender.asPlayer();
        if (player != null) {
            Level level = player.level();
            return level.registryAccess();
        }
        if (!Platform.isClient()) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) return server.registryAccess();
        }
        if (Platform.isClient()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) return mc.getConnection().registryAccess();
            if (mc.level != null) return mc.level.registryAccess();
        }
        return RegistryAccess.EMPTY;
    }
}

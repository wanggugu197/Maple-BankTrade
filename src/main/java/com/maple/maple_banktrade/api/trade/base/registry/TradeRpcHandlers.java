package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacket;
import com.lowdragmc.lowdraglib2.syncdata.rpc.RPCSender;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TradeRpcHandlers {

    public static void init() {}

    // ---------- 数据结构 ----------
    public record TradeEntryData(Identifier entryId, CompoundTag nbt) {}

    // ---------- 序列化/反序列化辅助 ----------
    public static CompoundTag serializeEntryDataList(List<TradeEntryData> entries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (TradeEntryData data : entries) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", data.entryId().toString());
            entry.put("nbt", data.nbt());
            list.add(entry);
        }
        root.put("entries", list);
        return root;
    }

    private static List<TradeEntryData> deserializeEntryDataList(CompoundTag root) {
        List<TradeEntryData> entries = new ArrayList<>();
        ListTag list = root.getListOrEmpty("entries");
        for (Tag tag : list) {
            if (tag instanceof CompoundTag entry) {
                Identifier id = Identifier.tryParse(entry.getStringOr("id", ""));
                CompoundTag nbt = entry.getCompoundOrEmpty("nbt");
                if (id != null) {
                    entries.add(new TradeEntryData(id, nbt));
                }
            }
        }
        return entries;
    }

    // ==========================================================
    // 1. 全量同步 RPC：服务端 -> 客户端
    // 客户端收到后替换整个 clientEntries
    // ==========================================================
    @RPCPacket("trade_full_sync")
    public static void handleFullSync(RPCSender sender, Identifier tradeTypeId, CompoundTag data) {
        AbstractTradeEntryStorage<?> storage = findStorage(tradeTypeId);
        if (storage == null) return;

        HolderLookup.Provider provider = getProvider(sender);
        List<TradeEntryData> entries = deserializeEntryDataList(data);

        Map<Identifier, TradeInfo> newMap = new LinkedHashMap<>();
        for (TradeEntryData entryData : entries) {
            TradeInfo entry = storage.createEmptyEntry();
            PersistedParser.deserializeNBT(entryData.nbt(), entry, provider);
            newMap.put(entryData.entryId(), entry);
        }
        storage.replaceClientCache(newMap);

        NeoForge.EVENT_BUS.post(new TradeSyncEvent(tradeTypeId));
    }

    // ==========================================================
    // 2. 增量同步 RPC：服务端 -> 客户端
    // 用于添加或更新单个/多个条目（客户端合并到 clientEntries）
    // ==========================================================
    @RPCPacket("trade_sync_entries")
    public static void handleSyncEntries(RPCSender sender, Identifier tradeTypeId, CompoundTag data) {
        AbstractTradeEntryStorage<?> storage = findStorage(tradeTypeId);
        if (storage == null) return;

        HolderLookup.Provider provider = getProvider(sender);
        List<TradeEntryData> entries = deserializeEntryDataList(data);

        for (TradeEntryData entryData : entries) {
            deserializeAndUpdate(storage, entryData.entryId(), entryData.nbt(), provider);
        }

        NeoForge.EVENT_BUS.post(new TradeSyncEvent(tradeTypeId));
    }

    // ==========================================================
    // 辅助方法
    // ==========================================================
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void deserializeAndUpdate(
                                             AbstractTradeEntryStorage storage,
                                             Identifier entryId,
                                             CompoundTag nbt,
                                             HolderLookup.Provider provider) {
        TradeInfo entry = storage.createEmptyEntry();
        PersistedParser.deserializeNBT(nbt, entry, provider);
        storage.updateClientEntry(entryId, entry);
    }

    private static AbstractTradeEntryStorage<?> findStorage(Identifier tradeTypeId) {
        return TradeRegistry.findStorage(tradeTypeId)
                .filter(AbstractTradeEntryStorage.class::isInstance)
                .map(AbstractTradeEntryStorage.class::cast)
                .orElse(null);
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

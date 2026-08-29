package com.maple.maple_banktrade.api.trade.base.registry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.maple.maple_banktrade.MapleBankTrade;

import java.util.*;

public abstract class AbstractTradeEntryStorage<E extends TradeInfo> implements TradeEntryStorage<E> {

    private final ResourceLocation tradeTypeId;

    // 服务端实际存储
    private final Map<ResourceLocation, E> entries = new LinkedHashMap<>();

    // 客户端同步缓存（完整数据）
    private final Map<ResourceLocation, E> clientEntries = new LinkedHashMap<>();

    // 客户端元数据缓存（ID → 版本号）
    private final Map<ResourceLocation, Integer> metaCache = new LinkedHashMap<>();

    protected AbstractTradeEntryStorage(ResourceLocation tradeTypeId) {
        this.tradeTypeId = Objects.requireNonNull(tradeTypeId, "tradeTypeId");
    }

    @Override
    public ResourceLocation tradeTypeId() {
        return tradeTypeId;
    }

    /**
     * 判断当前是否在服务端环境（而非客户端）
     */
    private boolean isServer() {
        return ServerLifecycleHooks.getCurrentServer() != null;
    }

    @Override
    public Optional<E> find(ResourceLocation tradeId) {
        if (tradeId == null) return Optional.empty();
        if (isServer()) {
            return Optional.ofNullable(entries.get(tradeId));
        } else {
            return Optional.ofNullable(clientEntries.get(tradeId));
        }
    }

    @Override
    public E require(ResourceLocation tradeId) {
        if (tradeId == null) return null;
        if (isServer()) {
            return entries.get(tradeId);
        } else {
            return clientEntries.get(tradeId);
        }
    }

    @Override
    public Map<ResourceLocation, E> entries() {
        if (isServer()) {
            return Collections.unmodifiableMap(entries);
        } else {
            return Collections.unmodifiableMap(clientEntries);
        }
    }

    /**
     * 子类必须实现，返回一个空的条目实例，用于客户端反序列化。
     */
    protected abstract E createEmptyEntry();

    @Override
    public E register(E entry) {
        Objects.requireNonNull(entry, "entry");
        if (!isValidEntry(entry)) {
            throw new IllegalArgumentException("Invalid trade entry: " + tradeTypeId + "/" + entry.id());
        }

        E existing = entries.get(entry.id());
        if (existing != null) {
            MapleBankTrade.LOGGER.error("Trade entry already exists: {}/{}", tradeTypeId, entry.id());
            return existing;
        }
        entries.put(entry.id(), entry);
        return entry;
    }

    private void broadcastEntryChange(E entry) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        HolderLookup.Provider provider = server.registryAccess();
        ListTag listTag = new ListTag();
        listTag.add(PersistedParser.serializeNBT(entry, provider));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RPCPacketDistributor.rpcToPlayer(player, "trade_sync_entries", this.tradeTypeId, listTag);
        }
    }

    // ---------- 客户端专用方法 ----------
    public void replaceClientCache(Map<ResourceLocation, ? extends TradeInfo> newEntries) {
        if (!Platform.isClient()) return;
        clientEntries.clear();
        for (Map.Entry<ResourceLocation, ? extends TradeInfo> e : newEntries.entrySet()) {
            @SuppressWarnings("unchecked")
            E entry = (E) e.getValue();
            clientEntries.put(e.getKey(), entry);
        }
    }

    public void updateClientEntry(ResourceLocation entryId, E entry) {
        if (!Platform.isClient()) return;
        clientEntries.put(entryId, entry);
    }

    public boolean hasClientData() {
        return !clientEntries.isEmpty();
    }

    public void clearAllEntries() {
        if (isServer()) {
            entries.clear();
            clientEntries.clear();
            metaCache.clear();
            onClear();
        }
    }

    protected void onClear() {}

    // ---------- 辅助 ----------
    private int getVersion(E entry) {
        return entry != null ? entry.hashCode() : 0;
    }

    protected abstract boolean isValidEntry(E entry);
}

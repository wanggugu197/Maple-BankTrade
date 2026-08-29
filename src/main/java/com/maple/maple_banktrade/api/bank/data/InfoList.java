package com.maple.maple_banktrade.api.bank.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.maple.maple_banktrade.MapleBankTrade;

import java.util.*;

/**
 * 信息列表定义：包含一组信息条目，每个条目有 ID、显示组件和所需完成次数。
 * 通过 nameIndex（即卡片的名称索引）进行注册和查找。
 *
 * @param id 与卡的 nameIndex 对应
 */
public record InfoList(ResourceLocation id,
                       LinkedHashMap<String, InfoEntry> entries,
                       List<Component> description,
                       IGuiTexture backgroundTexture) {

    public record InfoEntry(String id, Component display, int requiredCount, short tire) {

        public InfoEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(display, "display");
            if (requiredCount <= 0) {
                throw new IllegalArgumentException("requiredCount must be > 0");
            }
        }
    }

    // ==============================================
    // 注册表
    // ==============================================

    private static final Map<ResourceLocation, InfoList> REGISTRY = new LinkedHashMap<>();

    // ==============================================
    // 注册
    // ==============================================

    public static InfoList register(ResourceLocation id,
                                    List<Component> description,
                                    IGuiTexture backgroundTexture) {
        if (REGISTRY.containsKey(id)) {
            MapleBankTrade.LOGGER.warn("InfoList with id {} already exists, returning existing", id);
            return REGISTRY.get(id);
        }
        InfoList list = new InfoList(id, new LinkedHashMap<>(), description, backgroundTexture);
        REGISTRY.put(id, list);
        return list;
    }

    public static InfoList requireByNameIndex(ResourceLocation nameIndex) {
        return REGISTRY.get(nameIndex);
    }

    // ==============================================
    // 查询方法
    // ==============================================

    public InfoEntry getEntry(String entryId) {
        return entries.get(entryId);
    }

    public void addEntry(InfoEntry infoEntry) {
        Objects.requireNonNull(infoEntry, "entry");
        if (entries.containsKey(infoEntry.id())) {
            MapleBankTrade.LOGGER.warn("InfoList {} already contains entry {}, overwriting", id, infoEntry.id());
        }
        entries.put(infoEntry.id, infoEntry);
    }

    public void addEntries(InfoEntry... infoEntry) {
        for (InfoEntry info : infoEntry) addEntry(info);
    }

    public void addEntries(List<InfoEntry> infoEntry) {
        infoEntry.forEach(this::addEntry);
    }
}

package com.maple.maple_banktrade.api.bank.base;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.maple.maple_banktrade.MapleBankTrade;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;
import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * 世界持久化银行数据：全服银行卡集合与玩家持卡权限表。
 */
public class BankCardsWorldData extends SavedData {

    // ==============================================
    // 字段
    // ==============================================

    /** 全局银行卡存储：卡 UUID -> 银行卡实体。 */
    private final Map<UUID, BankCard> cards;
    /** 玩家持卡权限表：玩家 UUID -> {卡 UUID -> 权限}。 */
    private final Map<UUID, Map<UUID, BankCardPermission>> cardPermissions;

    // ==============================================
    // Codec
    // ==============================================

    /** 权限枚举 Codec。 */
    private static final Codec<BankCardPermission> CARD_PERMISSION_CODEC = StringRepresentable.fromEnum(BankCardPermission::values);
    /** UUID 作为 NBT/Map key 的字符串 Codec。 */
    private static final Codec<UUID> UUID_STRING_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UUID string: " + value);
        }
    }, UUID::toString);
    /** 严格银行卡列表 Codec，用于写入。 */
    private static final Codec<List<BankCard>> STRICT_CARDS_CODEC = Codec.list(BankCard.CODEC);
    /** 严格权限表 Codec，用于写入。 */
    private static final Codec<Map<UUID, Map<UUID, BankCardPermission>>> STRICT_CARD_PERMISSIONS_CODEC = Codec.unboundedMap(UUID_STRING_CODEC, Codec.unboundedMap(UUID_STRING_CODEC, CARD_PERMISSION_CODEC));
    /** 宽松银行卡列表 Codec，读取时跳过无法识别的卡。 */
    private static final Codec<List<BankCard>> CARDS_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(List<BankCard> input, DynamicOps<T> ops, T prefix) {
            return STRICT_CARDS_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<List<BankCard>, T>> decode(DynamicOps<T> ops, T input) {
            return decodeCards(ops, input);
        }
    };
    /** 宽松权限表 Codec，读取时跳过无法识别的权限记录。 */
    private static final Codec<Map<UUID, Map<UUID, BankCardPermission>>> CARD_PERMISSIONS_CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Map<UUID, Map<UUID, BankCardPermission>> input, DynamicOps<T> ops, T prefix) {
            return STRICT_CARD_PERMISSIONS_CODEC.encode(input, ops, prefix);
        }

        @Override
        public <T> DataResult<Pair<Map<UUID, Map<UUID, BankCardPermission>>, T>> decode(DynamicOps<T> ops, T input) {
            return decodeCardPermissions(ops, input);
        }
    };

    /** 世界数据序列化编解码器。 */
    public static final Codec<BankCardsWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CARDS_CODEC
                    .fieldOf("cards")
                    .forGetter(BankCardsWorldData::cardList),
            CARD_PERMISSIONS_CODEC
                    .optionalFieldOf("card_permissions", Map.of())
                    .forGetter(BankCardsWorldData::cardPermissions))
            .apply(instance, BankCardsWorldData::new));

    /** 全局 SavedData 类型；文件为 data/maple_banktrade/bank_cards.dat。 */
    public static final SavedDataType<BankCardsWorldData> TYPE = new SavedDataType<>(
            MapleBankTrade.id("bank_cards"),
            BankCardsWorldData::new,
            CODEC);

    // ==============================================
    // 构造
    // ==============================================

    /** 创建空白银行世界数据（新世界初始化）。 */
    public BankCardsWorldData() {
        this(List.of(), Map.of());
    }

    /** 反序列化构造：写入银行卡并恢复权限，跳过无效记录。 */
    private BankCardsWorldData(List<BankCard> cards, Map<UUID, Map<UUID, BankCardPermission>> cardPermissions) {
        this.cards = new HashMap<>();
        this.cardPermissions = new HashMap<>();
        // 写入银行卡并自动去重
        cards.forEach(card -> {
            if (this.cards.containsKey(card.getCardUuid())) {
                MapleBankTrade.LOGGER.error("跳过重复 UUID 的银行卡: {}", card.getCardUuid());
                return;
            }
            this.cards.put(card.getCardUuid(), card);
        });
        // 批量恢复权限记录
        cardPermissions.forEach((playerUuid, permissions) -> permissions.forEach((cardUuid, permission) -> {
            if (!this.cards.containsKey(cardUuid)) {
                MapleBankTrade.LOGGER.error("跳过玩家 {} 对不存在银行卡 {} 的权限: {}", playerUuid, cardUuid, permission);
                return;
            }
            putPermission(playerUuid, cardUuid, permission);
        }));
    }

    // ==============================================
    // 查询
    // ==============================================

    /** 序列化辅助：将银行卡 Map 转为列表。 */
    private List<BankCard> cardList() {
        return List.copyOf(cards.values());
    }

    /** 获取完整权限只读视图；并清理指向不存在卡的残留权限。 */
    public Map<UUID, Map<UUID, BankCardPermission>> cardPermissions() {
        if (removePermissionsIf(cardUuid -> !cards.containsKey(cardUuid))) setDirty();
        Map<UUID, Map<UUID, BankCardPermission>> result = new HashMap<>();
        cardPermissions.forEach((playerUuid, permissions) -> result.put(playerUuid, Collections.unmodifiableMap(permissions)));
        return Collections.unmodifiableMap(result);
    }

    /** 根据卡 UUID 查询银行卡实体。 */
    public BankCard getCard(UUID cardUuid) {
        if (cardUuid == null) return null;
        return cards.get(cardUuid);
    }

    /** 获取当前世界已注册银行卡总数量。 */
    public int size() {
        return cards.size();
    }

    // ==============================================
    // 修改
    // ==============================================

    /** 按创建定义新建银行卡，并将指定玩家绑定为拥有者。 */
    public BankCard createCard(BankCardFactory cardFactory, UUID ownerUuid) {
        if (cardFactory == null || ownerUuid == null) return null;
        UUID cardUuid;
        do {
            cardUuid = UUID.randomUUID();
        } while (cards.containsKey(cardUuid));
        BankCard card = cardFactory.create(cardUuid);
        if (card != null) {
            cards.put(card.getCardUuid(), card);
            setPermission(ownerUuid, card.getCardUuid(), BankCardPermission.OWNER);
            setDirty();
        }
        return card;
    }

    /** 删除指定银行卡，并清除所有玩家对该卡的权限。 */
    public void removeCard(UUID cardUuid) {
        if (cardUuid == null) return;
        boolean removedCard = cards.remove(cardUuid) != null;
        boolean removedPermissions = removePermissionsIf(cardUuid::equals);
        if (removedCard || removedPermissions) {
            setDirty();
        }
    }

    // ==============================================
    // 权限查询
    // ==============================================

    /** 查询玩家对指定银行卡的权限等级。 */
    public BankCardPermission getPermission(UUID playerUuid, UUID cardUuid) {
        if (playerUuid == null || cardUuid == null) return BankCardPermission.UNUSABLE;
        if (!ensureCardExists(cardUuid)) return BankCardPermission.UNUSABLE;
        Map<UUID, BankCardPermission> permissions = cardPermissions.get(playerUuid);
        return permissions == null ? BankCardPermission.UNUSABLE : permissions.getOrDefault(cardUuid, BankCardPermission.UNUSABLE);
    }

    /** 判断玩家是否拥有该卡使用权限（OWNER / ADMIN / USABLE）。 */
    public boolean canUse(UUID playerUuid, UUID cardUuid) {
        return getPermission(playerUuid, cardUuid).canUse();
    }

    /** 判断玩家是否拥有该卡管理权限（OWNER / ADMIN）。 */
    public boolean canManage(UUID playerUuid, UUID cardUuid) {
        return getPermission(playerUuid, cardUuid).canManage();
    }

    /** 判断玩家是否为该卡拥有者。 */
    public boolean isOwner(UUID playerUuid, UUID cardUuid) {
        return getPermission(playerUuid, cardUuid).isOwner();
    }

    /** 获取指定玩家名下全部持卡权限只读视图。 */
    public Map<UUID, BankCardPermission> getPermissionsForPlayer(UUID playerUuid) {
        if (playerUuid == null) return Collections.emptyMap();
        if (removePermissionsIf(cardUuid -> !cards.containsKey(cardUuid))) {
            setDirty();
        }
        Map<UUID, BankCardPermission> permissions = cardPermissions.get(playerUuid);
        return permissions == null ? Collections.emptyMap() : Collections.unmodifiableMap(permissions);
    }

    /** 获取指定玩家当前可以使用的全部银行卡实体。 */
    public List<BankCard> getUsableCardsForPlayer(UUID playerUuid) {
        if (playerUuid == null) return Collections.emptyList();
        Map<UUID, BankCardPermission> permissions = getPermissionsForPlayer(playerUuid);
        if (permissions.isEmpty()) return Collections.emptyList();

        List<BankCard> result = new ArrayList<>();
        permissions.entrySet().stream()
                .filter(entry -> entry.getValue().canUse())
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .forEach(entry -> {
                    BankCard card = cards.get(entry.getKey());
                    if (card != null) {
                        result.add(card);
                    }
                });
        return Collections.unmodifiableList(result);
    }

    /** 按名称索引查询玩家当前可用的银行卡。 */
    public List<BankCard> getUsableCardsForPlayerByNameIndex(UUID playerUuid, Identifier nameIndex) {
        if (playerUuid == null || nameIndex == null) return Collections.emptyList();
        return getUsableCardsForPlayer(playerUuid).stream()
                .filter(card -> nameIndex.equals(card.getNameIndex()))
                .toList();
    }

    /**
     * 按名称索引查询玩家当前可用的银行卡 UUID 列表。
     * <p>
     * 仅包含 {@link BankCardPermission#canUse()} 的卡；同一 nameIndex 可对应多张卡。
     * </p>
     */
    public List<UUID> getCardUuidsForPlayerByNameIndex(UUID playerUuid, Identifier nameIndex) {
        return getUsableCardsForPlayerByNameIndex(playerUuid, nameIndex).stream()
                .map(BankCard::getCardUuid)
                .toList();
    }

    /**
     * 按名称索引选取玩家权限最高的一张可用卡（OWNER &gt; ADMIN &gt; USABLE）。
     * 同级时按 cardUuid 字符串排序取较小者，保证结果稳定。
     *
     * @return 无匹配可用卡时返回 null
     */
    @Nullable
    public BankCard getHighestPermissionCardForPlayerByNameIndex(UUID playerUuid, Identifier nameIndex) {
        if (playerUuid == null || nameIndex == null) return null;
        Map<UUID, BankCardPermission> permissions = getPermissionsForPlayer(playerUuid);
        if (permissions.isEmpty()) return null;

        BankCard best = null;
        BankCardPermission bestPerm = null;
        for (Map.Entry<UUID, BankCardPermission> entry : permissions.entrySet()) {
            BankCardPermission perm = entry.getValue();
            if (perm == null || !perm.canUse()) continue;
            BankCard card = cards.get(entry.getKey());
            if (card == null || !nameIndex.equals(card.getNameIndex())) continue;

            if (best == null || BankCardPermission.compareRankDesc(perm, bestPerm) < 0 || (BankCardPermission.compareRankDesc(perm, bestPerm) == 0 && card.getCardUuid().toString().compareTo(best.getCardUuid().toString()) < 0)) {
                best = card;
                bestPerm = perm;
            }
        }
        return best;
    }

    /**
     * 按名称索引选取玩家权限最高的一张可用卡 UUID（OWNER &gt; ADMIN &gt; USABLE）。
     *
     * @return 无匹配可用卡时返回 null
     */
    @Nullable
    public UUID getHighestPermissionCardUuidForPlayerByNameIndex(UUID playerUuid, Identifier nameIndex) {
        BankCard card = getHighestPermissionCardForPlayerByNameIndex(playerUuid, nameIndex);
        return card == null ? null : card.getCardUuid();
    }

    /** 获取玩家拥有可用卡的银行 ID 集合（仅 canUse 计入）。 */
    public Set<Identifier> getBankTypeIdsForPlayer(UUID playerUuid) {
        if (playerUuid == null) return Collections.emptySet();
        Map<UUID, BankCardPermission> permissions = getPermissionsForPlayer(playerUuid);
        if (permissions.isEmpty()) return Collections.emptySet();

        Set<Identifier> result = new HashSet<>();
        permissions.forEach((cardUuid, permission) -> {
            if (!permission.canUse()) return;
            BankCard card = cards.get(cardUuid);
            if (card != null) {
                result.add(card.getBankTypeId());
            }
        });
        return Collections.unmodifiableSet(result);
    }

    /** 获取玩家拥有可用卡的银行列表，顺序与 BankType 注册顺序一致。 */
    public List<Identifier> getBankTypesForPlayer(UUID playerUuid) {
        Set<Identifier> bankTypeIds = getBankTypeIdsForPlayer(playerUuid);
        if (bankTypeIds.isEmpty()) return Collections.emptyList();

        List<Identifier> result = new ArrayList<>();
        BankType.values().forEach(bankType -> {
            if (bankTypeIds.contains(bankType.id())) {
                result.add(bankType.id());
            }
        });
        return Collections.unmodifiableList(result);
    }

    /** 查询玩家在指定银行拥有的全部可用银行卡 UUID。 */
    public Set<UUID> getCardsForPlayerInBank(UUID playerUuid, BankType bankType) {
        if (bankType == null) return Collections.emptySet();
        return getCardsForPlayerInBank(playerUuid, bankType.id());
    }

    /** 查询玩家在指定银行 ID 下拥有的全部可用银行卡 UUID。 */
    public Set<UUID> getCardsForPlayerInBank(UUID playerUuid, Identifier bankTypeId) {
        if (playerUuid == null || bankTypeId == null) return Collections.emptySet();
        Map<UUID, BankCardPermission> permissions = getPermissionsForPlayer(playerUuid);
        if (permissions.isEmpty()) return Collections.emptySet();

        Set<UUID> result = new HashSet<>();
        permissions.forEach((cardUuid, permission) -> {
            if (!permission.canUse()) return;
            BankCard card = cards.get(cardUuid);
            if (card != null && bankTypeId.equals(card.getBankTypeId())) {
                result.add(cardUuid);
            }
        });
        return Collections.unmodifiableSet(result);
    }

    /** 权限表外层出现过的全部玩家 UUID（无筛选）。 */
    public Set<UUID> getAllPlayersInPermissionTable() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(cardPermissions.keySet()));
    }

    /** 查询指定卡在权限表中登记过的全部玩家权限（含 UNUSABLE）。 */
    public Map<UUID, BankCardPermission> getPermissionsForCard(UUID cardUuid) {
        if (cardUuid == null) return Collections.emptyMap();
        if (!ensureCardExists(cardUuid)) return Collections.emptyMap();
        Map<UUID, BankCardPermission> result = new LinkedHashMap<>();
        cardPermissions.forEach((playerUuid, permissions) -> {
            if (permissions == null) return;
            BankCardPermission permission = permissions.get(cardUuid);
            if (permission != null) {
                result.put(playerUuid, permission);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /** 查询指定卡的可用权限持有者（OWNER / ADMIN / USABLE）。 */
    public Map<UUID, BankCardPermission> getUsablePermissionsForCard(UUID cardUuid) {
        Map<UUID, BankCardPermission> result = new LinkedHashMap<>();
        getPermissionsForCard(cardUuid).forEach((playerUuid, permission) -> {
            if (permission != null && permission.canUse()) {
                result.put(playerUuid, permission);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    // ==============================================
    // 权限修改
    // ==============================================

    /** 设置玩家对银行卡的访问权限，重复写入覆盖旧权限。 */
    public void setPermission(UUID playerUuid, UUID cardUuid, BankCardPermission permission) {
        if (playerUuid == null || cardUuid == null || permission == null) return;
        if (!ensureCardExists(cardUuid)) return;
        if (putPermission(playerUuid, cardUuid, permission)) {
            setDirty();
        }
    }

    /**
     * 移除单个玩家对单张银行卡的权限。
     */
    public void removePermission(UUID playerUuid, UUID cardUuid) {
        if (playerUuid == null || cardUuid == null) return;
        if (!cards.containsKey(cardUuid)) {
            boolean removed = removePermissionsIf(cardUuid::equals);
            if (removed) setDirty();
            return;
        }
        Map<UUID, BankCardPermission> permissions = cardPermissions.get(playerUuid);
        if (permissions == null || permissions.remove(cardUuid) == null) {
            return;
        }
        if (permissions.isEmpty()) {
            cardPermissions.remove(playerUuid);
        }
        setDirty();
    }

    /**
     * 由管理者向目标授予权限。
     * OWNER 可授 ADMIN/USABLE；ADMIN 仅可授 USABLE 且不能覆盖更高权限；不可改动拥有者、不可授予 OWNER。
     */
    public void grantPermission(UUID actorUuid, UUID targetUuid, UUID cardUuid, BankCardPermission granted) {
        if (actorUuid == null || targetUuid == null || cardUuid == null || granted == null) return;
        if (!canManage(actorUuid, cardUuid)) return;
        if (granted.isOwner() || granted == BankCardPermission.UNUSABLE) return;
        BankCardPermission targetCurrent = getPermission(targetUuid, cardUuid);
        if (targetCurrent.isOwner()) return;

        BankCardPermission actor = getPermission(actorUuid, cardUuid);
        if (actor.isOwner()) {
            if (granted != BankCardPermission.ADMIN && granted != BankCardPermission.USABLE) return;
            setPermission(targetUuid, cardUuid, granted);
            return;
        }
        if (actor == BankCardPermission.ADMIN) {
            if (granted != BankCardPermission.USABLE) return;
            if (targetCurrent.canManage()) return;
            setPermission(targetUuid, cardUuid, BankCardPermission.USABLE);
        }
    }

    /**
     * 由管理者撤销目标权限。
     * 不可撤销拥有者；OWNER 可撤 ADMIN/USABLE；ADMIN 仅可撤 USABLE。
     */
    public void revokeManagedPermission(UUID actorUuid, UUID targetUuid, UUID cardUuid) {
        if (actorUuid == null || targetUuid == null || cardUuid == null) return;
        if (!canManage(actorUuid, cardUuid)) return;
        BankCardPermission targetCurrent = getPermission(targetUuid, cardUuid);
        if (!targetCurrent.canUse() || targetCurrent.isOwner()) return;

        BankCardPermission actor = getPermission(actorUuid, cardUuid);
        if (actor.isOwner()) {
            removePermission(targetUuid, cardUuid);
            return;
        }
        if (actor == BankCardPermission.ADMIN) {
            if (targetCurrent != BankCardPermission.USABLE) return;
            removePermission(targetUuid, cardUuid);
        }
    }

    /** 拥有者删除银行卡；成功返回 true。 */
    public boolean deleteCardAsOwner(UUID actorUuid, UUID cardUuid) {
        if (!isOwner(actorUuid, cardUuid)) return false;
        if (!cards.containsKey(cardUuid)) return false;
        removeCard(cardUuid);
        return true;
    }

    // ==============================================
    // 工具
    // ==============================================

    /** 确认银行卡存在；若不存在则清理指向该卡的权限残留。 */
    private boolean ensureCardExists(UUID cardUuid) {
        if (cards.containsKey(cardUuid)) return true;
        if (removePermissionsIf(cardUuid::equals)) setDirty();
        return false;
    }

    /** 写入权限记录；返回权限表是否实际变化。 */
    private boolean putPermission(UUID playerUuid, UUID cardUuid, BankCardPermission permission) {
        Map<UUID, BankCardPermission> permissions = cardPermissions.computeIfAbsent(playerUuid, ignored -> new HashMap<>());
        BankCardPermission previous = permissions.put(cardUuid, permission);
        return previous != permission;
    }

    /** 按银行卡 UUID 条件清理权限，并移除清空后的玩家权限表。 */
    private boolean removePermissionsIf(Predicate<UUID> shouldRemove) {
        boolean removed = false;
        Iterator<Map.Entry<UUID, Map<UUID, BankCardPermission>>> iterator = cardPermissions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<UUID, BankCardPermission> permissions = iterator.next().getValue();
            removed |= permissions.keySet().removeIf(shouldRemove);
            if (permissions.isEmpty()) {
                iterator.remove();
            }
        }
        return removed;
    }

    /** 宽松读取银行卡列表，跳过无法反序列化的卡。 */
    private static <T> DataResult<Pair<List<BankCard>, T>> decodeCards(DynamicOps<T> ops, T input) {
        List<BankCard> result = new ArrayList<>();
        ops.getList(input)
                .resultOrPartial(message -> MapleBankTrade.LOGGER.error("无法读取银行卡列表，跳过全部银行卡: {}", message))
                .ifPresent(list -> list.accept(value -> BankCard.CODEC.decode(ops, value)
                        .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过无法反序列化的银行卡: {}", message))
                        .ifPresent(pair -> result.add(pair.getFirst()))));
        return DataResult.success(Pair.of(List.copyOf(result), ops.empty()));
    }

    /** 宽松读取权限表，跳过无法反序列化的玩家、卡或权限值。 */
    private static <T> DataResult<Pair<Map<UUID, Map<UUID, BankCardPermission>>, T>> decodeCardPermissions(DynamicOps<T> ops, T input) {
        Map<UUID, Map<UUID, BankCardPermission>> result = new HashMap<>();
        ops.getMapValues(input)
                .resultOrPartial(message -> MapleBankTrade.LOGGER.error("无法读取银行卡权限表，跳过全部权限: {}", message))
                .ifPresent(players -> players.forEach(playerEntry -> {
                    UUID playerUuid = UUID_STRING_CODEC.parse(ops, playerEntry.getFirst())
                            .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过无法反序列化的银行卡权限玩家 ID: {}", message))
                            .orElse(null);
                    if (playerUuid == null) return;

                    Map<UUID, BankCardPermission> permissions = new HashMap<>();
                    ops.getMapValues(playerEntry.getSecond())
                            .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过玩家 {} 的银行卡权限表: {}", playerUuid, message))
                            .ifPresent(cards -> cards.forEach(cardEntry -> {
                                UUID cardUuid = UUID_STRING_CODEC.parse(ops, cardEntry.getFirst())
                                        .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过玩家 {} 的银行卡权限记录，卡 ID 无法反序列化: {}", playerUuid, message))
                                        .orElse(null);
                                BankCardPermission permission = CARD_PERMISSION_CODEC.parse(ops, cardEntry.getSecond())
                                        .resultOrPartial(message -> MapleBankTrade.LOGGER.error("跳过玩家 {} 的银行卡权限记录，权限值无法反序列化: {}", playerUuid, message))
                                        .orElse(null);
                                if (cardUuid != null && permission != null) {
                                    permissions.put(cardUuid, permission);
                                }
                            }));
                    if (!permissions.isEmpty()) {
                        result.put(playerUuid, permissions);
                    }
                }));
        return DataResult.success(Pair.of(result, ops.empty()));
    }
}

package com.maple.maple_banktrade.api.bank.ui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.maple.maple_banktrade.MapleBankTrade;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 仿照 LDLib2 PlayerUIMenuType，支持打开 UI 时携带参数的玩家 UI 注册表。
 */
public final class ParameterizedPlayerUIMenuType {

    // ==============================================
    // 注册表
    // ==============================================

    /** UI 标识到编解码与 Holder 工厂的映射。 */
    private static final Map<Identifier, Entry<?>> UI_HOLDERS = new ConcurrentHashMap<>();

    /** 菜单类型延迟注册表。 */
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, MapleBankTrade.MODID);
    /** 参数化玩家 UI 菜单类型。 */
    public static final Supplier<MenuType<ModularUIContainerMenu>> PLAYER_PARAMETERIZED_UI = MENUS.register("player_parameterized_ui",
            () -> IMenuTypeExtension.create(ParameterizedPlayerUIMenuType::create));

    // ==============================================
    // 初始化 / 注册
    // ==============================================

    /** 将菜单类型注册到 mod 事件总线。 */
    public static void init(IEventBus modBus) {
        MENUS.register(modBus);
    }

    /** 注册带参数的玩家 UI。 */
    public static <T> void register(Identifier id, StreamCodec<RegistryFriendlyByteBuf, T> codec, BiFunction<Player, T, PlayerUIHolder> holderFactory) {
        UI_HOLDERS.put(id, new Entry<>(codec, holderFactory));
    }

    // ==============================================
    // 打开 / 创建
    // ==============================================

    /** 在服务端为玩家打开指定参数化 UI。 */
    public static <T> boolean openUI(ServerPlayer player, Identifier id, T payload) {
        Entry<T> entry = getEntry(id);
        if (entry == null) return false;
        PlayerUIHolder holder = entry.createHolder(player, payload);
        if (holder == null) return false;
        player.openMenu(new MenuProvider() {

            @Override
            public @NonNull Component getDisplayName() {
                return Component.translatable(id.toLanguageKey());
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, @NonNull Inventory playerInventory, @NonNull Player player) {
                return new ParameterizedPlayerUIContainerMenu(containerId, playerInventory, holder);
            }

            @Override
            public void writeClientSideData(@NonNull AbstractContainerMenu menu, @NonNull RegistryFriendlyByteBuf buffer) {
                buffer.writeIdentifier(id);
                entry.writePayload(buffer, payload);
            }
        });
        return true;
    }

    /** 客户端从网络缓冲创建参数化菜单。 */
    public static ModularUIContainerMenu create(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        Identifier id = data.readIdentifier();
        Entry<?> entry = UI_HOLDERS.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("No parameterized player ui holder found for id " + id);
        }
        PlayerUIHolder holder = entry.readHolder(inventory.player, data);
        if (holder == null) {
            throw new IllegalArgumentException("No parameterized player ui holder created for id " + id);
        }
        return new ParameterizedPlayerUIContainerMenu(windowId, inventory, holder);
    }

    /** 按标识获取注册条目。 */
    @SuppressWarnings("unchecked")
    private static <T> Entry<T> getEntry(Identifier id) {
        return (Entry<T>) UI_HOLDERS.get(id);
    }

    // ==============================================
    // 内部类型
    // ==============================================

    /** 单条 UI 注册：编解码器与 Holder 工厂。 */
    private record Entry<T>(StreamCodec<RegistryFriendlyByteBuf, T> codec,
                            BiFunction<Player, T, PlayerUIHolder> holderFactory) {

        /** 根据载荷创建 Holder。 */
        private PlayerUIHolder createHolder(Player player, T payload) {
            return holderFactory.apply(player, payload);
        }

        /** 将载荷写入网络缓冲。 */
        private void writePayload(RegistryFriendlyByteBuf buffer, T payload) {
            codec.encode(buffer, payload);
        }

        /** 从网络缓冲解码载荷并创建 Holder。 */
        private PlayerUIHolder readHolder(Player player, RegistryFriendlyByteBuf buffer) {
            return holderFactory.apply(player, codec.decode(buffer));
        }
    }

    /** 参数化玩家 UI 的容器持有者。 */
    @FunctionalInterface
    public interface PlayerUIHolder extends IContainerUIHolder {

        @Override
        @NonNull
        ModularUI createUI(@NonNull Player player);

        @Override
        default boolean isStillValid(@NonNull Player player) {
            return true;
        }

        /** 菜单关闭时回调。 */
        default void onClosed(Player player) {}
    }

    /** 关闭时转发给 PlayerUIHolder.onClosed 的容器菜单。 */
    private static final class ParameterizedPlayerUIContainerMenu extends ModularUIContainerMenu {

        private ParameterizedPlayerUIContainerMenu(int windowId, Inventory inventory, PlayerUIHolder holder) {
            super(PLAYER_PARAMETERIZED_UI.get(), windowId, inventory, holder);
        }

        @Override
        public void removed(@NonNull Player player) {
            super.removed(player);
            if (uiHolder instanceof PlayerUIHolder holder) {
                holder.onClosed(player);
            }
        }
    }

    /** 禁止实例化。 */
    private ParameterizedPlayerUIMenuType() {}
}

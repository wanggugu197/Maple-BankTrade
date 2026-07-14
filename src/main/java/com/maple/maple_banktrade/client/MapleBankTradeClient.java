package com.maple.maple_banktrade.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.bank.ui.ParameterizedPlayerUIMenuType;

/**
 * 客户端入口：配置屏与菜单 Screen 注册。
 */
@Mod(value = MapleBankTrade.MODID, dist = Dist.CLIENT)
public class MapleBankTradeClient {

    /** 注册客户端扩展点与事件监听。 */
    public MapleBankTradeClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerMenuScreens);
    }

    /** 注册参数化玩家 UI 的 Screen。 */
    private void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ParameterizedPlayerUIMenuType.PLAYER_PARAMETERIZED_UI.get(), ModularUIContainerScreen::new);
    }
}

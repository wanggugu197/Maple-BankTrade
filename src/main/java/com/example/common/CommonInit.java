package com.example.common;

import com.example.data.lang.ExampleLangHandler;

import net.neoforged.bus.api.IEventBus;

/**
 * 通用初始化类
 */
public class CommonInit {

    private static IEventBus modBus;

    public static void init(IEventBus modBus) {
        CommonInit.modBus = modBus;

        ExampleDataComponent.init();

        ExampleBlockTags.init();
        ExampleItemTags.init();

        ExampleTab.init();
        ExampleRegistration.init();
        ExampleRecipe.init();
        ExampleLangHandler.init();

        NeoForgeCommonEvent.init();
    }
}

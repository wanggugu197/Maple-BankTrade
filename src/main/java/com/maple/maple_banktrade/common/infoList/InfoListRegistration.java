package com.maple.maple_banktrade.common.infoList;

import net.minecraft.network.chat.Component;

import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.maple.maple_banktrade.api.bank.data.InfoList;
import com.maple.maple_banktrade.common.bank.CardRegistration;

import java.util.List;

import static com.maple.maple_banktrade.data.lang.MBTLangHandler.addLang;

public class InfoListRegistration {

    public static void init() {
        registerInfoLists();
        registerInfoListTranslations();
    }

    public static final InfoList combatList = InfoList.register(
            CardRegistration.CENTRAL_TAGGED_CARD.nameIndex(),
            List.of(Component.literal("一些描述信息"), Component.literal("又是一些描述信息")),
            Sprites.RECT_RD_LIGHT);
    public static final InfoList miningList = InfoList.register(
            CardRegistration.MAGIC_TAGGED_CARD.nameIndex(),
            List.of(),
            Sprites.RECT_RD_LIGHT);

    private static void registerInfoLists() {
        // 注册战斗信息列表

        // 添加战斗任务（20个）
        for (int i = 1; i <= 19; i++) {
            short tier = (short) (i <= 10 ? 1 : i <= 15 ? 2 : 3);
            int required = 10 + i * 5; // 例如 15, 20, 25 ... 110
            // 使用翻译键：条目显示名称由语言文件提供
            Component display = Component.translatable("info_list.combat.task." + i);
            combatList.addEntry(new InfoList.InfoEntry("combat_task_" + i, display, required, tier));
        }
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 20,
                Component.translatable("info_list.combat.task." + 20),
                1, (short) 4));
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 21,
                Component.translatable("info_list.combat.task." + 20),
                1, (short) 5));
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 22,
                Component.translatable("info_list.combat.task." + 20),
                1, (short) 6));
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 23,
                Component.translatable("info_list.combat.task." + 20),
                1, (short) 7));
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 24,
                Component.translatable("info_list.combat.task." + 20),
                1, (short) 8));
        combatList.addEntry(new InfoList.InfoEntry("combat_task_" + 25,
                Component.literal("info_list.combat.task.info_list.combat.task.info_list.combat.task.info_list.combat.task.info_list.combat.task."),
                1, (short) 0));

        // 添加挖掘任务（20个）
        for (int i = 1; i <= 20; i++) {
            short tier = (short) (i <= 10 ? 1 : i <= 15 ? 2 : 3);
            int required = 20 + i * 10; // 例如 30, 40, 50 ... 220
            Component display = Component.translatable("info_list.mining.task." + i);
            miningList.addEntry(new InfoList.InfoEntry("mining_task_" + i, display, required, tier));
        }
    }

    private static void registerInfoListTranslations() {
        // 战斗任务
        String[] combatNames = {
                "击杀僵尸", "击杀骷髅", "击杀苦力怕", "击杀蜘蛛", "击杀末影人",
                "击杀女巫", "击杀凋零骷髅", "击杀烈焰人", "击杀岩浆怪", "击杀恶魂",
                "击杀守卫者", "击杀远古守卫者", "击杀潜影贝", "击杀劫掠兽", "击杀唤魔者",
                "击杀恼鬼", "击杀掠夺者", "击杀猪灵", "击杀疣猪兽", "击杀末影龙"
        };
        for (int i = 0; i < combatNames.length; i++) {
            addLang("info_list.combat.task." + (i + 1), combatNames[i], "Kill " + combatNames[i].replace("击杀", ""));
        }

        // 挖掘任务
        String[] miningNames = {
                "挖掘石头", "挖掘花岗岩", "挖掘闪长岩", "挖掘安山岩", "挖掘煤炭",
                "挖掘铁矿石", "挖掘金矿石", "挖掘青金石", "挖掘红石", "挖掘钻石",
                "挖掘绿宝石", "挖掘下界石英", "挖掘下界金矿石", "挖掘远古残骸", "挖掘深板岩",
                "挖掘紫晶", "挖掘铜矿石", "挖掘锡矿石", "挖掘银矿石", "挖掘黑曜石"
        };
        for (int i = 0; i < miningNames.length; i++) {
            addLang("info_list.mining.task." + (i + 1), miningNames[i], "Mine " + miningNames[i].replace("挖掘", ""));
        }
    }
}

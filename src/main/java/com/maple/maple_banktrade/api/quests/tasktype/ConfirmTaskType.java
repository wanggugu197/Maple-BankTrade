package com.maple.maple_banktrade.api.quests.tasktype;

import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;
import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

/**
 * 确认完成类型 —— 无额外检查，直接完成。
 *
 * <p>
 * 这是最基础的完成类型，等同于点击即完成。使用时无需配置参数。
 */
public class ConfirmTaskType implements ITaskType {

    private static final Identifier ID = MapleBankTrade.id("confirm");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public boolean canComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        return true; // 确认完成：无额外条件
    }

    @Override
    public void onComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        // 确认完成：无副作用
    }

    @Override
    public String toString() {
        return "ConfirmTaskType";
    }
}

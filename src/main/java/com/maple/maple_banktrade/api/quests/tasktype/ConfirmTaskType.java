package com.maple.maple_banktrade.api.quests.tasktype;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

/**
 * 确认完成类型 —— 无额外检查，直接完成。
 *
 * <p>
 * 这是最基础的完成类型，等同于点击即完成。使用时无需配置参数。
 *
 * <p>
 * v3.6 重构：移除 {@code getId()}，使用单例模式直接引用。
 */
public class ConfirmTaskType implements ITaskType {

    /** 单例实例。 */
    public static final ConfirmTaskType INSTANCE = new ConfirmTaskType();

    @Override
    public boolean canComplete(ITaskDefinition def, IQuestRepository repo, Object context) {
        return true;
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

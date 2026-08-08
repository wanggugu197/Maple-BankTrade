package com.maple.maple_banktrade.api.quests.condition;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import com.maple.maple_banktrade.MapleBankTrade;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合条件：支持 AND、OR、NOT 三种逻辑组合。
 *
 * <p>
 * 包含三个内部静态类，通过 {@link QuestConditionRegistry} 注册：
 * <ul>
 * <li>{@code maple_banktrade:and} —— 所有子条件都满足</li>
 * <li>{@code maple_banktrade:or} —— 任一子条件满足</li>
 * <li>{@code maple_banktrade:not} —— 反转子条件结果</li>
 * </ul>
 *
 * <p>
 * AND/OR 参数格式：
 * 
 * <pre>{@code
 * {
 *   "conditions": [
 *     { "id": "level_ge", "params": { "level": 5 } },
 *     { "id": "has_item", "params": { "item": "minecraft:diamond" } }
 *   ]
 * }
 * }</pre>
 *
 * <p>
 * NOT 参数格式：
 * 
 * <pre>{@code
 * {
 *   "condition": { "id": "is_raining", "params": {} }
 * }
 * }</pre>
 */
public abstract class CompositeCondition extends BaseQuestCondition {

    // ==============================================
    // 子条件条目
    // ==============================================

    /**
     * 子条件条目：ID + 参数。
     */
    protected static class SubCondition {

        final Identifier id;
        final CompoundTag params;

        SubCondition(Identifier id, CompoundTag params) {
            this.id = id;
            this.params = params != null ? params : new CompoundTag();
        }

        boolean evaluate(Object context) {
            BaseQuestCondition condition = QuestConditionRegistry.getCondition(id, params);
            return condition.evaluate(context);
        }

        @Override
        public String toString() {
            return "SubCondition{id=" + id + "}";
        }
    }

    // ==============================================
    // AND 条件
    // ==============================================

    /**
     * 所有子条件都满足时才通过。
     */
    public static class AndCondition extends CompositeCondition {

        private final List<SubCondition> conditions;

        public AndCondition(List<SubCondition> conditions) {
            this.conditions = conditions != null ? conditions : List.of();
        }

        /**
         * 从 CompoundTag 构造：读取 "conditions" 列表。
         */
        public static AndCondition fromTag(CompoundTag tag) {
            List<SubCondition> list = new ArrayList<>();
            ListTag conditionsTag = tag.getListOrEmpty("conditions");
            for (Tag element : conditionsTag) {
                if (element instanceof CompoundTag ct) {
                    Identifier id = parseId(ct);
                    CompoundTag params = ct.getCompoundOrEmpty("params");
                    if (id != null) {
                        list.add(new SubCondition(id, params));
                    }
                }
            }
            return new AndCondition(list);
        }

        @Override
        public boolean evaluate(Object context) {
            if (conditions.isEmpty()) return true;
            for (SubCondition cond : conditions) {
                if (!cond.evaluate(context)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "AndCondition{conditions=" + conditions + "}";
        }
    }

    // ==============================================
    // OR 条件
    // ==============================================

    /**
     * 任一子条件满足即通过。
     */
    public static class OrCondition extends CompositeCondition {

        private final List<SubCondition> conditions;

        public OrCondition(List<SubCondition> conditions) {
            this.conditions = conditions != null ? conditions : List.of();
        }

        /**
         * 从 CompoundTag 构造：读取 "conditions" 列表。
         */
        public static OrCondition fromTag(CompoundTag tag) {
            List<SubCondition> list = new ArrayList<>();
            ListTag conditionsTag = tag.getListOrEmpty("conditions");
            for (Tag element : conditionsTag) {
                if (element instanceof CompoundTag ct) {
                    Identifier id = parseId(ct);
                    CompoundTag params = ct.getCompoundOrEmpty("params");
                    if (id != null) {
                        list.add(new SubCondition(id, params));
                    }
                }
            }
            return new OrCondition(list);
        }

        @Override
        public boolean evaluate(Object context) {
            if (conditions.isEmpty()) return false;
            for (SubCondition cond : conditions) {
                if (cond.evaluate(context)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "OrCondition{conditions=" + conditions + "}";
        }
    }

    // ==============================================
    // NOT 条件
    // ==============================================

    /**
     * 反转子条件结果。
     */
    public static class NotCondition extends CompositeCondition {

        private final SubCondition condition;

        public NotCondition(SubCondition condition) {
            this.condition = condition;
        }

        /**
         * 从 CompoundTag 构造：读取 "condition" 对象。
         */
        public static NotCondition fromTag(CompoundTag tag) {
            CompoundTag innerTag = tag.getCompoundOrEmpty("condition");
            Identifier id = parseId(innerTag);
            CompoundTag params = innerTag.getCompoundOrEmpty("params");
            return new NotCondition(id != null ? new SubCondition(id, params) : null);
        }

        @Override
        public boolean evaluate(Object context) {
            if (condition == null) return true;
            return !condition.evaluate(context);
        }

        @Override
        public String toString() {
            return "NotCondition{condition=" + condition + "}";
        }
    }

    // ==============================================
    // 辅助
    // ==============================================

    private static Identifier parseId(CompoundTag tag) {
        String idStr = tag.getStringOr("id", "");
        if (idStr.isEmpty()) return null;
        try {
            return Identifier.parse(idStr);
        } catch (Exception e) {
            MapleBankTrade.LOGGER.warn("Invalid condition id in CompositeCondition: {}", idStr, e);
            return null;
        }
    }
}

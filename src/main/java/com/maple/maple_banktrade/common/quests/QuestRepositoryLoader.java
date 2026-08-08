package com.maple.maple_banktrade.common.quests;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;
import com.maple.maple_banktrade.api.quests.repository.InMemoryQuestRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务仓储加载器。
 *
 * <p>
 * 负责将具体蓝图注册到仓储中，并执行拓扑验证（环检测、孤儿节点检测等）。
 */
public final class QuestRepositoryLoader {

    private QuestRepositoryLoader() {
        // 工具类，禁止实例化
    }

    /**
     * 初始化仓储并加载所有蓝图，加载完成后自动执行拓扑验证。
     */
    public static void loadAll(IQuestRepository repository) {
        List<ITaskDefinition> blueprints = QuestBlueprints.getAllBlueprints();

        // 加载到仓储
        if (repository instanceof InMemoryQuestRepository memRepo) {
            memRepo.loadDefinitions(blueprints);
        } else {
            for (ITaskDefinition def : blueprints) {
                // 其他实现逐个注册（预留扩展点）
            }
        }

        // 拓扑验证
        ValidationReport report = validate(blueprints);
        if (report.hasErrors()) {
            System.err.println("[Quest] Validation ERRORS (" + report.errors.size() + "):");
            report.errors.forEach(e -> System.err.println("  - " + e));
        }
        if (report.hasWarnings()) {
            System.out.println("[Quest] Validation WARNINGS (" + report.warnings.size() + "):");
            report.warnings.forEach(w -> System.out.println("  - " + w));
        }
        System.out.println("[Quest] Loaded " + blueprints.size() + " task blueprints" + (report.hasErrors() ? " (with errors)" : ""));
    }

    // ==============================================
    // 拓扑验证
    // ==============================================

    /**
     * 对蓝图列表执行拓扑验证。
     *
     * @return 验证报告，包含 errors 和 warnings
     */
    public static ValidationReport validate(List<ITaskDefinition> blueprints) {
        ValidationReport report = new ValidationReport();
        Set<String> allIds = blueprints.stream()
                .map(ITaskDefinition::getId)
                .collect(Collectors.toSet());

        for (ITaskDefinition def : blueprints) {
            String id = def.getId();

            // 自引用检测
            if (id.equals(def.getParentId())) {
                report.errors.add("Task '" + id + "' references itself as parent");
            }
            if (def.getDependentNodes().contains(id)) {
                report.errors.add("Task '" + id + "' depends on itself");
            }

            // 孤儿父节点检测
            String parentId = def.getParentId();
            if (parentId != null && !parentId.isEmpty() && !allIds.contains(parentId)) {
                report.errors.add("Task '" + id + "' references non-existent parent: '" + parentId + "'");
            }

            // 孤儿 prevSiblingId 检测
            String prevSiblingId = def.getPrevSiblingId();
            if (prevSiblingId != null && !prevSiblingId.isEmpty() && !allIds.contains(prevSiblingId)) {
                report.errors.add("Task '" + id + "' references non-existent prevSibling: '" + prevSiblingId + "'");
            }

            // 孤儿依赖检测
            for (String depId : def.getDependentNodes()) {
                if (!allIds.contains(depId)) {
                    report.errors.add("Task '" + id + "' depends on non-existent task: '" + depId + "'");
                }
            }

            // 孤儿 nextTaskInChain 检测
            String next = def.getNextTaskInChain();
            if (next != null && !next.isEmpty() && !allIds.contains(next)) {
                report.warnings.add("Task '" + id + "' chains to non-existent task: '" + next + "'");
            }

            // 孤儿 poolIds 检测
            for (String poolId : def.getPoolIds()) {
                if (!allIds.contains(poolId)) {
                    report.warnings.add("Task '" + id + "' pool references non-existent task: '" + poolId + "'");
                }
            }

            // 行为模式与配置一致性检查
            if (def.getBehavior().requiresPool() && def.getPoolIds().isEmpty()) {
                report.warnings.add("Task '" + id + "' uses " + def.getBehavior() + " but has empty poolIds");
            }
        }

        // 环检测
        detectCycles(blueprints, allIds, report);

        return report;
    }

    /**
     * 使用三色 DFS 检测依赖图中的环。
     */
    private static void detectCycles(List<ITaskDefinition> blueprints, Set<String> allIds, ValidationReport report) {
        // 构建依赖图：A 依赖 B 意味着 B 完成后 A 才能解锁
        Map<String, List<String>> adjacency = new HashMap<>();
        for (ITaskDefinition def : blueprints) {
            List<String> neighbors = new ArrayList<>();
            // dependentNodes：A 依赖 B → 边 A→B
            neighbors.addAll(def.getDependentNodes());
            // parentId：子节点依赖父节点完成
            String parentId = def.getParentId();
            if (parentId != null && !parentId.isEmpty()) {
                neighbors.add(parentId);
            }
            // prevSiblingId：A 排在前驱 B 之后 → A 依赖 B
            String prevSiblingId = def.getPrevSiblingId();
            if (prevSiblingId != null && !prevSiblingId.isEmpty()) {
                neighbors.add(prevSiblingId);
            }
            adjacency.put(def.getId(), neighbors);
        }

        // 三色标记法
        Set<String> white = new HashSet<>(allIds); // 未访问
        Set<String> gray = new HashSet<>();          // 访问中（递归栈中）
        Set<String> black = new HashSet<>();         // 已完成

        for (String nodeId : allIds) {
            if (white.contains(nodeId)) {
                List<String> path = new ArrayList<>();
                dfsCycleDetect(nodeId, adjacency, white, gray, black, path, report);
            }
        }
    }

    private static void dfsCycleDetect(String node, Map<String, List<String>> adjacency,
                                       Set<String> white, Set<String> gray, Set<String> black,
                                       List<String> path, ValidationReport report) {
        white.remove(node);
        gray.add(node);
        path.add(node);

        for (String neighbor : adjacency.getOrDefault(node, Collections.emptyList())) {
            if (gray.contains(neighbor)) {
                // 发现环
                int cycleStart = path.indexOf(neighbor);
                String cyclePath = String.join(" → ", path.subList(cycleStart, path.size())) + " → " + neighbor;
                report.errors.add("Cycle detected: " + cyclePath);
            } else if (white.contains(neighbor)) {
                dfsCycleDetect(neighbor, adjacency, white, gray, black, path, report);
            }
        }

        path.remove(path.size() - 1);
        gray.remove(node);
        black.add(node);
    }

    // ==============================================
    // 验证报告
    // ==============================================

    /**
     * 拓扑验证结果报告。
     */
    public static class ValidationReport {

        /** 严重问题（必须修复）。 */
        public final List<String> errors = new ArrayList<>();
        /** 警告（建议修复）。 */
        public final List<String> warnings = new ArrayList<>();

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            return "ValidationReport{errors=" + errors.size() + ", warnings=" + warnings.size() + "}";
        }
    }
}

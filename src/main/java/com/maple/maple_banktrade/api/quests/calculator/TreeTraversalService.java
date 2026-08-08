package com.maple.maple_banktrade.api.quests.calculator;

import com.maple.maple_banktrade.api.quests.core.IQuestRepository;
import com.maple.maple_banktrade.api.quests.core.ITaskDefinition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 树链拓扑服务：提供拓扑排序、路径查询、兄弟链排序等树/链混合结构的遍历能力。
 */
public final class TreeTraversalService {

    private TreeTraversalService() {
        // 工具类，禁止实例化
    }

    // ==============================================
    // 拓扑排序
    // ==============================================

    /**
     * 获取按依赖顺序排列的所有任务 ID（拓扑排序）。
     * 排序保证：父节点在前，兄弟链按 prevSiblingId 顺序，dependentNodes 依赖在前。
     *
     * @return 拓扑排序后的任务 ID 列表
     */
    public static List<String> topologicalSort(IQuestRepository repository) {
        List<ITaskDefinition> allDefs = repository.getAllDefinitions();
        Map<String, ITaskDefinition> defMap = allDefs.stream()
                .collect(Collectors.toMap(ITaskDefinition::getId, d -> d));

        // Kahn 算法
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (ITaskDefinition def : allDefs) {
            String id = def.getId();
            inDegree.putIfAbsent(id, 0);
            adjacency.putIfAbsent(id, new ArrayList<>());

            // 父节点依赖
            String parentId = def.getParentId();
            if (parentId != null && !parentId.isEmpty() && defMap.containsKey(parentId)) {
                adjacency.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
                inDegree.merge(id, 1, Integer::sum);
            }

            // 兄弟链依赖
            String prevSiblingId = def.getPrevSiblingId();
            if (prevSiblingId != null && !prevSiblingId.isEmpty() && defMap.containsKey(prevSiblingId)) {
                adjacency.computeIfAbsent(prevSiblingId, k -> new ArrayList<>()).add(id);
                inDegree.merge(id, 1, Integer::sum);
            }

            // 额外依赖
            for (String depId : def.getDependentNodes()) {
                if (defMap.containsKey(depId)) {
                    adjacency.computeIfAbsent(depId, k -> new ArrayList<>()).add(id);
                    inDegree.merge(id, 1, Integer::sum);
                }
            }
        }

        // 入度为 0 的节点入队
        Queue<String> queue = new ArrayDeque<>();
        for (String id : allDefs.stream().map(ITaskDefinition::getId).toList()) {
            if (inDegree.getOrDefault(id, 0) == 0) {
                queue.add(id);
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            for (String neighbor : adjacency.getOrDefault(current, Collections.emptyList())) {
                int newDegree = inDegree.merge(neighbor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    // ==============================================
    // 子树查询
    // ==============================================

    /**
     * 获取某节点的所有子孙节点（递归，包括所有后代）。
     */
    public static List<String> getDescendants(String taskId, IQuestRepository repository) {
        List<String> result = new ArrayList<>();
        collectDescendants(taskId, repository, result, new HashSet<>());
        return result;
    }

    private static void collectDescendants(String taskId, IQuestRepository repository,
                                           List<String> result, Set<String> visited) {
        if (!visited.add(taskId)) {
            return; // 防止环导致无限递归
        }
        List<ITaskDefinition> children = repository.getChildren(taskId);
        for (ITaskDefinition child : children) {
            result.add(child.getId());
            collectDescendants(child.getId(), repository, result, visited);
        }
    }

    /**
     * 获取某节点的所有祖先节点（从根到父节点）。
     */
    public static List<String> getAncestors(String taskId, IQuestRepository repository) {
        List<String> result = new ArrayList<>();
        String current = taskId;
        Set<String> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            ITaskDefinition def = repository.getDefinition(current).orElse(null);
            if (def == null) break;
            String parentId = def.getParentId();
            if (parentId != null && !parentId.isEmpty()) {
                result.add(parentId);
            }
            current = parentId;
        }
        Collections.reverse(result);
        return result;
    }

    // ==============================================
    // 兄弟链
    // ==============================================

    /**
     * 获取某节点的兄弟链顺序（按 prevSiblingId 排列）。
     * 返回同一父节点下按兄弟链顺序排列的任务 ID 列表。
     */
    public static List<String> getSiblingChain(String taskId, IQuestRepository repository) {
        ITaskDefinition def = repository.getDefinition(taskId).orElse(null);
        if (def == null) return Collections.emptyList();

        String parentId = def.getParentId();
        if (parentId == null || parentId.isEmpty()) return Collections.emptyList();

        List<ITaskDefinition> siblings = repository.getChildren(parentId);
        if (siblings.isEmpty()) return Collections.emptyList();

        // 构建 prevSiblingId 映射
        Map<String, String> nextMap = new HashMap<>();
        Set<String> hasPrev = new HashSet<>();
        for (ITaskDefinition sib : siblings) {
            String prevId = sib.getPrevSiblingId();
            if (prevId != null && !prevId.isEmpty()) {
                nextMap.put(prevId, sib.getId());
                hasPrev.add(sib.getId());
            }
        }

        // 找到链头（没有 prevSiblingId 的节点）
        List<String> heads = siblings.stream()
                .map(ITaskDefinition::getId)
                .filter(id -> !hasPrev.contains(id))
                .collect(Collectors.toList());

        // 从每个链头开始遍历
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String head : heads) {
            String current = head;
            while (current != null && visited.add(current)) {
                result.add(current);
                current = nextMap.get(current);
            }
        }

        // 追加不在链中的孤立兄弟
        for (ITaskDefinition sib : siblings) {
            if (!visited.contains(sib.getId())) {
                result.add(sib.getId());
            }
        }

        return result;
    }

    // ==============================================
    // 路径查询
    // ==============================================

    /**
     * 获取从根节点到目标节点的路径。
     */
    public static List<String> getPathToRoot(String taskId, IQuestRepository repository) {
        List<String> ancestors = getAncestors(taskId, repository);
        List<String> path = new ArrayList<>(ancestors);
        path.add(taskId);
        return path;
    }

    /**
     * 查找两个节点的最近公共祖先。
     */
    public static String findLowestCommonAncestor(String taskIdA, String taskIdB, IQuestRepository repository) {
        List<String> pathA = getPathToRoot(taskIdA, repository);
        List<String> pathB = getPathToRoot(taskIdB, repository);

        String lca = null;
        int minLen = Math.min(pathA.size(), pathB.size());
        for (int i = 0; i < minLen; i++) {
            if (pathA.get(i).equals(pathB.get(i))) {
                lca = pathA.get(i);
            } else {
                break;
            }
        }
        return lca;
    }
}

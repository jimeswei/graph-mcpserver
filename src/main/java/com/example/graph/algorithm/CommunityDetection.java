package com.example.graph.algorithm;

import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import com.example.graph.core.Edge; // 添加Edge导入
import java.util.*;

public class CommunityDetection {
    private final Graph graph;
    private Map<Node, Integer> communities; // 节点到社区的映射
    private Map<Integer, List<Node>> communitiesMap; // 社区到节点列表的映射
    private double modularity; // 最终模块度值
    private int nextCommunityId; // 新社区ID生成器

    // 边权重缓存（提升性能）
    private final Map<Node, Map<Node, Double>> edgeWeightCache = new HashMap<>();

    public CommunityDetection(Graph graph) {
        this.graph = graph;
        this.communities = new HashMap<>();
        this.nextCommunityId = 0;
        initializeCommunities();
        initializeEdgeCache(); // 初始化边权重缓存
    }

    // 初始化边权重缓存
    private void initializeEdgeCache() {
        for (Node node : graph.getNodes()) {
            Map<Node, Double> neighborWeights = new HashMap<>();
            for (Edge edge : graph.getEdgesFromNode(node)) {
                neighborWeights.put(edge.getDestination(), edge.getWeight());
            }
            edgeWeightCache.put(node, neighborWeights);
        }
    }

    // 初始化：每个节点独立社区
    private void initializeCommunities() {
        for (Node node : graph.getNodes()) {
            communities.put(node, nextCommunityId++);
        }
    }

    // 主检测方法（优化版）
    public void detectCommunities() {
        if (graph.getNodes().isEmpty()) {
            modularity = 0.0;
            return;
        }

        double totalWeight = graph.getTotalEdgeWeight();
        if (totalWeight <= 0) {
            modularity = 0.0;
            return;
        }

        Map<Node, Double> nodeWeights = precomputeNodeWeights();
        Map<Integer, Double> communityWeights = precomputeCommunityWeights(nodeWeights);
        Map<Node, Map<Integer, Double>> nodeCommunityWeights = new HashMap<>();

        boolean changed;
        double currentModularity = Double.NEGATIVE_INFINITY;
        int iteration = 0;
        final int MAX_ITERATIONS = 100;
        final double MIN_DELTA_Q = 1e-6;

        do {
            changed = false;
            iteration++;

            // 随机遍历节点，减少顺序偏差
            List<Node> nodes = new ArrayList<>(graph.getNodes());
            Collections.shuffle(nodes);

            int moves = 0;

            for (Node node : nodes) {
                int currentCommunity = communities.get(node);
                double nodeWeight = nodeWeights.get(node);

                double ki_in_current = getCachedNodeCommunityWeight(
                        node, currentCommunity, nodeCommunityWeights);
                double currentCommunityWeight = communityWeights.get(currentCommunity);

                // 临时移除节点
                communityWeights.put(currentCommunity, currentCommunityWeight - nodeWeight);

                // 优化：邻居社区集合去重
                Set<Integer> neighborCommunities = new HashSet<>();
                for (Node neighbor : graph.getNeighbors(node)) {
                    int neighborComm = communities.get(neighbor);
                    if (neighborComm != currentCommunity) {
                        neighborCommunities.add(neighborComm);
                    }
                }

                int bestCommunity = currentCommunity;
                double bestDeltaQ = 0.0;
                double stayDeltaQ = calculateDeltaQ(
                        ki_in_current,
                        currentCommunityWeight - nodeWeight,
                        nodeWeight,
                        totalWeight);

                // 只遍历不同的邻居社区
                for (int neighborComm : neighborCommunities) {
                    double ki_in_neighbor = getCachedNodeCommunityWeight(node, neighborComm, nodeCommunityWeights);
                    double deltaQ = calculateDeltaQ(
                            ki_in_neighbor,
                            communityWeights.getOrDefault(neighborComm, 0.0),
                            nodeWeight,
                            totalWeight);
                    if (deltaQ > bestDeltaQ) {
                        bestDeltaQ = deltaQ;
                        bestCommunity = neighborComm;
                    }
                }

                // 考虑新社区
                double isolationDeltaQ = -stayDeltaQ - (nodeWeight * nodeWeight) / (2 * totalWeight * totalWeight);
                if (isolationDeltaQ > bestDeltaQ) {
                    bestDeltaQ = isolationDeltaQ;
                    bestCommunity = nextCommunityId++;
                }

                // 仅当有正增益时才移动
                if (bestDeltaQ > 0 && bestCommunity != currentCommunity) {
                    communityWeights.merge(bestCommunity, nodeWeight, Double::sum);
                    communities.put(node, bestCommunity);
                    changed = true;
                    moves++;
                    updateNodeCommunityCache(node, currentCommunity, bestCommunity, nodeCommunityWeights);
                } else {
                    // 节点未移动，恢复社区权重
                    communityWeights.put(currentCommunity, currentCommunityWeight);
                }
            }

            // 增量更新模块度（只在节点移动时）
            double newModularity = calculateModularity(totalWeight, nodeWeights);

            // 自适应终止条件
            if (newModularity - currentModularity < MIN_DELTA_Q || moves == 0 || iteration >= MAX_ITERATIONS) {
                changed = false;
            } else {
                currentModularity = newModularity;
            }
        } while (changed);

        modularity = currentModularity;
        buildCommunitiesMap();
    }

    // 预计算节点权重（节点度数）
    private Map<Node, Double> precomputeNodeWeights() {
        Map<Node, Double> nodeWeights = new HashMap<>();
        for (Node node : graph.getNodes()) {
            nodeWeights.put(node, getNodeWeight(node));
        }
        return nodeWeights;
    }

    // 预计算社区总权重
    private Map<Integer, Double> precomputeCommunityWeights(Map<Node, Double> nodeWeights) {
        Map<Integer, Double> communityWeights = new HashMap<>();
        for (Map.Entry<Node, Integer> entry : communities.entrySet()) {
            Node node = entry.getKey();
            int commId = entry.getValue();
            communityWeights.merge(commId, nodeWeights.get(node), Double::sum);
        }
        return communityWeights;
    }

    // 获取缓存的节点-社区连接权重
    private double getCachedNodeCommunityWeight(Node node, int communityId,
            Map<Node, Map<Integer, Double>> cache) {
        // 优先从缓存获取
        if (cache.containsKey(node) && cache.get(node).containsKey(communityId)) {
            return cache.get(node).get(communityId);
        }

        // 缓存未命中则计算并缓存
        double weight = calculateNodeCommunityWeight(node, communityId);
        cache.computeIfAbsent(node, k -> new HashMap<>())
                .put(communityId, weight);
        return weight;
    }

    // 更新节点社区缓存（移动节点后）
    private void updateNodeCommunityCache(Node node, int oldComm, int newComm,
            Map<Node, Map<Integer, Double>> cache) {
        // 清除旧社区缓存
        if (cache.containsKey(node)) {
            cache.get(node).remove(oldComm);

            // 更新邻居节点的缓存
            for (Node neighbor : graph.getNeighbors(node)) {
                if (cache.containsKey(neighbor)) {
                    // 从旧社区移除
                    cache.get(neighbor).computeIfPresent(oldComm, (k, v) -> v - getEdgeWeight(node, neighbor));
                    // 添加到新社区
                    cache.get(neighbor).merge(newComm, getEdgeWeight(node, neighbor), Double::sum);
                }
            }
        }
    }

    /**
     * 计算模块度变化ΔQ（核心公式）
     * ΔQ = [ki_in / m] - [Σ_tot * ki / (2 * m * m)]
     * 
     * @param ki_in       节点在目标社区的连接权重
     * @param sigma_tot   目标社区总权重
     * @param nodeWeight  节点权重
     * @param totalWeight 图的总权重
     * @return 模块度变化值
     */
    private double calculateDeltaQ(double ki_in, double sigma_tot,
            double nodeWeight, double totalWeight) {
        return (ki_in / totalWeight) - (sigma_tot * nodeWeight) / (2 * totalWeight * totalWeight);
    }

    /**
     * 计算节点在指定社区内的连接权重
     */
    public double calculateNodeCommunityWeight(Node node, int communityId) {
        double weight = 0.0;
        for (Node neighbor : graph.getNeighbors(node)) {
            if (communities.get(neighbor) == communityId) {
                weight += getEdgeWeight(node, neighbor);
            }
        }
        return weight;
    }

    /**
     * 计算节点权重（度）
     */
    public double getNodeWeight(Node node) {
        return graph.getEdgesFromNode(node).stream()
                .mapToDouble(edge -> edge.getWeight())
                .sum();
    }

    /**
     * 获取边权重（使用缓存提升性能）
     */
    public double getEdgeWeight(Node source, Node destination) {
        // 从缓存获取权重
        Map<Node, Double> weights = edgeWeightCache.get(source);
        if (weights != null) {
            Double weight = weights.get(destination);
            if (weight != null) {
                return weight;
            }
        }
        return 0.0; // 没有边存在
    }

    /**
     * 构建社区映射（延迟构建）
     */
    private void buildCommunitiesMap() {
        communitiesMap = new HashMap<>();
        for (Map.Entry<Node, Integer> entry : communities.entrySet()) {
            int commId = entry.getValue();
            Node node = entry.getKey();
            communitiesMap.computeIfAbsent(commId, k -> new ArrayList<>()).add(node);
        }
    }

    /**
     * 高效模块度计算（使用预存社区信息）
     */
    public double calculateModularity(double totalWeight, Map<Node, Double> nodeWeights) {
        if (totalWeight <= 0)
            return 0.0;

        double q = 0.0;
        double totalWeightSq = 2 * totalWeight * totalWeight;

        // 遍历所有边（避免节点对重复计算）
        for (Node node : graph.getNodes()) {
            for (Node neighbor : graph.getNeighbors(node)) {
                // 仅处理同一社区的边（避免重复计数）
                if (node.getId().compareTo(neighbor.getId()) < 0 &&
                        communities.get(node).equals(communities.get(neighbor))) {

                    double weight = getEdgeWeight(node, neighbor);
                    q += 2 * (weight - (nodeWeights.get(node) * nodeWeights.get(neighbor)) / totalWeightSq);
                }
            }
        }
        return q / (2 * totalWeight);
    }

    // ========== 结果获取方法 ==========
    public Map<Integer, List<Node>> getCommunities() {
        if (communitiesMap == null)
            buildCommunitiesMap();
        // return Collections.ununmodifiableMap(communitiesMap);
        return Collections.unmodifiableMap(communitiesMap);
    }

    public double getModularity() {
        return modularity;
    }

    public Set<Set<Node>> getCommunitiesSet() {
        Map<Integer, Set<Node>> commMap = new HashMap<>();
        for (Map.Entry<Node, Integer> entry : communities.entrySet()) {
            commMap.computeIfAbsent(entry.getValue(), k -> new HashSet<>())
                    .add(entry.getKey());
        }
        return new HashSet<>(commMap.values());
    }
}
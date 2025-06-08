package com.example.graph.algorithm;

import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import java.util.*;

public class JaccardSimilarity {
    private final Graph graph;
    // 缓存每个节点的邻居集合，提升效率
    private final Map<String, Set<String>> neighborCache = new HashMap<>();

    public JaccardSimilarity(Graph graph) {
        this.graph = graph;
        cacheNeighbors();
    }

    // 预先缓存所有节点的邻居ID集合
    private void cacheNeighbors() {
        for (Node node : graph.getNodes()) {
            Set<String> neighbors = new HashSet<>();
            for (Node n : graph.getNeighbors(node)) {
                neighbors.add(n.getId());
            }
            neighborCache.put(node.getId(), neighbors);
        }
    }

    /**
     * 计算两个节点的Jaccard相似度（通过节点ID）
     */
    public double compute(String idA, String idB) {
        Set<String> neighborsA = neighborCache.getOrDefault(idA, Collections.emptySet());
        Set<String> neighborsB = neighborCache.getOrDefault(idB, Collections.emptySet());
        if (neighborsA.isEmpty() && neighborsB.isEmpty())
            return 1.0;
        Set<String> intersection = new HashSet<>(neighborsA);
        intersection.retainAll(neighborsB);
        Set<String> union = new HashSet<>(neighborsA);
        union.addAll(neighborsB);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 计算两个节点的Jaccard相似度（通过Node对象）
     */
    public double compute(Node nodeA, Node nodeB) {
        return compute(nodeA.getId(), nodeB.getId());
    }

    /**
     * 计算全图所有节点对的Jaccard相似度矩阵
     * 返回Map<节点对字符串, 相似度>
     * 
     * @param onlyNonZero 是否只输出非零相似度
     */
    public Map<String, Double> computeAll(boolean onlyNonZero) {
        List<Node> nodes = new ArrayList<>(graph.getNodes());
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                String idA = nodes.get(i).getId();
                String idB = nodes.get(j).getId();
                double sim = compute(idA, idB);
                if (!onlyNonZero || sim > 0.0) {
                    String key = idA + "," + idB;
                    result.put(key, sim);
                }
            }
        }
        return result;
    }

    /**
     * 默认输出所有节点对的Jaccard相似度
     */
    public Map<String, Double> computeAll() {
        return computeAll(false);
    }
}

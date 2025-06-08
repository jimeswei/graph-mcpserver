package com.example.graph.algorithm;

import com.example.graph.core.Edge;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;

import java.util.*;
import java.util.stream.Collectors;

public class PageRank {
    private final Graph graph;
    private final double dampingFactor;
    private final double tolerance;
    private final int maxIterations;
    private Map<Node, Double> pageRanks;
    // 缓存每个节点的出度和入度节点，提升效率
    private final Map<Node, Integer> outDegreeCache = new HashMap<>();
    private final Map<Node, List<Node>> inNodesCache = new HashMap<>();

    public PageRank(Graph graph) {
        this(graph, 0.85, 1e-6, 100);
    }

    public PageRank(Graph graph, double dampingFactor, double tolerance, int maxIterations) {
        this.graph = graph;
        this.dampingFactor = dampingFactor;
        this.tolerance = tolerance;
        this.maxIterations = maxIterations;
        cacheDegrees();
    }

    // 预先缓存出度和入度节点
    private void cacheDegrees() {
        for (Node node : graph.getNodes()) {
            outDegreeCache.put(node, graph.getDegree(node));
        }
        for (Node node : graph.getNodes()) {
            inNodesCache.put(node, getIncomingNodes(node));
        }
    }

    public void compute() {
        initializePageRanks();
        int iteration = 0;
        double totalChange;
        int nodeCount = graph.getNodes().size();
        if (nodeCount == 0)
            return;

        do {
            totalChange = 0.0;
            Map<Node, Double> newPageRanks = new HashMap<>();

            // 预先计算所有悬挂节点（无出边节点）的PageRank总和
            double danglingSum = 0.0;
            for (Node node : graph.getNodes()) {
                if (outDegreeCache.get(node) == 0) {
                    danglingSum += pageRanks.get(node);
                }
            }

            // 计算每个节点的新PageRank
            for (Node node : graph.getNodes()) {
                double incomingPR = 0.0;
                for (Node incoming : inNodesCache.get(node)) {
                    int outgoingCount = outDegreeCache.get(incoming);
                    if (outgoingCount > 0) {
                        incomingPR += pageRanks.get(incoming) / outgoingCount;
                    }
                }
                double newRank = (1.0 - dampingFactor) / nodeCount;
                newRank += dampingFactor * (incomingPR + danglingSum / nodeCount);
                totalChange += Math.abs(newRank - pageRanks.get(node));
                newPageRanks.put(node, newRank);
            }

            // 归一化，提升数值稳定性
            double sum = newPageRanks.values().stream().mapToDouble(Double::doubleValue).sum();
            if (sum > 0) {
                newPageRanks.replaceAll((node, rank) -> rank / sum);
            }

            pageRanks = newPageRanks;
            iteration++;
        } while (totalChange > tolerance && iteration < maxIterations);
    }

    // 获取节点的所有入度节点
    private List<Node> getIncomingNodes(Node node) {
        return graph.getEdges().stream()
                .filter(edge -> edge.getDestination().equals(node))
                .map(Edge::getSource)
                .distinct()
                .collect(Collectors.toList());
    }

    private void initializePageRanks() {
        pageRanks = new HashMap<>();
        int nodeCount = graph.getNodes().size();
        if (nodeCount == 0)
            return;
        double initialRank = 1.0 / nodeCount;
        for (Node node : graph.getNodes()) {
            pageRanks.put(node, initialRank);
        }
    }

    public Map<Node, Double> getPageRanks() {
        return Collections.unmodifiableMap(pageRanks);
    }

    // 新增：支持通过节点ID获取分数
    public Double getPageRankById(String nodeId) {
        for (Node node : pageRanks.keySet()) {
            if (node.getId().equals(nodeId)) {
                return pageRanks.get(node);
            }
        }
        return null;
    }
}
package com.example.graph.core;

import java.util.*;
import java.util.stream.Collectors;

public class Graph {
    private final Map<Node, List<Edge>> adjacencyList = new HashMap<>();
    private final boolean directed;
    private final boolean weighted;


    public Graph() {
        this(false, false);
    }

    public Graph(boolean directed, boolean weighted) {
        this.directed = directed;
        this.weighted = weighted;
    }

    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, new ArrayList<>());
    }

    public void addEdge(Node source, Node destination) {
        addEdge(source, destination, 1.0);
    }

    public void addEdge(Node source, Node destination, double weight) {
        if (!adjacencyList.containsKey(source)) addNode(source);
        if (!adjacencyList.containsKey(destination)) addNode(destination);

        double finalWeight = weighted ? weight : 1.0;
        adjacencyList.get(source).add(new Edge(source, destination, finalWeight));

        if (!directed) {
            adjacencyList.get(destination).add(new Edge(destination, source, finalWeight));
        }
    }

    public Collection<Node> getNodes() {
        return new ArrayList<>(adjacencyList.keySet());
    }



    public Collection<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        for (List<Edge> edgeList : adjacencyList.values()) {
            edges.addAll(edgeList);
        }
        return edges;
    }

    public Collection<Edge> getEdgesFromNode(Node node) {
        return adjacencyList.getOrDefault(node, Collections.emptyList());
    }

    public Collection<Node> getNeighbors(Node node) {
        return getEdgesFromNode(node).stream()
                .map(Edge::getDestination)
                .collect(Collectors.toList());
    }

    public int getDegree(Node node) {
        return getEdgesFromNode(node).size();
    }

    public boolean containsNode(Node node) {
        return adjacencyList.containsKey(node);
    }

    public boolean isDirected() {
        return directed;
    }

    public boolean isWeighted() {
        return weighted;
    }

    public Graph createSubgraph(Set<Node> nodes) {
        Graph subgraph = new Graph(directed, weighted);
        for (Node node : nodes) {
            subgraph.addNode(node);
            for (Edge edge : getEdgesFromNode(node)) {
                if (nodes.contains(edge.getDestination())) {
                    subgraph.addEdge(edge.getSource(), edge.getDestination(), edge.getWeight());
                }
            }
        }
        return subgraph;
    }

    public double getTotalEdgeWeight() {
        return adjacencyList.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Edge::getWeight)
                .sum();
    }


}
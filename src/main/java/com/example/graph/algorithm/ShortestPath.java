package com.example.graph.algorithm;

import com.example.graph.core.Edge;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

public class ShortestPath {
    private final Graph graph;

    public ShortestPath(Graph graph) {
        this.graph = graph;
    }

    public Map<Node, PathResult> dijkstra(Node source) {
        if (!graph.containsNode(source)) {
            throw new IllegalArgumentException("Source node not in graph");
        }

        // Initialize data structures
        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::getDistance));
        
        // Initialize distances
        for (Node node : graph.getNodes()) {
            distances.put(node, Double.MAX_VALUE);
        }
        distances.put(source, 0.0);
        queue.add(new NodeDistance(source, 0.0));
        
        while (!queue.isEmpty()) {
            Node current = queue.poll().getNode();
            double currentDist = distances.get(current);
            
            // Explore neighbors
            for (Edge edge : graph.getEdgesFromNode(current)) {
                Node neighbor = edge.getDestination();
                double edgeWeight = edge.getWeight();
                double newDist = currentDist + edgeWeight;
                
                // If we found a shorter path to the neighbor
                if (newDist < distances.get(neighbor)) {
                    distances.put(neighbor, newDist);
                    predecessors.put(neighbor, current);
                    queue.add(new NodeDistance(neighbor, newDist));
                }
            }
        }
        
        // Build results
        Map<Node, PathResult> results = new HashMap<>();
        for (Node node : graph.getNodes()) {
            List<Node> path = buildPath(predecessors, source, node);
            results.put(node, new PathResult(path, distances.get(node)));
        }
        
        return results;
    }

    public Map<Node, PathResult> bfs(Node source) {
        if (!graph.containsNode(source)) {
            throw new IllegalArgumentException("Source node not in graph");
        }

        // Initialize data structures
        Map<Node, Integer> distances = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        
        // Initialize distances
        for (Node node : graph.getNodes()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(source, 0);
        queue.add(source);
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            int currentDist = distances.get(current);
            
            // Explore neighbors
            for (Edge edge : graph.getEdgesFromNode(current)) {
                Node neighbor = edge.getDestination();
                
                // If we haven't visited this neighbor yet
                if (distances.get(neighbor) == Integer.MAX_VALUE) {
                    distances.put(neighbor, currentDist + 1);
                    predecessors.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        
        // Build results
        Map<Node, PathResult> results = new HashMap<>();
        for (Node node : graph.getNodes()) {
            List<Node> path = buildPath(predecessors, source, node);
            results.put(node, new PathResult(path, (double) distances.get(node)));
        }
        
        return results;
    }

    private List<Node> buildPath(Map<Node, Node> predecessors, Node source, Node target) {
        LinkedList<Node> path = new LinkedList<>();
        if (predecessors.get(target) == null && !source.equals(target)) {
            return Collections.emptyList(); // No path exists
        }
        
        Node current = target;
        while (current != null && !current.equals(source)) {
            path.addFirst(current);
            current = predecessors.get(current);
        }
        path.addFirst(source);
        
        return path;
    }

    public static class PathResult {
        private final List<Node> path;
        private final double distance;

        public PathResult(List<Node> path, double distance) {
            this.path = path;
            this.distance = distance;
        }

        public List<Node> getPath() {
            return Collections.unmodifiableList(path);
        }

        public double getDistance() {
            return distance;
        }

        public boolean hasPath() {
            return !path.isEmpty();
        }
    }

    private static class NodeDistance {
        private final Node node;
        private final double distance;

        public NodeDistance(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        public Node getNode() {
            return node;
        }

        public double getDistance() {
            return distance;
        }
    }
}
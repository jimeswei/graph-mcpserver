package com.example.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import com.example.graph.algorithm.PageRank;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



class PageRankTest {
    private Graph graph;
    private Node nodeA, nodeB, nodeC;

    @BeforeEach
    void setUp() {
        graph = new Graph(true, false);
        nodeA = new Node("A");
        nodeB = new Node("B");
        nodeC = new Node("C");
        
        // A -> B, A -> C, B -> C, C -> A
        graph.addEdge(nodeA, nodeB);
        graph.addEdge(nodeA, nodeC);
        graph.addEdge(nodeB, nodeC);
        graph.addEdge(nodeC, nodeA);
    }

    @Test
    void testPageRank() {
        PageRank pageRank = new PageRank(graph);
        pageRank.compute();
        
        Map<Node, Double> ranks = pageRank.getPageRanks();
        
        // All ranks should be between 0 and 1
        for (Double rank : ranks.values()) {
            assertTrue(rank > 0 && rank < 1);
        }
        
        // Sum of all ranks should be 1
        double sum = ranks.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001);
        
        // In this graph, nodeC should have the highest rank
        double rankA = ranks.get(nodeA);
        double rankB = ranks.get(nodeB);
        double rankC = ranks.get(nodeC);
        
        assertTrue(rankC > rankA);
        assertTrue(rankC > rankB);
    }

    @Test
    void testDanglingNode() {
        Node nodeD = new Node("D");
        graph.addNode(nodeD);
        
        PageRank pageRank = new PageRank(graph);
        pageRank.compute();
        
        Map<Node, Double> ranks = pageRank.getPageRanks();
        double rankD = ranks.get(nodeD);
        
        // Dangling node should have some rank
        assertTrue(rankD > 0);
    }

    @Test
    void testSingleNodeGraph() {
        Graph singleNodeGraph = new Graph(true, false);
        Node singleNode = new Node("Single");
        singleNodeGraph.addNode(singleNode);
        
        PageRank pageRank = new PageRank(singleNodeGraph);
        pageRank.compute();
        
        Map<Node, Double> ranks = pageRank.getPageRanks();
        assertEquals(1.0, ranks.get(singleNode));
    }
}
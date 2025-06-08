package com.example.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import com.example.graph.algorithm.ShortestPath;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



class ShortestPathTest {
    private Graph unweightedGraph;
    private Graph weightedGraph;
    private Node nodeA, nodeB, nodeC, nodeD;

    @BeforeEach
    void setUp() {
        unweightedGraph = new Graph(false, false);
        weightedGraph = new Graph(false, true);
        
        nodeA = new Node("A");
        nodeB = new Node("B");
        nodeC = new Node("C");
        nodeD = new Node("D");
        
        // Unweighted graph: A-B-C, A-D
        unweightedGraph.addEdge(nodeA, nodeB);
        unweightedGraph.addEdge(nodeB, nodeC);
        unweightedGraph.addEdge(nodeA, nodeD);
        
        // Weighted graph: A-B (1), B-C (2), A-C (4), A-D (1), D-C (1)
        weightedGraph.addEdge(nodeA, nodeB, 1.0);
        weightedGraph.addEdge(nodeB, nodeC, 2.0);
        weightedGraph.addEdge(nodeA, nodeC, 4.0);
        weightedGraph.addEdge(nodeA, nodeD, 1.0);
        weightedGraph.addEdge(nodeD, nodeC, 1.0);
    }

    @Test
    void testBFS() {
        ShortestPath shortestPath = new ShortestPath(unweightedGraph);
        Map<Node, ShortestPath.PathResult> results = shortestPath.bfs(nodeA);
        
        // Check distances
        assertEquals(0.0, results.get(nodeA).getDistance());
        assertEquals(1.0, results.get(nodeB).getDistance());
        assertEquals(1.0, results.get(nodeD).getDistance());
        assertEquals(2.0, results.get(nodeC).getDistance());
        
        // Check path to C
        List<Node> pathToC = results.get(nodeC).getPath();
        assertEquals(3, pathToC.size());
        assertEquals(nodeA, pathToC.get(0));
        assertEquals(nodeB, pathToC.get(1));
        assertEquals(nodeC, pathToC.get(2));
    }

    @Test
    void testDijkstra() {
        ShortestPath shortestPath = new ShortestPath(weightedGraph);
        Map<Node, ShortestPath.PathResult> results = shortestPath.dijkstra(nodeA);
        
        // Check distances
        assertEquals(0.0, results.get(nodeA).getDistance());
        assertEquals(1.0, results.get(nodeB).getDistance());
        assertEquals(1.0, results.get(nodeD).getDistance());
        assertEquals(2.0, results.get(nodeC).getDistance()); // A->D->C = 1+1=2
        
        // Check path to C
        List<Node> pathToC = results.get(nodeC).getPath();
        assertEquals(3, pathToC.size());
        assertEquals(nodeA, pathToC.get(0));
        assertEquals(nodeD, pathToC.get(1));
        assertEquals(nodeC, pathToC.get(2));
    }

    @Test
    void testDisconnectedGraph() {
        Node isolated = new Node("Isolated");
        unweightedGraph.addNode(isolated);
        
        ShortestPath shortestPath = new ShortestPath(unweightedGraph);
        Map<Node, ShortestPath.PathResult> results = shortestPath.bfs(nodeA);
        
        ShortestPath.PathResult isolatedResult = results.get(isolated);
        assertTrue(isolatedResult.getPath().isEmpty());
        assertEquals(Double.MAX_VALUE, isolatedResult.getDistance());
    }

    @Test
    void testSelfNode() {
        ShortestPath shortestPath = new ShortestPath(weightedGraph);
        Map<Node, ShortestPath.PathResult> results = shortestPath.dijkstra(nodeA);
        
        ShortestPath.PathResult aResult = results.get(nodeA);
        assertEquals(1, aResult.getPath().size());
        assertEquals(nodeA, aResult.getPath().get(0));
        assertEquals(0.0, aResult.getDistance());
    }
}
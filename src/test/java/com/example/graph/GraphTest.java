package com.example.graph;



import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

import com.example.graph.core.Edge;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class GraphTest {
    private Graph graph;
    private Node nodeA;
    private Node nodeB;
    private Node nodeC;

    @BeforeEach
    void setUp() {
        graph = new Graph(false, false);
        nodeA = new Node("A");
        nodeB = new Node("B");
        nodeC = new Node("C");
        
        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addEdge(nodeA, nodeB);
    }

    @Test
    public void testGetNodes() {
        System.out.println("测试依赖是否对");
    }

    @Test
    void testAddNode() {
        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.containsNode(nodeA));
        assertTrue(graph.containsNode(nodeB));
    }

    @Test
    void testAddEdge() {
        assertEquals(1, graph.getEdgesFromNode(nodeA).size());
        assertEquals(1, graph.getEdgesFromNode(nodeB).size());

        Collection<Node> neighbors = graph.getNeighbors(nodeA);
        assertEquals(1, neighbors.size());
        assertEquals(nodeB, neighbors.stream().toList().get(0));
    }

    @Test
    void testDirectedGraph() {
        Graph directedGraph = new Graph(true, false);
        directedGraph.addEdge(nodeA, nodeB);
        
        assertEquals(1, directedGraph.getEdgesFromNode(nodeA).size());
        assertEquals(0, directedGraph.getEdgesFromNode(nodeB).size());
    }

    @Test
    void testWeightedGraph() {
        Graph weightedGraph = new Graph(false, true);
        weightedGraph.addEdge(nodeA, nodeB, 5.0);
        
        Edge edge = weightedGraph.getEdgesFromNode(nodeA).stream().toList().get(0);
        assertEquals(5.0, edge.getWeight());
    }

    @Test
    void testCreateSubgraph() {
        graph.addNode(nodeC);
        graph.addEdge(nodeA, nodeC);
        
        Set<Node> subNodes = new HashSet<>();
        subNodes.add(nodeA);
        subNodes.add(nodeB);
        
        Graph subgraph = graph.createSubgraph(subNodes);
        
        assertEquals(2, subgraph.getNodes().size());
        assertEquals(1, subgraph.getEdges().size());
        assertTrue(subgraph.getEdgesFromNode(nodeA).size() > 0);
    }
}
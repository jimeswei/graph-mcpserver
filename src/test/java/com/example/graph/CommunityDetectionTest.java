package com.example.graph;

import com.example.graph.algorithm.CommunityDetection;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import com.example.graph.core.Edge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommunityDetectionTest {
    private Graph mockGraph;
    private Node nodeA, nodeB, nodeC, nodeD;

    @BeforeEach
    void setUp() {
        // 初始化模拟节点
        nodeA = new Node("A");
        nodeB = new Node("B");
        nodeC = new Node("C");
        nodeD = new Node("D");

        // 创建模拟图
        mockGraph = mock(Graph.class);
        when(mockGraph.getNodes()).thenReturn(Set.of(nodeA, nodeB, nodeC, nodeD));
    }

    // 测试用例 1: 空图处理
    @Test
    void testEmptyGraph() {
        when(mockGraph.getNodes()).thenReturn(Collections.emptySet());
        CommunityDetection detector = new CommunityDetection(mockGraph);
        detector.detectCommunities();

        assertTrue(detector.getCommunities().isEmpty());
        assertEquals(0.0, detector.getModularity());
    }

    // 测试用例 2: 单节点图
    @Test
    void testSingleNode() {
        when(mockGraph.getNodes()).thenReturn(Set.of(nodeA));
        when(mockGraph.getEdgesFromNode(nodeA)).thenReturn(Collections.emptyList());
        when(mockGraph.getNeighbors(nodeA)).thenReturn(Collections.emptySet());
        when(mockGraph.getTotalEdgeWeight()).thenReturn(0.0);

        CommunityDetection detector = new CommunityDetection(mockGraph);
        detector.detectCommunities();

        Map<Integer, List<Node>> communities = detector.getCommunities();
        assertEquals(1, communities.size());
        assertTrue(communities.values().iterator().next().contains(nodeA));
        assertEquals(0.0, detector.getModularity());
    }

    // 测试用例 3: 两个紧密连接的社区
    @Test
    void testTwoCommunities() {
        // 配置边关系
        // 社区1: A-B (权重1.0)
        Edge edgeAB = new Edge(nodeA, nodeB, 1.0);
        // 社区2: C-D (权重1.0)
        Edge edgeCD = new Edge(nodeC, nodeD, 1.0);

        // 设置图行为
        when(mockGraph.getEdgesFromNode(nodeA)).thenReturn(List.of(edgeAB));
        when(mockGraph.getEdgesFromNode(nodeB)).thenReturn(List.of(new Edge(nodeB, nodeA, 1.0)));
        when(mockGraph.getEdgesFromNode(nodeC)).thenReturn(List.of(edgeCD));
        when(mockGraph.getEdgesFromNode(nodeD)).thenReturn(List.of(new Edge(nodeD, nodeC, 1.0)));
        when(mockGraph.getTotalEdgeWeight()).thenReturn(2.0); // 总权重 = 1+1

        // 邻居配置
        when(mockGraph.getNeighbors(nodeA)).thenReturn(Set.of(nodeB));
        when(mockGraph.getNeighbors(nodeB)).thenReturn(Set.of(nodeA));
        when(mockGraph.getNeighbors(nodeC)).thenReturn(Set.of(nodeD));
        when(mockGraph.getNeighbors(nodeD)).thenReturn(Set.of(nodeC));

        CommunityDetection detector = new CommunityDetection(mockGraph);
        detector.detectCommunities();

        // 验证社区数量
        Map<Integer, List<Node>> communities = detector.getCommunities();
        assertEquals(2, communities.size());

        // 验证社区分组
        List<Node> comm1 = communities.values().stream().filter(list -> list.contains(nodeA)).findFirst().get();
        List<Node> comm2 = communities.values().stream().filter(list -> list.contains(nodeC)).findFirst().get();
        assertTrue(comm1.contains(nodeB) && !comm1.contains(nodeC));
        assertTrue(comm2.contains(nodeD) && !comm2.contains(nodeA));

        // 验证模块度 (预期值 = 0.5)
        double mod = detector.getModularity();
        double expectedModularity = 0.5; // Q = (1 - (1 * 1)/(2 * 2)) * 2  / 2
        assertEquals(expectedModularity, mod, 0.01);
    }

    // 测试用例 4: 节点移动逻辑
    @Test
    void testNodeMovement() {
        // 初始状态：A-B-C 连接，D 孤立
        Edge edgeAB = new Edge(nodeA, nodeB, 1.0);
        Edge edgeBC = new Edge(nodeB, nodeC, 1.0);

        // 配置节点关系
        when(mockGraph.getEdgesFromNode(nodeA)).thenReturn(List.of(edgeAB));
        when(mockGraph.getEdgesFromNode(nodeB)).thenReturn(Arrays.asList(
                new Edge(nodeB, nodeA, 1.0),
                new Edge(nodeB, nodeC, 1.0)
        ));
        when(mockGraph.getEdgesFromNode(nodeC)).thenReturn(List.of(new Edge(nodeC, nodeB, 1.0)));
        when(mockGraph.getEdgesFromNode(nodeD)).thenReturn(Collections.emptyList());
        when(mockGraph.getTotalEdgeWeight()).thenReturn(2.0);

        // 邻居配置
        when(mockGraph.getNeighbors(nodeA)).thenReturn(Set.of(nodeB));
        when(mockGraph.getNeighbors(nodeB)).thenReturn(Set.of(nodeA, nodeC));
        when(mockGraph.getNeighbors(nodeC)).thenReturn(Set.of(nodeB));
        when(mockGraph.getNeighbors(nodeD)).thenReturn(Collections.emptySet());

        CommunityDetection detector = new CommunityDetection(mockGraph);
        detector.detectCommunities();

        // 验证：A/B/C 应在一个社区（B的桥梁作用）
        Map<Integer, List<Node>> communities = detector.getCommunities();
        List<Node> mainComm = communities.values().stream()
                .filter(list -> list.size() == 3)
                .findFirst()
                .orElse(null);
        assertNotNull(mainComm);
        assertTrue(mainComm.containsAll(List.of(nodeA, nodeB, nodeC)));
    }

    // 测试用例 5: 模块度计算逻辑
    @Test
    void testModularityCalculation() {
        // 构造已知模块度的图
        // 两个节点全连接：A-B (权重2.0)
        Edge edgeAB = new Edge(nodeA, nodeB, 2.0);

        when(mockGraph.getEdgesFromNode(nodeA)).thenReturn(List.of(edgeAB));
        when(mockGraph.getEdgesFromNode(nodeB)).thenReturn(List.of(new Edge(nodeB, nodeA, 2.0)));
        when(mockGraph.getTotalEdgeWeight()).thenReturn(2.0); // 总权重=2

        CommunityDetection detector = new CommunityDetection(mockGraph);
        detector.detectCommunities();

        // 预期模块度公式：Q = (2 - (2 * 2)/(2 * 2)) / (2 * 2) = 0.25
        double expected = 0.25;
        assertEquals(expected, detector.getModularity(), 0.001);
    }
}
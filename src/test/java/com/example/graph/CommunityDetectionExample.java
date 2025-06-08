package com.example.graph;

import com.example.graph.algorithm.CommunityDetection;
import com.example.graph.core.Graph;
import com.example.graph.core.Node;

import java.util.List;
import java.util.Map;

public class CommunityDetectionExample {
    public static void main(String[] args) {
        // 创建图
        Graph graph = new Graph();
        
        // 创建节点
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        Node d = new Node("D");
        Node e = new Node("E");
        Node f = new Node("F");
        
        // 添加节点（可选，addEdge 会自动添加）
        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        graph.addNode(d);
        graph.addNode(e);
        graph.addNode(f);
        
        // 社区1：A-B-C（无向图）
        graph.addEdge(a, b, 1.0); // 自动添加反向边
        graph.addEdge(b, c, 1.0); // 自动添加反向边
        
        // 社区2：D-E-F（无向图）
        graph.addEdge(d, e, 1.0); // 自动添加反向边
        graph.addEdge(e, f, 1.0); // 自动添加反向边
        
        // 社区间连接（弱连接，无向）
        graph.addEdge(c, d, 0.1); // 自动添加反向边
        
        // 执行社区检测
        CommunityDetection detector = new CommunityDetection(graph);
        detector.detectCommunities();
        
        // 输出结果
        System.out.println("模块度: " + detector.getModularity());
        
        System.out.println("\n社区结构:");
        Map<Integer, List<Node>> communities = detector.getCommunities();
        for (Map.Entry<Integer, List<Node>> entry : communities.entrySet()) {
            System.out.println("社区 " + entry.getKey() + ":");
            for (Node node : entry.getValue()) {
                System.out.println("  - " + node.getId());
            }
        }
    }
}
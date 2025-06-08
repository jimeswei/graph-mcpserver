package com.example.graph.mcp.service;

import com.example.graph.core.Graph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.HashMap;
import com.example.graph.algorithm.CommunityDetection;
import com.example.graph.algorithm.JaccardSimilarity;
import com.example.graph.algorithm.PageRank;
import com.example.graph.core.Node;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Slf4j
@Service
public class AlgorithmService {

    @Autowired
    private GraphDataService graphDataService;

    public String communityDetection(Graph graph) throws IOException {
        CommunityDetection detection = new CommunityDetection(graph);
        detection.detectCommunities();
        // 输出每个节点所属社团
        var communities = detection.getCommunities(); // Map<Integer, List<Node>>
        // 构造节点到社团编号的映射
        var nodeToCommunity = new HashMap<String, Integer>();
        for (var entry : communities.entrySet()) {
            int commId = entry.getKey();
            for (Node node : entry.getValue()) {
                nodeToCommunity.put(node.getId(), commId);
            }
        }
        // 返回JSON
        ObjectMapper mapper = new ObjectMapper();
        var result = new HashMap<String, Object>();
        result.put("communities", nodeToCommunity);
        result.put("modularity", detection.getModularity());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    public String pageank(Graph graph) throws IOException {
        PageRank pr = new PageRank(graph);
        pr.compute();
        var pageRanks = pr.getPageRanks(); // Map<Node, Double>
        var result = new HashMap<String, Double>();
        for (var entry : pageRanks.entrySet()) {
            result.put(entry.getKey().getId(), entry.getValue());
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    public String jaccardSimilarity(Graph graph) throws IOException {
        JaccardSimilarity similarity = new JaccardSimilarity(graph);
        var result = similarity.computeAll(); // Map<String, Double>
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    // 支持直接传入人名列表的社团检测
    @Tool(name = "community_detection", description = "社团检测")
    public String communityDetection(List<String> names) throws IOException {
        Graph graph = graphDataService.querySubgraphByNames(names);
        return communityDetection(graph);
    }

    // 支持直接传入人名列表的pagerank
    @Tool(name = "pagerank", description = "pagerank计算节点重要性")
    public String pageank(List<String> names) throws IOException {
        Graph graph = graphDataService.querySubgraphByNames(names);
        return pageank(graph);
    }

    // 支持直接传入人名列表的Jaccard相似度
    @Tool(name = "jaccard_similarity", description = "Jaccard相似度")
    public String jaccardSimilarity(List<String> names) throws IOException {
        Graph graph = graphDataService.querySubgraphByNames(names);
        return jaccardSimilarity(graph);
    }

}

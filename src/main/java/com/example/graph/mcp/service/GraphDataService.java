package com.example.graph.mcp.service;

import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import com.example.graph.core.Edge;
import com.example.graph.mcp.util.JsonExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GraphDataService extends BaseGraphServiceOptimized {

    /**
     * 根据指定节点名称集合，查询这些节点及其搭档关系，组装为Graph对象
     */
    public Graph getSubgraphByNames(List<String> names) {
        // 1. 查询图数据库，获取names及其一阶搭档的所有节点和边
        // 伪代码：实际应调用父类或DAO层的Gremlin/SQL等查询
        List<Node> nodes = queryNodesByNames(names);
        List<Edge> edges = queryEdgesByNames(names);

        // 2. 构建Graph对象
        Graph graph = new Graph(false, false);
        for (Node node : nodes) {
            graph.addNode(node);
        }
        for (Edge edge : edges) {
            graph.addEdge(edge.getSource(), edge.getDestination());
        }
        return graph;
    }

    public List<Node> queryNodesByNames(List<String> names) {
        validateInput(names);
        try {
            // 构造Gremlin查询
            String gremlin = "g.V().has('celebrity', 'name', within([${names}])).as('center').both('celebrity_celebrity').as('partner').select('center','partner').by(valueMap('celebrity_id','name','profession','company','nationality'))";
            Map<String, Object> params = new HashMap<>();
            params.put("names", "'" + String.join("','", names) + "'");
            ResponseEntity<String> response = executeGremlinRequest(gremlin, params);
            String json = JsonExtractor.parseResponse(response.getBody());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(json);
            List<Node> result = new ArrayList<>();
            for (JsonNode obj : arr) {
                // 取center和partner
                for (String key : new String[] { "center", "partner" }) {
                    JsonNode nodeObj = obj.get(key);
                    if (nodeObj != null && nodeObj.has("name")) {
                        String id = nodeObj.get("name").asText();
                        Node node = new Node(id);
                        node.setAttribute("celebrity_id", nodeObj.path("celebrity_id").asText(""));
                        node.setAttribute("profession", nodeObj.path("profession").asText(""));
                        node.setAttribute("company", nodeObj.path("company").asText(""));
                        node.setAttribute("nationality", nodeObj.path("nationality").asText(""));
                        result.add(node);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("queryNodesByNames error", e);
        }
    }

    public List<Edge> queryEdgesByNames(List<String> names) {
        validateInput(names);
        try {
            // 构造Gremlin查询
            String gremlin = "g.V().has('celebrity', 'name', within([${names}])).bothE('celebrity_celebrity').as('e').otherV().as('other').select('e','other').by(valueMap()).by(valueMap('name'))";
            Map<String, Object> params = new HashMap<>();
            params.put("names", "'" + String.join("','", names) + "'");
            ResponseEntity<String> response = executeGremlinRequest(gremlin, params);
            String json = JsonExtractor.parseResponse(response.getBody());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(json);
            List<Edge> result = new ArrayList<>();
            for (JsonNode obj : arr) {
                JsonNode eObj = obj.get("e");
                JsonNode otherObj = obj.get("other");
                if (eObj != null && otherObj != null && otherObj.has("name")) {
                    String sourceId = eObj.path("outV").asText("");
                    String targetId = otherObj.get("name").asText("");
                    Node source = new Node(sourceId);
                    Node target = new Node(targetId);
                    double weight = 1.0;
                    if (eObj.has("weight")) {
                        try {
                            weight = eObj.get("weight").asDouble();
                        } catch (Exception ignore) {
                        }
                    }
                    result.add(new Edge(source, target, weight));
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("queryEdgesByNames error", e);
        }
    }

    public Graph querySubgraphByNames(List<String> names) {
        List<Node> nodes = queryNodesByNames(names);
        List<Edge> edges = queryEdgesByNames(names);
        Graph graph = new Graph(false, false);
        for (Node node : nodes) {
            graph.addNode(node);
        }
        for (Edge edge : edges) {
            graph.addEdge(edge.getSource(), edge.getDestination(), edge.getWeight());
        }
        return graph;
    }
}
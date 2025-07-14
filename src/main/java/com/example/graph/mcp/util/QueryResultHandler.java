package com.example.graph.mcp.util;

import com.example.graph.core.Edge;
import com.example.graph.core.Node;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.example.graph.mcp.constant.GraphConstants.*;

public class QueryResultHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理图查询结果 - 集成结果优化器
     */
    public static String processGraphQueryResult(ResponseEntity<String> response) throws IOException {
        String relationsJson = JsonExtractor.parseGraphView(response.getBody());
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        String rawResult = JsonExtractor.buildFinalResponseSimple(detailsJson, relationsJson);
        
        // 使用结果优化器进行优化
        return ResultOptimizer.optimizeGraphResult(rawResult);
    }

    /**
     * 处理路径查询结果（专门用于关系链查询）
     */
    public static String processPathQueryResult(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = objectMapper.readTree(detailsJson);
        
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode pathsArray = result.putArray("paths");
        
        if (root.isArray()) {
            for (JsonNode path : root) {
                ObjectNode pathNode = objectMapper.createObjectNode();
                ArrayNode nodesArray = pathNode.putArray("nodes");
                ArrayNode relationshipsArray = pathNode.putArray("relationships");
                
                if (path.has("objects")) {
                    JsonNode objects = path.get("objects");
                    if (objects.isArray()) {
                        // 处理节点
                        for (int i = 0; i < objects.size(); i++) {
                            JsonNode obj = objects.get(i);
                            ObjectNode node = objectMapper.createObjectNode();
                            
                            // 提取节点信息
                            String name = extractValue(obj, "name");
                            String profession = extractValue(obj, "profession");
                            if (name != null) {
                                node.put("name", name);
                                if (profession != null) {
                                    node.put("profession", profession);
                                }
                                nodesArray.add(node);
                            }
                            
                            // 如果不是最后一个节点，处理关系
                            if (i < objects.size() - 1) {
                                ObjectNode relationship = objectMapper.createObjectNode();
                                String weight = extractValue(obj, "weight");
                                String type = extractValue(obj, "relationship_type");
                                
                                if (weight != null) {
                                    relationship.put("weight", weight);
                                }
                                relationship.put("type", type != null ? type : "关联");
                                relationship.put("source", name);
                                relationship.put("target", extractValue(objects.get(i + 1), "name"));
                                relationshipsArray.add(relationship);
                            }
                        }
                    }
                }
                
                // 添加路径长度
                pathNode.put("length", nodesArray.size() - 1);
                // 生成路径表达式
                pathNode.put("expression", generatePathExpression(nodesArray, relationshipsArray));
                pathsArray.add(pathNode);
            }
        }
        
        // 添加统计信息
        result.put("totalPaths", pathsArray.size());
        result.put("summary", String.format("找到 %d 条关系路径", pathsArray.size()));
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /**
     * 生成路径表达式
     */
    private static String generatePathExpression(ArrayNode nodes, ArrayNode relationships) {
        if (nodes.size() < 2) {
            return "";
        }
        
        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            expression.append("[").append(node.get("name").asText()).append("]");
            
            if (i < relationships.size()) {
                JsonNode rel = relationships.get(i);
                expression.append(" --")
                         .append(rel.has("type") ? "|" + rel.get("type").asText() + "|" : "")
                         .append("--> ");
            }
        }
        
        return expression.toString();
    }

    /**
     * 处理共同好友查询结果
     */
    public static String processMutualFriendsResult(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = objectMapper.readTree(detailsJson);
        
        List<String> mutualFriends = new ArrayList<>();
        List<Map<String, Object>> relationships = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode item : root) {
                // 处理 commonFriend 部分
                if (item.has("commonFriend")) {
                    JsonNode friend = item.get("commonFriend");
                    String name = extractValue(friend, "name");
                    String profession = extractValue(friend, "profession");
                    
                    if (name != null && !mutualFriends.contains(name)) {
                        String friendInfo = profession != null ? 
                            String.format("%s (%s)", name, profession) : name;
                        mutualFriends.add(friendInfo);
                    }
                }
                
                // 处理 relationships 部分
                if (item.has("relationships")) {
                    JsonNode rels = item.get("relationships");
                    if (rels.isArray()) {
                        for (JsonNode rel : rels) {
                            Map<String, Object> relationship = new HashMap<>();
                            relationship.put("weight", extractValue(rel, "weight"));
                            relationship.put("type", extractValue(rel, "relationship_type"));
                            relationships.add(relationship);
                        }
                    }
                }
            }
        }
        
        // 构建优化后的结果
        return buildEnhancedMutualFriendsResponse(mutualFriends, relationships);
    }

    /**
     * 从JsonNode中提取值（处理数组和单值情况）
     */
    private static String extractValue(JsonNode node, String field) {
        if (node.has(field)) {
            JsonNode value = node.get(field);
            if (value.isArray() && value.size() > 0) {
                return value.get(0).asText();
            } else {
                return value.asText();
            }
        }
        return null;
    }

    /**
     * 构建增强的共同好友响应
     */
    private static String buildEnhancedMutualFriendsResponse(List<String> friends, List<Map<String, Object>> relationships) throws IOException {
        ObjectNode result = objectMapper.createObjectNode();
        
        // 添加共同好友列表
        ArrayNode friendsArray = result.putArray("mutualFriends");
        friends.forEach(friendsArray::add);
        
        // 添加关系信息
        ArrayNode relsArray = result.putArray("relationships");
        relationships.forEach(rel -> {
            ObjectNode relNode = objectMapper.createObjectNode();
            rel.forEach((k, v) -> {
                if (v != null) {
                    relNode.put(k, v.toString());
                }
            });
            relsArray.add(relNode);
        });
        
        // 添加统计信息
        result.put("count", friends.size());
        result.put("summary", String.format("找到 %d 个共同好友", friends.size()));
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    public static List<Node> extractNodes(ResponseEntity<String> response) throws IOException {
        String json = JsonExtractor.parseResponse(response.getBody());
        JsonNode arr = objectMapper.readTree(json);
        List<Node> result = new ArrayList<>();

        for (JsonNode obj : arr) {
            for (String key : new String[] { "center", "partner" }) {
                JsonNode nodeObj = obj.get(key);
                if (nodeObj != null && nodeObj.has(NAME_PROPERTY)) {
                    String id = nodeObj.get(NAME_PROPERTY).asText();
                    Node node = new Node(id);
                    // 只保留必要的属性
                    if (nodeObj.has(PROFESSION_PROPERTY)) {
                        node.setAttribute(PROFESSION_PROPERTY, nodeObj.get(PROFESSION_PROPERTY).asText(""));
                    }
                    if (nodeObj.has(NATIONALITY_PROPERTY)) {
                        node.setAttribute(NATIONALITY_PROPERTY, nodeObj.get(NATIONALITY_PROPERTY).asText(""));
                    }
                    result.add(node);
                }
            }
        }
        return result;
    }

    public static List<Edge> extractEdges(ResponseEntity<String> response) throws IOException {
        String json = JsonExtractor.parseResponse(response.getBody());
        JsonNode arr = objectMapper.readTree(json);
        List<Edge> result = new ArrayList<>();

        for (JsonNode obj : arr) {
            JsonNode eObj = obj.get("e");
            JsonNode otherObj = obj.get("other");
            if (eObj != null && otherObj != null && otherObj.has(NAME_PROPERTY)) {
                String sourceId = eObj.path("outV").asText("");
                String targetId = otherObj.get(NAME_PROPERTY).asText("");
                Node source = new Node(sourceId);
                Node target = new Node(targetId);
                double weight = 1.0;
                if (eObj.has(WEIGHT_PROPERTY)) {
                    try {
                        weight = eObj.get(WEIGHT_PROPERTY).asDouble();
                    } catch (Exception ignore) {
                    }
                }
                result.add(new Edge(source, target, weight));
            }
        }
        return result;
    }



    public static Map<String, Object> processCommonAncestorResult(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> processedResult = new HashMap<>();
        Map<String, Object> firstResult = results.get(0);

        processedResult.put("person1", firstResult.get("person1"));
        processedResult.put("person2", firstResult.get("person2"));
        
        Map<String, Object> commonAncestor = (Map<String, Object>) firstResult.get("common_ancestor");
        if (commonAncestor != null) {
            processedResult.put("common_ancestor", commonAncestor);
            processedResult.put("found", true);
        } else {
            processedResult.put("found", false);
        }

        return processedResult;
    }

    public static Map<String, Object> processCommonAncestorByIdResult(List<Map<String, Object>> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> processedResult = new HashMap<>();
        Map<String, Object> firstResult = results.get(0);

        List<String> children = (List<String>) firstResult.get("children");
        processedResult.put("children", children);
        
        Map<String, Object> ancestor = (Map<String, Object>) firstResult.get("ancestor");
        if (ancestor != null) {
            processedResult.put("ancestor", ancestor);
            processedResult.put("found", true);
        } else {
            processedResult.put("found", false);
        }

        return processedResult;
    }
}
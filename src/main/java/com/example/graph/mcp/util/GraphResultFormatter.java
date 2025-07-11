package com.example.graph.mcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.*;

/**
 * 图结果格式化器 - 统一处理返回vertices和edges格式的结果
 * 简洁优雅的设计，避免重复代码
 */
@Slf4j
public class GraphResultFormatter {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * 格式化关系链查询结果
     */
    public static String formatRelationChain(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = mapper.readTree(detailsJson);
        
        return buildGraphResult(extractRelationChainData(root));
    }
    
    /**
     * 格式化共同好友查询结果
     */
    public static String formatMutualFriends(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = mapper.readTree(detailsJson);
        
        return buildGraphResult(extractMutualFriendsData(root));
    }
    
    /**
     * 格式化共同作品查询结果
     */
    public static String formatCommonWorks(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = mapper.readTree(detailsJson);
        
        return buildGraphResult(extractCommonWorksData(root));
    }
    
    /**
     * 格式化相似度查询结果
     */
    public static String formatSimilarity(ResponseEntity<String> response) throws IOException {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return createEmptyResult();
        }
        
        String jsonData = JsonExtractor.parseResponse(responseBody);
        
        if (jsonData == null || jsonData.trim().isEmpty() || "[]".equals(jsonData.trim())) {
            return createEmptyResult();
        }

        JsonNode root = mapper.readTree(jsonData);
        GraphData data = extractSimilarityData(root);
        return buildSimilarityResult(data);
    }
    
    /**
     * 格式化共同祖先查询结果
     */
    public static String formatCommonAncestor(ResponseEntity<String> response) throws IOException {
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        JsonNode root = mapper.readTree(detailsJson);
        
        return buildGraphResult(extractCommonAncestorData(root));
    }
    
    /**
     * 构建统一的图结果格式
     */
    private static String buildGraphResult(GraphData data) throws IOException {
        ObjectNode result = mapper.createObjectNode();
        
        // 创建vertices数组
        ArrayNode verticesArray = result.putArray("vertices");
        if (data != null && data.vertices != null) {
            for (Vertex vertex : data.vertices) {
                ObjectNode vertexNode = createVertexNode(vertex);
                verticesArray.add(vertexNode);
            }
        }
        
        // 创建edges数组
        ArrayNode edgesArray = result.putArray("edges");
        if (data != null && data.edges != null) {
            for (Edge edge : data.edges) {
                ObjectNode edgeNode = createEdgeNode(edge);
                edgesArray.add(edgeNode);
            }
        }
        
        // 添加摘要
        result.put("summary", data != null ? data.summary : "");
        
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
    
    /**
     * 创建顶点节点
     */
    private static ObjectNode createVertexNode(Vertex vertex) {
        ObjectNode node = mapper.createObjectNode();
        
        // 创建数组格式的属性值
        if (vertex.name != null) {
            ArrayNode titleArray = mapper.createArrayNode();
            titleArray.add(vertex.name);
            node.set("title", titleArray);
        }
        
        if (vertex.description != null) {
            ArrayNode descArray = mapper.createArrayNode();
            descArray.add(vertex.description);
            node.set("description", descArray);
        }
        
        return node;
    }
    
    /**
     * 创建边节点
     */
    private static ObjectNode createEdgeNode(Edge edge) {
        ObjectNode node = mapper.createObjectNode();
        if (edge.id != null) {
            node.put("id", edge.id);
        }
        node.put("label", "celebrity_celebrity");
        if (edge.source != null) {
            node.put("source", edge.source);
        }
        if (edge.target != null) {
            node.put("target", edge.target);
        }
        
        // 添加properties对象
        ObjectNode properties = mapper.createObjectNode();
        properties.put("from", edge.from);
        properties.put("to", edge.to);
        properties.put("e_type", edge.type);
        if (edge.weight != null) {
            properties.put("weight", edge.weight);
        }
        if (edge.similarity != null) {
            properties.put("similarity", edge.similarity);
        }
        node.set("properties", properties);
        
        return node;
    }
    
    /**
     * 提取关系链数据
     */
    private static GraphData extractRelationChainData(JsonNode root) {
        Set<Vertex> vertices = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode path : root) {
                if (path.has("objects") && path.get("objects").isArray()) {
                    JsonNode objects = path.get("objects");
                    
                    // 提取顶点
                    for (JsonNode obj : objects) {
                        Vertex vertex = createVertex(obj);
                        if (vertex != null) {
                            vertices.add(vertex);
                        }
                    }
                    
                    // 提取边
                    for (int i = 0; i < objects.size() - 1; i++) {
                        String from = extractStringValue(objects.get(i), "name");
                        String to = extractStringValue(objects.get(i + 1), "name");
                        String type = extractStringValue(objects.get(i), "relationship_type", "好友");
                        
                        if (from != null && to != null) {
                            edges.add(new Edge(from, to, type, null));
                        }
                    }
                }
            }
        }
        
        String summary = String.format("找到 %d 条关系路径，包含 %d 个节点", 
                                       edges.size() > 0 ? 1 : 0, vertices.size());
        return new GraphData(new ArrayList<>(vertices), edges, summary);
    }
    
    /**
     * 提取共同好友数据
     */
    private static GraphData extractMutualFriendsData(JsonNode root) {
        Set<Vertex> vertices = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode item : root) {
                if (item.has("commonFriend")) {
                    Vertex friend = createVertex(item.get("commonFriend"));
                    if (friend != null) {
                        vertices.add(friend);
                    }
                }
            }
        }
        
        String summary = String.format("找到 %d 个共同好友", vertices.size());
        return new GraphData(new ArrayList<>(vertices), edges, summary);
    }
    
    /**
     * 提取共同作品数据
     */
    private static GraphData extractCommonWorksData(JsonNode root) {
        Set<Vertex> vertices = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode work : root) {
                // 提取作品信息，处理数组格式的属性值
                String title = extractArrayValue(work, "title");
                String releaseDate = extractArrayValue(work, "release_date");
                String workType = extractArrayValue(work, "work_type", "电影");
                String description = extractArrayValue(work, "description", "");
                
                if (title != null) {
                    // 创建作品节点，包含所有信息
                    Vertex workVertex = new Vertex(title, null, workType, null, null, null, releaseDate, description);
                    vertices.add(workVertex);
                }
            }
        }
        
        String summary = String.format("找到 %d 部共同作品", vertices.size());
        return new GraphData(new ArrayList<>(vertices), edges, summary);
    }
    
    /**
     * 提取相似度数据
     */
    private static GraphData extractSimilarityData(JsonNode root) {
        List<Vertex> vertices = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode item : root) {
                // 处理每个结果项
                if (item.isObject()) {
                    String title = null;
                    String description = null;
                    String type = null;
                    
                    // 尝试获取作品信息
                    if (item.has("title")) {
                        title = extractStringValue(item, "title");
                        description = extractStringValue(item, "description");
                        type = "作品";
                    }
                    // 尝试获取活动信息
                    else if (item.has("event_date")) {
                        title = extractStringValue(item, "name");
                        description = extractStringValue(item, "event_date");
                        type = "活动";
                    }
                    // 尝试获取好友信息
                    else if (item.has("name")) {
                        title = extractStringValue(item, "name");
                        description = extractStringValue(item, "profession", "共同好友");
                        type = "好友";
                    }
                    
                    if (title != null) {
                        vertices.add(new Vertex(title, null, type, null, null, null, null, description));
                    }
                }
            }
        }
        
        // 构建摘要
        int workCount = 0;
        int eventCount = 0;
        int friendCount = 0;
        
        for (Vertex v : vertices) {
            if ("作品".equals(v.profession)) workCount++;
            else if ("活动".equals(v.profession)) eventCount++;
            else if ("好友".equals(v.profession)) friendCount++;
        }
        
        String summary = String.format("找到%d部共同作品，%d个共同活动，%d个共同好友", 
            workCount, eventCount, friendCount);
        
        return new GraphData(vertices, new ArrayList<>(), summary);
    }
    
    /**
     * 提取共同祖先数据
     */
    private static GraphData extractCommonAncestorData(JsonNode root) {
        Set<Vertex> vertices = new LinkedHashSet<>();
        List<Edge> edges = new ArrayList<>();
        
        if (root.isArray()) {
            for (JsonNode item : root) {
                // 处理查询到的人物和祖先
                if (item.has("p1")) {
                    vertices.add(new Vertex(item.get("p1").asText(), null, null, null, null, null));
                }
                if (item.has("p2")) {
                    vertices.add(new Vertex(item.get("p2").asText(), null, null, null, null, null));
                }
                if (item.has("grandparent1") || item.has("parent1")) {
                    JsonNode ancestor = item.has("grandparent1") ? 
                                       item.get("grandparent1") : item.get("parent1");
                    Vertex ancestorVertex = createVertex(ancestor);
                    if (ancestorVertex != null) {
                        vertices.add(ancestorVertex);
                        
                        // 添加祖先关系边
                        if (item.has("p1")) {
                            edges.add(new Edge(ancestorVertex.name, item.get("p1").asText(), 
                                             "祖先", null));
                        }
                        if (item.has("p2")) {
                            edges.add(new Edge(ancestorVertex.name, item.get("p2").asText(), 
                                             "祖先", null));
                        }
                    }
                }
            }
        }
        
        String summary = String.format("找到 %d 个共同祖先关系", edges.size());
        return new GraphData(new ArrayList<>(vertices), edges, summary);
    }
    
    /**
     * 创建顶点对象
     */
    private static Vertex createVertex(JsonNode node) {
        String name = extractStringValue(node, "name");
        if (name == null) {
            name = extractStringValue(node, "title"); // 作品标题
        }
        
        if (name == null) return null;
        
        String gender = extractStringValue(node, "gender");
        String profession = extractStringValue(node, "profession");
        String education = extractStringValue(node, "education");
        String company = extractStringValue(node, "company");
        String nationality = extractStringValue(node, "nationality");
        
        return new Vertex(name, gender, profession, education, company, nationality);
    }
    
    /**
     * 提取字符串值
     */
    private static String extractStringValue(JsonNode node, String field) {
        return extractStringValue(node, field, null);
    }
    
    private static String extractStringValue(JsonNode node, String field, String defaultValue) {
        if (node.has(field)) {
            JsonNode value = node.get(field);
            if (value.isArray() && value.size() > 0) {
                return value.get(0).asText();
            } else if (!value.isNull()) {
                return value.asText();
            }
        }
        return defaultValue;
    }
    
    /**
     * 构建明星与作品的关系
     */
    private static void buildCelebrityWorkRelations(JsonNode objects, List<Edge> edges) {
        // 简化实现：假设路径中明星和作品相邻
        for (int i = 0; i < objects.size() - 1; i++) {
            String from = extractStringValue(objects.get(i), "name");
            String to = extractStringValue(objects.get(i + 1), "name");
            
            if (from != null && to != null) {
                edges.add(new Edge(from, to, "合作", null));
            }
        }
    }
    
    /**
     * 构建相似关系
     */
    private static void buildSimilarityRelations(JsonNode objects, List<Edge> edges) {
        // 简化实现：构建对象间的相似关系
        for (int i = 0; i < objects.size() - 1; i++) {
            String from = extractStringValue(objects.get(i), "name");
            String to = extractStringValue(objects.get(i + 1), "name");
            
            if (from != null && to != null) {
                edges.add(new Edge(from, to, "相似", null));
            }
        }
    }
    
    /**
     * 构建带相似度分数的相似关系
     */
    private static void buildSimilarityRelationsWithScore(JsonNode objects, List<Edge> edges, Double similarity) {
        // 简化实现：构建对象间的相似关系
        for (int i = 0; i < objects.size() - 1; i++) {
            String from = extractStringValue(objects.get(i), "name");
            String to = extractStringValue(objects.get(i + 1), "name");
            
            if (from != null && to != null) {
                edges.add(new Edge(from, to, "相似", null, similarity));
            }
        }
    }
    
    /**
     * 计算作品数量
     */
    private static int countWorks(Set<Vertex> vertices) {
        return (int) vertices.stream()
                .filter(v -> v.profession == null || v.profession.contains("作品"))
                .count();
    }
    
    /**
     * 数据类
     */
    private static class GraphData {
        final List<Vertex> vertices;
        final List<Edge> edges;
        final String summary;
        
        GraphData(List<Vertex> vertices, List<Edge> edges, String summary) {
            this.vertices = vertices;
            this.edges = edges;
            this.summary = summary;
        }
    }
    
    private static class Vertex {
        final String name;
        final String gender;
        final String profession;
        final String education;
        final String company;
        final String nationality;
        final String releaseDate;  // 作品发布日期
        final String description;  // 作品描述

        Vertex(String name, String gender, String profession, String education, String company, String nationality) {
            this(name, gender, profession, education, company, nationality, null, null);
        }

        Vertex(String name, String gender, String profession, String education, String company, String nationality, 
               String releaseDate, String description) {
            this.name = name;
            this.gender = gender;
            this.profession = profession;
            this.education = education;
            this.company = company;
            this.nationality = nationality;
            this.releaseDate = releaseDate;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vertex vertex = (Vertex) o;
            return Objects.equals(name, vertex.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
    
    private static class Edge {
        final String id;
        final String source;
        final String target;
        final String from;
        final String to;
        final String type;
        final String weight;
        final Double similarity;  // 添加相似度字段
        
        Edge(String from, String to, String type, String weight) {
            this(null, null, null, from, to, type, weight, null);
        }
        
        Edge(String from, String to, String type, String weight, Double similarity) {
            this(null, null, null, from, to, type, weight, similarity);
        }
        
        Edge(String id, String source, String target, String from, String to, String type, String weight, Double similarity) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.from = from;
            this.to = to;
            this.type = type;
            this.weight = weight;
            this.similarity = similarity;
        }
    }

    /**
     * 从数组格式的属性中提取第一个值
     */
    private static String extractArrayValue(JsonNode node, String field) {
        return extractArrayValue(node, field, null);
    }

    /**
     * 从数组格式的属性中提取第一个值，如果不存在则返回默认值
     */
    private static String extractArrayValue(JsonNode node, String field, String defaultValue) {
        if (node != null && node.has(field)) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode.isArray() && fieldNode.size() > 0) {
                return fieldNode.get(0).asText();
            }
        }
        return defaultValue;
    }

    /**
     * 获取属性的第一个值
     */
    private static String getFirstValue(JsonNode node, String field) {
        if (node.has(field)) {
            JsonNode value = node.get(field);
            if (value.isArray() && value.size() > 0) {
                return value.get(0).asText();
            } else {
                return value.asText();
            }
        }
        return "";
    }

    private static String createEmptyResult() throws IOException {
        ObjectNode result = mapper.createObjectNode();
        result.putArray("vertices");
        result.putArray("edges");
        result.put("summary", "未找到任何相似关系");
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    private static String buildSimilarityResult(GraphData data) throws IOException {
        ObjectNode result = mapper.createObjectNode();
        
        // 创建vertices数组
        ArrayNode verticesArray = result.putArray("vertices");
        if (data != null && data.vertices != null) {
            for (Vertex vertex : data.vertices) {
                ObjectNode vertexNode = mapper.createObjectNode();
                vertexNode.put("name", vertex.name);
                vertexNode.put("type", vertex.profession != null ? vertex.profession : "未知");
                vertexNode.put("description", vertex.description != null ? vertex.description : "");
                verticesArray.add(vertexNode);
            }
        }
        
        // 创建edges数组
        ArrayNode edgesArray = result.putArray("edges");
        if (data != null && data.edges != null) {
            for (Edge edge : data.edges) {
                ObjectNode edgeNode = mapper.createObjectNode();
                edgeNode.put("from", edge.from);
                edgeNode.put("to", edge.to);
                edgeNode.put("type", edge.type);
                if (edge.similarity != null) {
                    edgeNode.put("similarity", edge.similarity);
                }
                edgesArray.add(edgeNode);
            }
        }
        
        // 添加摘要
        result.put("summary", data != null ? data.summary : "未找到任何相似关系");
        
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }
} 
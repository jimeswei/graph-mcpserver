package com.example.graph.mcp.service;


import com.example.graph.mcp.config.GremlinQueryProperties;
import com.example.graph.mcp.util.GremlinQueryUtil;
import com.example.graph.mcp.util.JsonExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.HashSet;

@Slf4j
@Service
@ConditionalOnBean(GremlinQueryUtil.class)
public class GraphAnalysisService {

    @Autowired(required = false)
    private GremlinQueryUtil gremlinQueryUtil;

    @Autowired(required = false)
    private GraphCacheService graphCacheService;

    @Autowired(required = false)
    private GremlinQueryProperties gremlinQueryProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ====== Gremlin查询模板已全部移至 gremlin-queries.yml 配置文件 ======

    public String relationChain(String sourceName, String targetName, String threadId) throws IOException {
        log.info("开始查询关系链: {} -> {}, threadId: {}", sourceName, targetName, threadId);
        
        try {
            if (sourceName == null || sourceName.trim().isEmpty()) {
                throw new IllegalArgumentException("源人名不能为空");
            }
            if (targetName == null || targetName.trim().isEmpty()) {
                throw new IllegalArgumentException("目标人名不能为空");
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("sourceName", sourceName);
            params.put("targetName", targetName);

            String gremlinQuery = gremlinQueryProperties.getRelationChainQuery();

            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildRelationChainResult(response, sourceName, targetName);
            
            graphCacheService.saveCacheRecord(threadId, result, "relationChain");
            
            log.info("关系链查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询关系链失败", e);
            String errorResult = "{\"error\": \"查询关系链失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "relationChain");
            throw e;
        }
    }

    public String mutualFriend(List<String> names, String threadId) throws IOException {
        log.info("开始查询共同好友: {}, threadId: {}", names, threadId);
        
        try {
            validateMinimumNames(names, 2);
            
            Map<String, Object> params = new HashMap<>();
            params.put("name1", "'" + names.get(0) + "'");
            params.put("name2", "'" + names.get(1) + "'");

            // 使用增强版的共同好友查询
            String gremlinQuery = gremlinQueryProperties.getEnhancedMutualFriendsQuery();

            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildMutualFriendResult(response);
            
            graphCacheService.saveCacheRecord(threadId, result, "mutualFriend");
            
            log.info("共同好友查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询共同好友失败", e);
            String errorResult = "{\"error\": \"查询共同好友失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "mutualFriend");
            throw e;
        }
    }

    public String dreamTeam(List<String> names, String threadId) throws IOException {
        log.info("开始查询共同作品: {}, threadId: {}", names, threadId);
        
        try {
            validateMinimumNames(names, 2);
            
            if (names.size() == 2) {
                // 两人共同作品查询
                Map<String, Object> params = new HashMap<>();
                params.put("name1", "'" + names.get(0) + "'");
                params.put("name2", "'" + names.get(1) + "'");

                String gremlinQuery = gremlinQueryProperties.getCommonWorksQuery();
                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                String result = buildDreamTeamResult(response, names);
                
                graphCacheService.saveCacheRecord(threadId, result, "dreamTeam");
                log.info("共同作品查询完成并已缓存到数据库");
                return result;
            } else {
                // 多人共同作品查询
                // 多人共同作品查询简化实现
                Map<String, Object> result = new HashMap<>();
                result.put("query_type", "dream_team");
                result.put("persons", names);
                result.put("message", "多人共同作品查询暂未完整实现");
                result.put("status", "partial_implementation");
                result.put("data", new ArrayList<>());
                
                String resultJson = objectMapper.writeValueAsString(result);
                graphCacheService.saveCacheRecord(threadId, resultJson, "dreamTeam");
                log.info("多人共同作品查询完成并已缓存到数据库");
                return resultJson;
            }
        } catch (Exception e) {
            log.error("查询共同作品失败", e);
            String errorResult = "{\"error\": \"查询共同作品失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "dreamTeam");
            throw e;
        }
    }

    public String similarity(List<String> names, String relationshipType, String threadId) throws IOException {
        log.info("开始查询相似度: {} with relationship type: {}, threadId: {}", names, relationshipType, threadId);
        
        try {
            validateMinimumNames(names, 2);
            
            if (relationshipType == null || relationshipType.trim().isEmpty()) {
                relationshipType = "合作"; // 默认关系类型
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("name1", "'" + names.get(0) + "'");
            params.put("name2", "'" + names.get(1) + "'");

            String gremlinQuery = gremlinQueryProperties.getSimilarityAnalysisQuery();
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildSimilarityResult(response, names, relationshipType);
            
            graphCacheService.saveCacheRecord(threadId, result, "similarity");
            
            log.info("相似度查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询相似度失败", e);
            String errorResult = "{\"error\": \"查询相似度失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "similarity");
            throw e;
        }
    }

    public String commonAncestor(List<String> names, Integer maxDepth, String threadId) throws IOException {
        log.info("开始查询共同祖先: {} with maxDepth: {}, threadId: {}", names, maxDepth, threadId);
        
        try {
            validateMinimumNames(names, 2);
            
            int depth = (maxDepth != null && maxDepth > 0 && maxDepth <= 6) ? maxDepth : 3;
            
            Map<String, Object> params = new HashMap<>();
            params.put("name1", "'" + names.get(0) + "'");
            params.put("name2", "'" + names.get(1) + "'");
            params.put("maxDepth", String.valueOf(depth));

            String gremlinQuery = gremlinQueryProperties.getCommonAncestorQuery();
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildCommonAncestorResultNew(response, names, depth);
            
            graphCacheService.saveCacheRecord(threadId, result, "commonAncestor");
            
            log.info("共同祖先查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询共同祖先失败", e);
            String errorResult = "{\"error\": \"查询共同祖先失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "commonAncestor");
            throw e;
        }
    }

    public Mono<Map<String, Object>> findCommonAncestorByNames(String person1, String person2, String threadId) {
        log.info("开始异步查询共同祖先: {} 和 {}, threadId: {}", person1, person2, threadId);
        
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("name1", "'" + person1 + "'");
                params.put("name2", "'" + person2 + "'");
                params.put("maxDepth", "3");

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(
                    gremlinQueryProperties.getCommonAncestorQuery(), params);
                String result = buildCommonAncestorResult(response, Arrays.asList(person1, person2), 3);
                Map<String, Object> resultMap = objectMapper.readValue(result, Map.class);
                
                graphCacheService.saveCacheRecord(threadId, result, "commonAncestor");
                log.info("异步共同祖先查询完成并已缓存到数据库");
                
                return resultMap;
            } catch (Exception e) {
                try {
                    String errorResult = "{\"error\": \"异步查询共同祖先失败: " + e.getMessage() + "\"}";
                    graphCacheService.saveCacheRecord(threadId, errorResult, "commonAncestor");
                } catch (Exception ex) {
                    log.error("缓存异步查询错误失败", ex);
                }
                throw new RuntimeException(e);
            }
        });
    }

    public String queryCelebrityRelationships(List<String> names, String threadId) throws IOException {
        log.info("开始查询明星关系网络: {}, threadId: {}", names, threadId);
        
        try {
            if (names == null || names.isEmpty()) {
                throw new IllegalArgumentException("名字列表不能为空");
            }
            
            // 验证每个名字都不为空
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException(String.format("第%d个名字不能为空", i + 1));
                }
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("names", "'" + String.join("','", names) + "'");

            String gremlinQuery = gremlinQueryProperties.getCelebrityRelationshipsQuery();
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildCelebrityRelationshipsResult(response, names);
            
            graphCacheService.saveCacheRecord(threadId, result, "queryCelebrityRelationships");
            
            log.info("明星关系网络查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询明星关系网络失败", e);
            String errorResult = "{\"error\": \"查询明星关系网络失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "queryCelebrityRelationships");
            throw e;
        }
    }

    public String commonEventent(List<String> names, String threadId) throws IOException {
        log.info("开始查询共同参与的活动: {}, threadId: {}", names, threadId);
        
        try {
            validateMinimumNames(names, 2);
            
            if (names.size() != 2) {
                throw new IllegalArgumentException("共同活动查询仅支持两个人");
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("name1", "'" + names.get(0) + "'");
            params.put("name2", "'" + names.get(1) + "'");

            String gremlinQuery = gremlinQueryProperties.getCommonEventQuery();
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = buildCommonEventResult(response, names);
            
            graphCacheService.saveCacheRecord(threadId, result, "commonEventent");
            
            log.info("共同活动查询完成并已缓存到数据库");
            return result;
        } catch (Exception e) {
            log.error("查询共同活动失败", e);
            String errorResult = "{\"error\": \"查询共同活动失败: " + e.getMessage() + "\"}";
            graphCacheService.saveCacheRecord(threadId, errorResult, "commonEventent");
            throw e;
        }
    }

    // ====== 结果构建方法 ======
    
    private String buildCommonEventResult(ResponseEntity<String> response, List<String> names) throws IOException {
        try {
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Response body is null or empty for commonEventent");
                return buildEmptyVerticesEdgesResult();
            }

            String jsonResult = JsonExtractor.parseResponse(responseBody);
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.warn("JsonResult is null or empty after parsing: {}", jsonResult);
                return buildEmptyVerticesEdgesResult();
            }

            List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
            
            // 检查queryResults是否为null
            if (queryResults == null || queryResults.isEmpty()) {
                log.warn("queryResults is null or empty, returning empty result");
                return buildEmptyVerticesEdgesResult();
            }
            
            // 分离vertices和edges，使用Set去重
            List<Map<String, Object>> vertices = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> addedVertices = new HashSet<>();  // 用于去重vertices
            Set<String> addedEdges = new HashSet<>();     // 用于去重edges
            
            // 收集所有顶点信息（celebrity和event），避免重复
            for (Map<String, Object> item : queryResults) {
                if (item == null) {
                    log.warn("Encountered null item in queryResults, skipping");
                    continue;
                }
                
                // 判断是名人节点还是活动节点
                if (item.containsKey("event_type") && "event".equals(item.get("event_type"))) {
                    // 这是活动数据，作为event vertex
                    String eventName = (String) item.get("event_name");
                    String eventId = (String) item.get("event_id");
                    
                    // 使用eventId或eventName作为唯一标识避免重复
                    String uniqueKey = eventId != null ? eventId : eventName;
                    if (eventName != null && !addedVertices.contains(uniqueKey)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", eventId != null ? eventId : eventName);
                        vertex.put("label", "event");
                        vertex.put("event_name", eventName);
                        vertex.put("title", item.get("title"));
                        vertices.add(vertex);
                        addedVertices.add(uniqueKey);
                    }
                } else if (item.containsKey("name") && item.containsKey("celebrity_id")) {
                    // 这是celebrity数据，作为celebrity vertex
                    String name = (String) item.get("name");
                    String celebrityId = (String) item.get("celebrity_id");
                    
                    // 使用name作为唯一标识避免重复
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) && !celebrityId.trim().isEmpty() ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("celebrity_id", celebrityId);
                        vertex.put("education", item.get("education"));
                        vertex.put("profession", item.get("profession"));
                        vertices.add(vertex);
                        addedVertices.add(name);
                    }
                }
            }
            
            // 处理边数据，避免重复
            for (Map<String, Object> item : queryResults) {
                if (item == null) {
                    continue;
                }
                
                if (item.containsKey("from") && item.containsKey("to")) {
                    String from = (String) item.get("from");
                    String to = (String) item.get("to");
                    String edgeId = (String) item.get("id");
                    
                    // 创建边的唯一标识符，避免重复边
                    String edgeKey = from + "->" + to;
                    if (!addedEdges.contains(edgeKey)) {
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("from", from);
                        edge.put("to", to);
                        edge.put("label", item.get("label") != null ? item.get("label") : "celebrity_event");
                        if (edgeId != null) {
                            edge.put("id", edgeId);
                        }
                        edges.add(edge);
                        addedEdges.add(edgeKey);
                    }
                }
            }
            
            log.info("CommonEvent result built: {} vertices, {} edges", vertices.size(), edges.size());
            
            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("构建共同活动结果失败", e);
            return buildEmptyVerticesEdgesResult();
        }
    }
    
    private String buildCelebrityRelationshipsResult(ResponseEntity<String> response, List<String> names) throws IOException {
        try {
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Response body is null or empty for queryCelebrityRelationships");
                return buildEmptyVerticesEdgesResult();
            }

            String jsonResult = JsonExtractor.parseResponse(responseBody);
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.warn("JsonResult is null or empty after parsing: {}", jsonResult);
                return buildEmptyVerticesEdgesResult();
            }

            List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
            
            // 检查queryResults是否为null
            if (queryResults == null || queryResults.isEmpty()) {
                log.warn("queryResults is null or empty, returning empty result");
                return buildEmptyVerticesEdgesResult();
            }
            
            // 分离vertices和edges
            List<Map<String, Object>> vertices = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> addedVertices = new HashSet<>();
            
            for (Map<String, Object> item : queryResults) {
                if (item == null) {
                    log.warn("Encountered null item in queryResults, skipping");
                    continue;
                }
                
                // 处理顶点数据
                if (item.containsKey("name")) {
                    String name = (String) item.get("name");
                    String celebrityId = (String) item.get("celebrity_id");
                    String profession = (String) item.get("profession");
                    String education = (String) item.get("education");
                    
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("celebrity_id", celebrityId);
                        vertex.put("profession", profession);
                        vertex.put("education", education);
                        vertices.add(vertex);
                        addedVertices.add(name);
                    }
                }
                
                // 处理边数据
                if (item.containsKey("from") && item.containsKey("to") && item.containsKey("id")) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", item.get("from"));
                    edge.put("to", item.get("to"));
                    edge.put("label", item.get("label") != null ? item.get("label") : "celebrity_celebrity");
                    edge.put("id", item.get("id"));
                    edges.add(edge);
                }
            }
            
            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            
            log.info("Built celebrity relationships result: {} vertices, {} edges", vertices.size(), edges.size());
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("构建明星关系网络结果失败", e);
            return buildEmptyVerticesEdgesResult();
        }
    }
    
    private String buildCommonAncestorResultNew(ResponseEntity<String> response, List<String> names, int depth) throws IOException {
        try {
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Response body is null or empty for commonAncestor");
                return buildEmptyVerticesEdgesResult();
            }

            String jsonResult = JsonExtractor.parseResponse(responseBody);
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.warn("JsonResult is null or empty after parsing: {}", jsonResult);
                return buildEmptyVerticesEdgesResult();
            }

            List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
            
            // 检查queryResults是否为null
            if (queryResults == null || queryResults.isEmpty()) {
                log.warn("queryResults is null or empty, returning empty result");
                return buildEmptyVerticesEdgesResult();
            }
            
            // 分离vertices和edges
            List<Map<String, Object>> vertices = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> addedVertices = new HashSet<>();
            
            for (Map<String, Object> item : queryResults) {
                if (item == null) {
                    log.warn("Encountered null item in queryResults, skipping");
                    continue;
                }
                
                // 处理顶点数据
                if (item.containsKey("name")) {
                    String name = (String) item.get("name");
                    String celebrityId = (String) item.get("celebrity_id");
                    String profession = (String) item.get("profession");
                    
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("celebrity_id", celebrityId);
                        vertex.put("profession", profession);
                        vertices.add(vertex);
                        addedVertices.add(name);
                    }
                }
                
                // 处理边数据（如果查询返回了边信息）
                if (item.containsKey("from") && item.containsKey("to") && item.containsKey("id")) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", item.get("from"));
                    edge.put("to", item.get("to"));
                    edge.put("label", item.get("label") != null ? item.get("label") : "celebrity_celebrity");
                    edge.put("id", item.get("id"));
                    edges.add(edge);
                }
            }
            
            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("构建commonAncestor结果失败", e);
            return buildEmptyVerticesEdgesResult();
        }
    }
    
    // ====== 辅助方法 ======

    private void validateMinimumNames(List<String> names, int minCount) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("名字列表不能为空");
        }

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException(String.format("第%d个名字不能为空", i + 1));
            }
        }

        if (names.size() < minCount) {
            throw new IllegalArgumentException(String.format("需要至少 %d 个人名", minCount));
        }
    }

    private String buildMutualFriendResult(ResponseEntity<String> response) throws IOException {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return buildEmptyMutualFriendResult();
        }

        String jsonResult = JsonExtractor.parseResponse(responseBody);
        log.debug("Mutual friend raw response: {}", responseBody);
        log.debug("Mutual friend parsed JSON: {}", jsonResult);
        
        if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
            return buildEmptyMutualFriendResult();
        }

        List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
        log.debug("Query results size: {}", queryResults.size());
        
        // 分离vertices和edges
        List<Map<String, Object>> vertices = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        for (Map<String, Object> item : queryResults) {
            if (item.containsKey("name")) {
                // 这是vertex数据
                Map<String, Object> vertex = new HashMap<>();
                String celebrityId = (String) item.get("celebrity_id");
                String name = (String) item.get("name");
                vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) && !celebrityId.trim().isEmpty() ? celebrityId : name);
                vertex.put("label", "celebrity");
                vertex.put("name", name);
                vertex.put("education", item.get("education"));
                vertex.put("profession", item.get("profession"));
                vertices.add(vertex);
            } else if (item.containsKey("from") && item.containsKey("to")) {
                // 这是edge数据
                Map<String, Object> edge = new HashMap<>();
                edge.put("from", item.get("from"));
                edge.put("to", item.get("to"));
                edge.put("label", "celebrity_celebrity");
                if (item.containsKey("id")) {
                    edge.put("id", item.get("id"));
                }
                edges.add(edge);
            }
        }
        
        // 构建最终结果
        Map<String, Object> result = new HashMap<>();
        result.put("vertices", vertices);
        result.put("edges", edges);
        
        return objectMapper.writeValueAsString(result);
    }

    private String buildRelationChainResult(ResponseEntity<String> response, String sourceName, String targetName) throws IOException {
        try {
            String responseBody = response.getBody();
            log.info("Relation chain raw response: {}", responseBody);
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Response body is null or empty for relationChain");
                return buildEmptyVerticesEdgesResult();
            }

            String jsonResult = JsonExtractor.parseResponse(responseBody);
            log.info("Relation chain parsed JSON: {}", jsonResult);
            
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.warn("JsonResult is null or empty after parsing");
                return buildEmptyVerticesEdgesResult();
            }

            // 解析path查询结果
            List<Object> pathResults = objectMapper.readValue(jsonResult, List.class);
            if (pathResults == null || pathResults.isEmpty()) {
                log.warn("pathResults is null or empty");
                return buildEmptyVerticesEdgesResult();
            }
            
            // 分离vertices和edges
            List<Map<String, Object>> vertices = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();
            Set<String> addedVertices = new HashSet<>();
            Map<String, String> nameToIdMap = new HashMap<>();
            
            // 处理第一个路径结果（因为我们用了limit(1)）
            Object firstPath = pathResults.get(0);
            log.info("First path object type: {}, content: {}", firstPath.getClass().getName(), firstPath);
            
            if (firstPath instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pathMap = (Map<String, Object>) firstPath;
                
                // 检查是否有objects字段
                Object objectsObj = pathMap.get("objects");
                if (objectsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> pathObjects = (List<Map<String, Object>>) objectsObj;
                    
                    log.info("Found {} objects in path", pathObjects.size());
                    
                    // 提取vertices
                    for (Map<String, Object> obj : pathObjects) {
                        String name = (String) obj.get("name");
                        String celebrityId = (String) obj.get("celebrity_id");
                        
                        if (name != null && !addedVertices.contains(name)) {
                            Map<String, Object> vertex = new HashMap<>();
                            vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) ? celebrityId : name);
                            vertex.put("label", "celebrity");
                            vertex.put("name", name);
                            vertex.put("education", obj.get("education"));
                            vertex.put("profession", obj.get("profession"));
                            vertices.add(vertex);
                            addedVertices.add(name);
                            
                            // 收集name到ID的映射
                            if (celebrityId != null && !"N/A".equals(celebrityId)) {
                                nameToIdMap.put(name, celebrityId);
                            }
                        }
                    }
                    
                    // 创建edges（路径中相邻节点之间的关系）
                    for (int i = 0; i < pathObjects.size() - 1; i++) {
                        Map<String, Object> fromObj = pathObjects.get(i);
                        Map<String, Object> toObj = pathObjects.get(i + 1);
                        
                        String fromName = (String) fromObj.get("name");
                        String toName = (String) toObj.get("name");
                        
                        if (fromName != null && toName != null) {
                            String fromId = nameToIdMap.getOrDefault(fromName, fromName);
                            String toId = nameToIdMap.getOrDefault(toName, toName);
                            
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("from", fromId);
                            edge.put("to", toId);
                            edge.put("label", "celebrity_celebrity");
                            if (fromObj.containsKey("edge_id")) {
                                edge.put("id", fromObj.get("edge_id"));
                            } else {
                                // 生成边ID：格式为 fromId>1>1>>toId
                                edge.put("id", fromId + ">1>1>>" + toId);
                            }
                            edges.add(edge);
                        }
                    }
                }
            } else if (firstPath instanceof List) {
                // 如果直接是List格式的路径
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pathObjects = (List<Map<String, Object>>) firstPath;
                
                log.info("Path is direct list with {} objects", pathObjects.size());
                
                // 提取vertices
                for (Map<String, Object> obj : pathObjects) {
                    String name = (String) obj.get("name");
                    String celebrityId = (String) obj.get("celebrity_id");
                    
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("education", obj.get("education"));
                        vertex.put("profession", obj.get("profession"));
                        vertices.add(vertex);
                        addedVertices.add(name);
                        
                        // 收集name到ID的映射
                        if (celebrityId != null && !"N/A".equals(celebrityId)) {
                            nameToIdMap.put(name, celebrityId);
                        }
                    }
                }
                
                // 创建edges（路径中相邻节点之间的关系）
                for (int i = 0; i < pathObjects.size() - 1; i++) {
                    Map<String, Object> fromObj = pathObjects.get(i);
                    Map<String, Object> toObj = pathObjects.get(i + 1);
                    
                    String fromName = (String) fromObj.get("name");
                    String toName = (String) toObj.get("name");
                    
                    if (fromName != null && toName != null) {
                        String fromId = nameToIdMap.getOrDefault(fromName, fromName);
                        String toId = nameToIdMap.getOrDefault(toName, toName);
                        
                        Map<String, Object> edge = new HashMap<>();
                        edge.put("from", fromId);
                        edge.put("to", toId);
                        edge.put("label", "celebrity_celebrity");
                        if (fromObj.containsKey("edge_id")) {
                            edge.put("id", fromObj.get("edge_id"));
                        } else {
                            // 生成边ID：格式为 fromId>1>1>>toId
                            edge.put("id", fromId + ">1>1>>" + toId);
                        }
                        edges.add(edge);
                    }
                }
            }
            
            log.info("Built relation chain result: {} vertices, {} edges", vertices.size(), edges.size());
            
            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("构建关系链结果失败", e);
            return buildEmptyVerticesEdgesResult();
        }
    }
    
    private String buildEmptyMutualFriendResult() throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("vertices", new ArrayList<>());
        result.put("edges", new ArrayList<>());
        
        return objectMapper.writeValueAsString(result);
    }


    private String buildDreamTeamResult(ResponseEntity<String> response, List<String> names) throws IOException {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return buildEmptyVerticesEdgesResult();
        }

        String jsonResult = JsonExtractor.parseResponse(responseBody);
        if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
            return buildEmptyVerticesEdgesResult();
        }

        List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
        
        // 分离vertices和edges，使用Set去重
        List<Map<String, Object>> vertices = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> addedVertices = new HashSet<>();  // 用于去重vertices
        Set<String> addedEdges = new HashSet<>();     // 用于去重edges
        
        // 收集所有顶点信息（celebrity和work），避免重复
        for (Map<String, Object> item : queryResults) {
            if (item.containsKey("name")) {
                String name = (String) item.get("name");
                
                // 判断是名人节点还是作品节点
                if (item.containsKey("work_type") && "work".equals(item.get("work_type"))) {
                    // 这是作品数据，作为work vertex
                    
                    // 使用name作为唯一标识避免重复
                    String uniqueKey = name;
                    if (name != null && !addedVertices.contains(uniqueKey)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", name);
                        vertex.put("label", "work");
                        vertex.put("name", name);
                        vertex.put("title", item.get("title"));
                        vertices.add(vertex);
                        addedVertices.add(uniqueKey);
                    }
                } else if (item.containsKey("celebrity_id")) {
                    // 这是celebrity数据，作为celebrity vertex
                    String celebrityId = (String) item.get("celebrity_id");
                    
                    // 使用name作为唯一标识避免重复
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) && !celebrityId.trim().isEmpty() ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("education", item.get("education"));
                        vertex.put("profession", item.get("profession"));
                        vertices.add(vertex);
                        addedVertices.add(name);
                    }
                }
            }
        }
        
        // 处理边数据，避免重复
        for (Map<String, Object> item : queryResults) {
            if (item.containsKey("from") && item.containsKey("to")) {
                String from = (String) item.get("from");
                String to = (String) item.get("to");
                String edgeId = (String) item.get("id");
                
                // 创建边的唯一标识符，避免重复边
                String edgeKey = from + "->" + to;
                if (!addedEdges.contains(edgeKey)) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", from);
                    edge.put("to", to);
                    edge.put("label", item.get("label") != null ? item.get("label") : "celebrity_work");
                    if (edgeId != null) {
                        edge.put("id", edgeId);
                    }
                    edges.add(edge);
                    addedEdges.add(edgeKey);
                }
            }
        }
        
        log.info("DreamTeam result built: {} vertices, {} edges", vertices.size(), edges.size());
        
        // 构建最终结果
        Map<String, Object> result = new HashMap<>();
        result.put("vertices", vertices);
        result.put("edges", edges);
        
        return objectMapper.writeValueAsString(result);
    }

    private String buildSimilarityResult(ResponseEntity<String> response, List<String> names, String relationshipType) throws IOException {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return buildEmptyVerticesEdgesResult();
        }

        String jsonResult = JsonExtractor.parseResponse(responseBody);
        if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
            return buildEmptyVerticesEdgesResult();
        }

        try {
            List<Map<String, Object>> queryResults = objectMapper.readValue(jsonResult, List.class);
            
            // 分离vertices和edges
            List<Map<String, Object>> vertices = new ArrayList<>();
            List<Map<String, Object>> edges = new ArrayList<>();  
            Set<String> addedVertices = new HashSet<>();
            
            for (Map<String, Object> item : queryResults) {
                if (item == null) {
                    continue;
                }
                
                // 处理顶点数据 - 名人节点
                if (item.containsKey("name") && item.containsKey("celebrity_id")) {
                    String name = (String) item.get("name");
                    String celebrityId = (String) item.get("celebrity_id");
                    String profession = (String) item.get("profession");
                    String education = (String) item.get("education");
                    
                    if (name != null && !addedVertices.contains(name)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", celebrityId != null && !"N/A".equals(celebrityId) ? celebrityId : name);
                        vertex.put("label", "celebrity");
                        vertex.put("name", name);
                        vertex.put("celebrity_id", celebrityId);
                        vertex.put("profession", profession);
                        vertex.put("education", education);
                        vertices.add(vertex);
                        addedVertices.add(name);
                    }
                }
                // 处理顶点数据 - 作品节点
                else if (item.containsKey("work_type") && item.get("work_type").equals("work")) {
                    String workName = (String) item.get("name");
                    String workId = (String) item.get("work_id");
                    
                    if (workName != null && !addedVertices.contains(workName)) {
                        Map<String, Object> vertex = new HashMap<>();
                        vertex.put("id", workId != null ? workId : workName);
                        vertex.put("label", "work");
                        vertex.put("name", workName);
                        vertex.put("title", item.get("title"));
                        vertices.add(vertex);
                        addedVertices.add(workName);
                    }
                }
                
                // 处理边数据
                if (item.containsKey("from") && item.containsKey("to") && item.containsKey("id")) {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("from", item.get("from"));
                    edge.put("to", item.get("to"));
                    edge.put("label", item.get("label") != null ? item.get("label") : "celebrity_celebrity");
                    edge.put("id", item.get("id"));
                    edges.add(edge);
                }
            }
            
            // 构建最终结果
            Map<String, Object> result = new HashMap<>();
            result.put("vertices", vertices);
            result.put("edges", edges);
            
            return objectMapper.writeValueAsString(result);
            
        } catch (Exception e) {
            log.error("构建相似度结果失败", e);
            return buildEmptyVerticesEdgesResult();
        }
    }
    
    private double calculateSimilarityScore(List<?> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        
        double totalWeight = 0.0;
        for (Object item : items) {
            if (item instanceof Map) {
                Map<?, ?> itemMap = (Map<?, ?>) item;
                Object weight = itemMap.get("weight");
                if (weight instanceof Number) {
                    totalWeight += ((Number) weight).doubleValue();
                }
            }
        }
        
        return Math.min(totalWeight, 10.0); // 最高分数限制为10
    }
    
    private String buildCommonAncestorResult(ResponseEntity<String> response, List<String> names, int depth) throws IOException {
        return buildCommonAncestorResultNew(response, names, depth);
    }


    private String buildEmptyResult(String queryType, List<String> names, String message) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("query_type", queryType);
        result.put("persons", names);
        result.put("message", message);
        result.put("status", "no_result");
        result.put("data", new ArrayList<>());
        
        return objectMapper.writeValueAsString(result);
    }

    private String buildEmptyVerticesEdgesResult() throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("vertices", new ArrayList<>());
        result.put("edges", new ArrayList<>());
        
        return objectMapper.writeValueAsString(result);
    }
}
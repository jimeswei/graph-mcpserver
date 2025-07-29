package com.example.graph.mcp.handler;

import com.example.graph.mcp.model.StreamableResponse;
import com.example.graph.mcp.service.GraphServiceOptimized;
import com.example.graph.mcp.config.GraphApiConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/mcp")
public class GraphMcpHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphMcpHandler.class);
    
    @Autowired
    private GraphServiceOptimized graphService;


    @PostMapping(value = "/mutual_friend_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> mutualFriend(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        String result = graphService.mutualFriend(names);
                        sink.next(StreamableResponse.completed("mutual_friend_between_stars", result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询共同好友失败", e);
                        sink.next(StreamableResponse.error("查询共同好友失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }

    @PostMapping(value = "/dream_team_common_works", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> dreamTeam(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");

        if (names == null || names.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Flux.just(StreamableResponse.error("需要至少两个有效用户名")));
        }

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        String result = graphService.dreamTeam(names);
                        sink.next(StreamableResponse.completed("dream_team_common_works", result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询共同作品失败", e);
                        sink.next(StreamableResponse.error("查询共同作品失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }


    @PostMapping(value = "/relation_chain_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> relationChain(@RequestBody Map<String, Object> params) {
        String sourceName = (String) params.get("sourceName");
        String targetName = (String) params.get("targetName");

        return ResponseEntity.ok()
                .body(Flux.create(sink -> {
                    try {
                        String result = graphService.relationChain(sourceName, targetName);
                        // 直接使用包含 id 字段的结果
                        sink.next(StreamableResponse.completed("relation_chain_between_stars", result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询关系链失败", e);
                        sink.next(StreamableResponse.error("查询关系链失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }



    @PostMapping(value = "/similarity_between_stars", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> similarity(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        String relationshipType = (String) params.get("relationshipType");

        if (names == null || names.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "需要至少两个有效用户名"));
        }

        try {
            // 执行查询，获取包含id字段的结果
            String result = graphService.similarity(names, relationshipType);
            
            // 解析结果
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResult = mapper.readValue(result, Map.class);
            
            // 直接返回包含id字段的完整结果
            return ResponseEntity.ok(jsonResult);
            
        } catch (Exception e) {
            log.error("查询相似度失败", e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Unrecognized token")) {
                errorMessage = "查询结果格式错误，请检查查询语句和参数";
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询相似度失败: " + errorMessage));
        }
    }

    @PostMapping(value = "/most_recent_common_ancestor", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> commonAncestor(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        Integer maxDepth = (Integer) params.get("maxDepth");

        try {
            // 执行查询，获取包含id字段的结果
            String result = graphService.commonAncestor(names, maxDepth);
            
            // 解析并返回包含id字段的结果
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResult = mapper.readValue(result, Map.class);
            
            return ResponseEntity.ok(jsonResult);
            
        } catch (Exception e) {
            log.error("查询共同祖先失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "查询共同祖先失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping(value = "/query_celebrity_relationships", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> queryCelebrityRelationships(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");

        if (names == null || names.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "需要至少一个有效用户名"));
        }

        try {
            // 执行查询，获取包含id字段的结果
            String result = graphService.getNodeEdgeByNames(names);
            
            // 解析并返回包含id字段的结果
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResult = mapper.readValue(result, Map.class);
            
            return ResponseEntity.ok(jsonResult);
            
        } catch (Exception e) {
            log.error("查询明星关系网络失败", e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Unrecognized token")) {
                errorMessage = "查询结果格式错误，请检查查询语句和参数";
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询明星关系网络失败: " + errorMessage));
        }
    }
}
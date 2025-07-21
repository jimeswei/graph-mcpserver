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
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/mcp")
public class GraphMcpHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphMcpHandler.class);
    
    @Autowired
    private GraphServiceOptimized graphService;

    @PostMapping(value = "/relation_chain_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> relationChain(@RequestBody Map<String, Object> params) {
        String sourceName = (String) params.get("sourceName");
        String targetName = (String) params.get("targetName");

        return ResponseEntity.ok()
                .body(Flux.create(sink -> {
                    try {
                        String result = graphService.relationChain(sourceName, targetName);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (IOException e) {
                        log.error("查询关系链失败", e);
                        sink.next(StreamableResponse.error("查询关系链失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }

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
                    } catch (IOException e) {
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
                        sink.next(StreamableResponse.started("开始查询共同作品"));
                        String result = graphService.dreamTeam(names);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询共同作品失败", e);
                        sink.next(StreamableResponse.error("查询共同作品失败: " + e.getMessage()));
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
            // 执行查询，获取原始结果
            String result = graphService.similarity(names, relationshipType);
            
            // 解析并提取简化的数据格式
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonResult = mapper.readValue(result, Map.class);
            
            // 检查查询是否成功
            if (jsonResult.get("status").equals(200)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) jsonResult.get("data");
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonView = (Map<String, Object>) data.get("json_view");
                @SuppressWarnings("unchecked")
                List<Object> resultData = (List<Object>) jsonView.get("data");
                
                // 直接返回简化的数据数组
                return ResponseEntity.ok(resultData);
            } else {
                // 查询失败时返回错误信息
                return ResponseEntity.badRequest()
                        .body(Map.of("error", jsonResult.get("message")));
            }
            
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
    public ResponseEntity<Mono<Map<String, Object>>> commonAncestor(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        Integer maxDepth = (Integer) params.get("maxDepth");

        // 如果是两个人，使用新的优化查询
        if (names != null && names.size() == 2) {
            try {
                Mono<Map<String, Object>> result = graphService.findCommonAncestorByNames(names.get(0), names.get(1));
                return ResponseEntity.ok(result.map(data -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "COMPLETED");
                    response.put("progress", 100);
                    response.put("data", data);
                    response.put("error", null);
                    response.put("message", "查询共同祖先完成");
                    return response;
                }));
            } catch (Exception e) {
                log.error("查询共同祖先失败", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("progress", 0);
                errorResponse.put("data", null);
                errorResponse.put("error", e.getMessage());
                errorResponse.put("message", "查询共同祖先失败");
                return ResponseEntity.badRequest().body(Mono.just(errorResponse));
            }
        }

        // 否则使用原有的多人查询逻辑
        try {
            String result = graphService.commonAncestor(names, maxDepth);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "COMPLETED");
            response.put("progress", 100);
            response.put("data", result);
            response.put("error", null);
            response.put("message", "查询共同祖先完成");

            return ResponseEntity.ok(Mono.just(response));
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("progress", 0);
            errorResponse.put("data", null);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "查询共同祖先失败");

            return ResponseEntity.badRequest().body(Mono.just(errorResponse));
        }
    }
}
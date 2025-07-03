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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class GraphMcpHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphMcpHandler.class);
    private final GraphServiceOptimized graphService;

    @PostMapping(value = "/relation_chain_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> relationChain(@RequestBody Map<String, Object> params) {
        String sourceName = (String) params.get("sourceName");
        String targetName = (String) params.get("targetName");

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        sink.next(StreamableResponse.started("开始查询关系链"));
                        String result = graphService.relationChain(sourceName, targetName);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
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
                        sink.next(StreamableResponse.started("开始查询共同好友"));
                        String result = graphService.mutualFriend(names);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
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
                        sink.next(StreamableResponse.error(e.getMessage()));
                        sink.complete();
                    }
                }));
    }

    @PostMapping(value = "/similarity_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> similarity(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        String relationshipType = (String) params.get("relationshipType");

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        sink.next(StreamableResponse.started("开始查询相似度"));
                        String result = graphService.similarity(names, relationshipType);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
                    }
                }));
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
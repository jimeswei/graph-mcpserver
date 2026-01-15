package com.example.graph.mcp.handler;

import com.example.graph.mcp.model.StreamableResponse;
import com.example.graph.mcp.service.GraphServiceOptimized;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * MCP图分析接口控制器
 * 提供明星关系图谱的查询分析功能
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class GraphMcpHandler {

    private static final Logger log = LoggerFactory.getLogger(GraphMcpHandler.class);

    private final GraphServiceOptimized graphService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 查询两个明星之间的共同好友
     */
    @PostMapping(value = "/mutual_friend_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> mutualFriend(@RequestBody Map<String, Object> params) {
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
                        String result = graphService.mutualFriend(names);
                        sink.next(StreamableResponse.completed("mutual_friend_between_stars", result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询共同好友失败: {}", e.getMessage());
                        sink.next(StreamableResponse.error("查询共同好友失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }

    /**
     * 查询多个明星共同参演的作品
     */
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
                        log.error("查询共同作品失败: {}", e.getMessage());
                        sink.next(StreamableResponse.error("查询共同作品失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }


    /**
     * 查询两个明星之间的关系链路径
     */
    @PostMapping(value = "/relation_chain_between_stars", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<Flux<StreamableResponse>> relationChain(@RequestBody Map<String, Object> params) {
        String sourceName = (String) params.get("sourceName");
        String targetName = (String) params.get("targetName");

        if (sourceName == null || sourceName.trim().isEmpty() ||
                targetName == null || targetName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Flux.just(StreamableResponse.error("源名字和目标名字都不能为空")));
        }

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        String result = graphService.relationChain(sourceName, targetName);
                        sink.next(StreamableResponse.completed("relation_chain_between_stars", result));
                        sink.complete();
                    } catch (Exception e) {
                        log.error("查询关系链失败: {}", e.getMessage());
                        sink.next(StreamableResponse.error("查询关系链失败: " + e.getMessage()));
                        sink.complete();
                    }
                }));
    }



    /**
     * 查询多个明星之间的相似度分析
     */
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
            String result = graphService.similarity(names, relationshipType);
            Map<String, Object> jsonResult = objectMapper.readValue(result, Map.class);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("查询相似度失败: {}", e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Unrecognized token")) {
                errorMessage = "查询结果格式错误，请检查查询语句和参数";
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询相似度失败: " + errorMessage));
        }
    }

    /**
     * 查询多个明星之间的最近共同祖先
     */
    @PostMapping(value = "/most_recent_common_ancestor", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> commonAncestor(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        Integer maxDepth = (Integer) params.get("maxDepth");

        if (names == null || names.size() < 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "需要至少两个有效用户名"));
        }

        try {
            String result = graphService.commonAncestor(names, maxDepth);
            Map<String, Object> jsonResult = objectMapper.readValue(result, Map.class);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("查询共同祖先失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询共同祖先失败: " + e.getMessage()));
        }
    }

    /**
     * 查询明星的第一圈好友关系网络
     */
    @PostMapping(value = "/query_celebrity_relationships", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> queryCelebrityRelationships(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");

        if (names == null || names.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "需要至少一个有效用户名"));
        }

        try {
            String result = graphService.getNodeEdgeByNames(names);
            Map<String, Object> jsonResult = objectMapper.readValue(result, Map.class);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("查询明星关系网络失败: {}", e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Unrecognized token")) {
                errorMessage = "查询结果格式错误，请检查查询语句和参数";
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询明星关系网络失败: " + errorMessage));
        }
    }

    /**
     * 查询两个明星共同参与的活动
     */
    @PostMapping(value = "/recent_common_celebrity_event", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> recentCommonCelebrityEvent(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");

        if (names == null || names.size() != 2) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "需要恰好两个有效用户名"));
        }

        try {
            String result = graphService.commonEventent(names);
            Map<String, Object> jsonResult = objectMapper.readValue(result, Map.class);
            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            log.error("查询共同活动失败: {}", e.getMessage());
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Unrecognized token")) {
                errorMessage = "查询结果格式错误，请检查查询语句和参数";
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "查询共同活动失败: " + errorMessage));
        }
    }

}
package com.example.graph.mcp.handler;

import com.example.graph.mcp.model.StreamableResponse;
import com.example.graph.mcp.service.GraphServiceOptimized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.text.StringSubstitutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class GraphMcpHandler {

    private static final String BASE_URL = "http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

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

        return ResponseEntity.ok()
                .header("X-Streamable-Status", "STARTED")
                .body(Flux.create(sink -> {
                    try {
                        sink.next(StreamableResponse.started("开始查询共同作品"));
                        String result = graphService.dreamTeam(names);
                        sink.next(StreamableResponse.completed(result));
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
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
    public ResponseEntity<Map<String, Object>> commonAncestor(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) params.get("names");
        Integer maxDepth = (Integer) params.get("maxDepth");

        try {
            String result = graphService.commonAncestor(names, maxDepth);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "COMPLETED");
            response.put("progress", 100);
            response.put("data", result);
            response.put("error", null);
            response.put("message", "查询共同祖先完成");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("progress", 0);
            errorResponse.put("data", null);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "查询共同祖先失败");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private ResponseEntity<String> executeGremlinRequest(String query, Map<String, Object> params)
            throws JsonProcessingException {
        StringSubstitutor substitutor = new StringSubstitutor(params);
        String gremlin = substitutor.replace(query);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", gremlin);
        String json = objectMapper.writeValueAsString(requestBody);
        log.info("gremlin: {}", json);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> content = restTemplate.exchange(
                BASE_URL,
                HttpMethod.POST,
                entity,
                String.class);
        return content;
    }

    private void validateInput(List<String> names) {
        if (names == null || names.size() < 2) {
            throw new IllegalArgumentException("需要两个有效用户名");
        }
    }

}
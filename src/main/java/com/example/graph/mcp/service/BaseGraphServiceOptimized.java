package com.example.graph.mcp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseGraphServiceOptimized {
    private static final String BASE_URL = "http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query";
    private static final RestClient restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected ResponseEntity<String> executeGremlinRequest(String query, Map<String, Object> params) throws JsonProcessingException {
        StringSubstitutor substitutor = new StringSubstitutor(params);
        String gremlin = substitutor.replace(query);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("content", gremlin);  // 若查询使用 :names
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);
        log.info("gremlin: {}", json);
        ResponseEntity<String> content = restClient.post()
                .body(json)  // 关键修改：发送完整的JSON体
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE) // 显式设置头
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(String.class);
        return content;
    }

    protected void validateInput(List<String> names) {
        if (names == null || names.size() < 2) {
            throw new IllegalArgumentException("需要两个有效用户名");
        }
    }

}

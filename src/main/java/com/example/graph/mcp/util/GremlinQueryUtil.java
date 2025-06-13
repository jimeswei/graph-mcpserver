package com.example.graph.mcp.util;

import com.example.graph.mcp.config.GraphApiConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GremlinQueryUtil {
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final GraphApiConfig graphApiConfig;

    public GremlinQueryUtil(GraphApiConfig graphApiConfig) {
        this.graphApiConfig = graphApiConfig;
    }

    public ResponseEntity<String> executeGremlinRequest(String query, Map<String, Object> params)
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

        return restTemplate.exchange(
                graphApiConfig.getBaseUrl(),
                HttpMethod.POST,
                entity,
                String.class);
    }

    public static void validateInput(List<String> names) {
        if (names == null || names.size() < 2) {
            throw new IllegalArgumentException("需要两个有效用户名");
        }
    }
}
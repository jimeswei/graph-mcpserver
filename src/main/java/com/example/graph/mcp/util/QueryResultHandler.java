package com.example.graph.mcp.util;

import com.example.graph.core.Edge;
import com.example.graph.core.Node;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static com.example.graph.mcp.constant.GraphConstants.*;

public class QueryResultHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String processGraphQueryResult(ResponseEntity<String> response) throws IOException {
        String relationsJson = JsonExtractor.parseGraphView(response.getBody());
        String detailsJson = JsonExtractor.parseResponse(response.getBody());
        return JsonExtractor.buildFinalResponseSimple(detailsJson, relationsJson);
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
                    node.setAttribute(CELEBRITY_ID_PROPERTY, nodeObj.path(CELEBRITY_ID_PROPERTY).asText(""));
                    node.setAttribute(PROFESSION_PROPERTY, nodeObj.path(PROFESSION_PROPERTY).asText(""));
                    node.setAttribute(COMPANY_PROPERTY, nodeObj.path(COMPANY_PROPERTY).asText(""));
                    node.setAttribute(NATIONALITY_PROPERTY, nodeObj.path(NATIONALITY_PROPERTY).asText(""));
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

    /**
     * 截断返回结果，最大保留55000字符
     */
    public static String truncateResult(String result) {
        if (result != null && result.length() > 55000) {
            return result.substring(0, 55000);
        }
        return result;
    }
}
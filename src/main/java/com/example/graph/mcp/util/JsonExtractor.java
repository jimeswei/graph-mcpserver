package com.example.graph.mcp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonExtractor {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String extractObjectsToJson(String jsonInput) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(jsonInput);
        List<ObjectNode> resultList = new ArrayList<>();

        // 处理两种情况：输入是数组或单个对象
        if (rootNode.isArray()) {
            // 情况1：输入是数组
            ArrayNode arrayNode = (ArrayNode) rootNode;
            arrayNode.forEach(item -> processItem(item, resultList));
        } else if (rootNode.isObject()) {
            // 情况2：输入是单个对象
            processItem(rootNode, resultList);
        }

        // 去重和输出逻辑保持不变
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultList);
    }

    private static void processItem(JsonNode item, List<ObjectNode> resultList) {
        JsonNode objectsNode = item.get("objects");
        if (objectsNode != null && objectsNode.isArray()) {
            ArrayNode objectsArray = (ArrayNode) objectsNode;
            objectsArray.forEach(objNode -> {
                ObjectNode cleanNode = mapper.createObjectNode();
                objNode.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();
                    if (value.isArray() && value.size() == 1) {
                        cleanNode.set(entry.getKey(), value.get(0));
                    } else {
                        cleanNode.set(entry.getKey(), value);
                    }
                });
                resultList.add(cleanNode);
            });
        }
    }

    public static String buildFinalResponseSimple(String detailsJson, String relationsJson) {
        return String.format("""
                {
                  "details": %s,
                  "relations": %s
                }""", detailsJson, relationsJson);
    }

    public static String parseResponse(String json) throws IOException {
        JsonNode rootNode = mapper.readTree(json);
        JsonNode dataNode = rootNode.path("data").path("json_view").path("data");
        return dataNode.toString(); // 返回指定节点的JSON字符串
    }

    public static String parseGraphView(String json) throws IOException {
        JsonNode rootNode = mapper.readTree(json);
        JsonNode dataNode = rootNode.path("data").path("graph_view").path("edges");
        return dataNode.toString(); // 返回指定节点的JSON字符串
    }

}
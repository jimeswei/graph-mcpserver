package com.example.graph.mcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.*;

/**
 * 结果优化器 - 专门处理图查询结果的优化和简化
 * 1. 移除不必要的字段（如celebrity_id）
 * 2. 优化关系表达为人名格式: [周杰伦] -->|妻子| [昆凌]
 * 3. 简化数据结构，提高可读性
 */
public class ResultOptimizer {

    private static final ObjectMapper mapper = new ObjectMapper();
    
    // 不需要返回给调用者的字段
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
        "celebrity_id", "id", "outV", "inV", "T.id", "T.label", 
        "vertex_id", "edge_id", "source_id", "target_id"
    );
    
    // 关系类型映射
    private static final Map<String, String> RELATIONSHIP_LABELS = Map.of(
        "celebrity_celebrity", "好友",
        "celebrity_work", "参演",
        "celebrity_event", "参与",
        "spouse", "配偶",
        "colleague", "同事",
        "cooperation", "合作"
    );

    /**
     * 优化图查询结果
     */
    public static String optimizeGraphResult(String jsonResult) {
        try {
            if (jsonResult == null || jsonResult.trim().isEmpty()) {
                return createEmptyResult();
            }

            JsonNode root = mapper.readTree(jsonResult);
            
            // 检查是否是包装在response对象中的数据
            if (root.has("data")) {
                String dataStr = root.get("data").asText();
                // 解析data字段中的JSON字符串
                root = mapper.readTree(dataStr);
            }
            
            // 根据不同的数据结构进行优化
            String optimizedResult;
            if (root.has("details") && root.has("relations")) {
                optimizedResult = optimizeDetailedResult(root);
            } else if (root.has("entities") && root.has("relationships")) {
                optimizedResult = optimizeDetailedResult(root);
            } else if (root.isArray()) {
                optimizedResult = optimizeArrayResult(root);
            } else {
                optimizedResult = optimizeGenericResult(root);
            }

            // 如果原始数据是包装在response对象中的，需要保持相同的结构
            if (mapper.readTree(jsonResult).has("data")) {
                ObjectNode response = mapper.createObjectNode();
                response.put("status", "COMPLETED");
                response.put("progress", 100);
                response.put("data", optimizedResult);
                response.putNull("error");
                response.putNull("message");
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            }
            
            return optimizedResult;
            
        } catch (Exception e) {
            System.err.println("结果优化失败: " + e.getMessage());
            return jsonResult; // 失败时返回原始结果
        }
    }

    /**
     * 优化详细结果（包含details/entities和relations/relationships）
     */
    private static String optimizeDetailedResult(JsonNode root) throws IOException {
        ObjectNode result = mapper.createObjectNode();
        
        // 创建ID到名称的映射
        Map<String, String> idToNameMap = new HashMap<>();
        
        // 确定使用哪个字段名
        JsonNode details = root.get("details");
        if (details == null) {
            details = root.get("entities");
        }
        
        // 从details中收集ID到名称的映射
        if (details != null && details.isArray()) {
            for (JsonNode entity : details) {
                if (entity.has("objects") && entity.get("objects").isArray()) {
                    for (JsonNode obj : entity.get("objects")) {
                        if (obj.has("properties")) {
                            JsonNode props = obj.get("properties");
                            String id = obj.get("id").asText();
                            if (props.has("name")) {
                                String name = props.get("name").asText();
                                // 如果是明星，添加到映射中
                                if (obj.has("label") && obj.get("label").asText().equals("celebrity")) {
                                    idToNameMap.put(id, name);
                                }
                            } else if (props.has("title")) {
                                String title = props.get("title").asText();
                                // 如果是作品，添加到映射中
                                if (obj.has("label") && obj.get("label").asText().equals("work")) {
                                    idToNameMap.put(id, title);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 保持原始的details结构
        result.set("details", details);
        
        // 优化relations部分，使用收集到的ID到名称映射
        JsonNode relations = root.get("relations");
        if (relations == null) {
            relations = root.get("relationships");
        }
        
        if (relations != null && relations.isArray()) {
            ArrayNode optimizedRelations = mapper.createArrayNode();
            for (JsonNode relation : relations) {
                ObjectNode optimizedRelation = mapper.createObjectNode();
                
                String sourceId = relation.has("source") ? relation.get("source").asText() : "";
                String targetId = relation.has("target") ? relation.get("target").asText() : "";
                
                // 获取关系类型
                String type = "作品";
                if (relation.has("properties") && relation.get("properties").has("e_type")) {
                    type = relation.get("properties").get("e_type").asText();
                }
                
                // 使用名称替代ID
                String sourceName = idToNameMap.getOrDefault(sourceId, sourceId);
                String targetName = idToNameMap.getOrDefault(targetId, targetId);
                
                // 生成关系表达式
                String relationExpression = String.format("[%s] -->|%s| [%s]", 
                    sourceName, type, targetName);
                
                // 设置优化后的关系属性
                optimizedRelation.put("type", type);
                optimizedRelation.put("relationship", relationExpression);
                optimizedRelation.put("source", sourceName);
                optimizedRelation.put("target", targetName);
                
                optimizedRelations.add(optimizedRelation);
            }
            result.set("relations", optimizedRelations);
        }
        
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /**
     * 优化数组结果
     */
    private static String optimizeArrayResult(JsonNode arrayNode) throws IOException {
        ArrayNode optimizedArray = mapper.createArrayNode();
        
        for (JsonNode item : arrayNode) {
            ObjectNode optimizedItem = optimizeEntity(item);
            if (optimizedItem.size() > 0) {
                optimizedArray.add(optimizedItem);
            }
        }
        
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(optimizedArray);
    }

    /**
     * 优化通用结果
     */
    private static String optimizeGenericResult(JsonNode root) throws IOException {
        ObjectNode optimized = optimizeEntity(root);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(optimized);
    }

    /**
     * 优化实体列表
     */
    private static ArrayNode optimizeEntities(JsonNode entitiesNode) {
        ArrayNode optimizedEntities = mapper.createArrayNode();
        
        if (entitiesNode.isArray()) {
            for (JsonNode entity : entitiesNode) {
                ObjectNode optimizedEntity = optimizeEntity(entity);
                if (optimizedEntity.size() > 0) {
                    optimizedEntities.add(optimizedEntity);
                }
            }
        } else if (entitiesNode.isObject()) {
            ObjectNode optimizedEntity = optimizeEntity(entitiesNode);
            if (optimizedEntity.size() > 0) {
                optimizedEntities.add(optimizedEntity);
            }
        }
        
        return optimizedEntities;
    }

    /**
     * 优化关系列表
     */
    private static ArrayNode optimizeRelationships(JsonNode relationsNode) {
        ArrayNode optimizedRelations = mapper.createArrayNode();
        
        if (relationsNode.isArray()) {
            for (JsonNode relation : relationsNode) {
                ObjectNode optimizedRelation = optimizeRelationship(relation);
                if (optimizedRelation.size() > 0) {
                    optimizedRelations.add(optimizedRelation);
                }
            }
        }
        
        return optimizedRelations;
    }

    /**
     * 优化单个实体
     */
    private static ObjectNode optimizeEntity(JsonNode entity) {
        ObjectNode optimized = mapper.createObjectNode();
        
        entity.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode fieldValue = entry.getValue();
            
            // 跳过不需要的字段
            if (EXCLUDED_FIELDS.contains(fieldName) || fieldName.toLowerCase().contains("_id")) {
                return;
            }
            
            // 处理嵌套对象
            if (fieldValue.isObject()) {
                ObjectNode nestedOptimized = optimizeEntity(fieldValue);
                if (nestedOptimized.size() > 0) {
                    optimized.set(fieldName, nestedOptimized);
                }
            } else if (fieldValue.isArray()) {
                // 优化数组内容
                ArrayNode optimizedArray = mapper.createArrayNode();
                for (JsonNode item : fieldValue) {
                    if (item.isObject()) {
                        ObjectNode optimizedItem = optimizeEntity(item);
                        if (optimizedItem.size() > 0) {
                            optimizedArray.add(optimizedItem);
                        }
                    } else if (!item.isNull() && !item.asText().isEmpty()) {
                        optimizedArray.add(item);
                    }
                }
                if (optimizedArray.size() > 0) {
                    optimized.set(fieldName, optimizedArray);
                }
            } else if (!fieldValue.isNull() && !fieldValue.asText().isEmpty()) {
                optimized.set(fieldName, fieldValue);
            }
        });
        
        return optimized;
    }

    /**
     * 优化单个关系 - 转换为人名关系表达
     */
    private static ObjectNode optimizeRelationship(JsonNode relation) {
        ObjectNode optimized = mapper.createObjectNode();
        
        // 获取关系类型
        String relationshipType = extractRelationshipType(relation);
        
        // 获取源节点和目标节点的名称或标题
        String sourceId = relation.has("source") ? relation.get("source").asText() : "";
        String targetId = relation.has("target") ? relation.get("target").asText() : "";
        
        // 生成关系表达式，使用名称替代ID
        String relationExpression = String.format("[%s] -->|%s| [%s]", 
            sourceId, relationshipType, targetId);
            
        optimized.put("type", relationshipType);
        optimized.put("relationship", relationExpression);
        
        return optimized;
    }

    /**
     * 提取名称
     */
    private static String extractName(JsonNode node, String prefix) {
        // 尝试多种可能的字段名
        String[] possibleFields = {
            prefix + "Name", "name", prefix + "_name", 
            prefix, "vertex_name", "celebrity_name"
        };
        
        for (String field : possibleFields) {
            JsonNode nameNode = node.get(field);
            if (nameNode != null && !nameNode.isNull()) {
                String name = nameNode.asText().trim();
                if (!name.isEmpty() && !EXCLUDED_FIELDS.contains(name)) {
                    return name;
                }
            }
        }
        
        return null;
    }

    /**
     * 提取关系类型
     */
    private static String extractRelationshipType(JsonNode relation) {
        JsonNode typeNode = relation.get("label");
        if (typeNode == null) {
            typeNode = relation.get("type");
        }
        if (typeNode == null) {
            typeNode = relation.get("relationship_type");
        }
        if (typeNode == null) {
            typeNode = relation.get("e_type");
        }
        
        if (typeNode != null && !typeNode.isNull()) {
            String type = typeNode.asText();
            return RELATIONSHIP_LABELS.getOrDefault(type, type);
        }
        
        return "关联";
    }

    /**
     * 创建空结果
     */
    private static String createEmptyResult() {
        try {
            ObjectNode empty = mapper.createObjectNode();
            empty.put("message", "未找到相关数据");
            empty.set("entities", mapper.createArrayNode());
            empty.set("relationships", mapper.createArrayNode());
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(empty);
        } catch (Exception e) {
            return "{\"message\":\"未找到相关数据\"}";
        }
    }

    /**
     * 简化路径结果（用于关系链查询）
     */
    public static String optimizePathResult(String pathResult) {
        try {
            JsonNode root = mapper.readTree(pathResult);
            if (root.isArray() && root.size() > 0) {
                ArrayNode optimizedPaths = mapper.createArrayNode();
                
                for (JsonNode path : root) {
                    if (path.has("objects")) {
                        JsonNode objects = path.get("objects");
                        if (objects.isArray() && objects.size() > 0) {
                            String pathExpression = buildPathExpression(objects);
                            if (!pathExpression.isEmpty()) {
                                ObjectNode pathNode = mapper.createObjectNode();
                                pathNode.put("path", pathExpression);
                                pathNode.put("length", objects.size());
                                optimizedPaths.add(pathNode);
                            }
                        }
                    }
                }
                
                ObjectNode result = mapper.createObjectNode();
                result.set("relationChains", optimizedPaths);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            }
        } catch (Exception e) {
            System.err.println("路径结果优化失败: " + e.getMessage());
        }
        
        return pathResult;
    }

    /**
     * 构建路径表达式
     */
    private static String buildPathExpression(JsonNode objects) {
        List<String> names = new ArrayList<>();
        
        for (JsonNode obj : objects) {
            String name = null;
            if (obj.has("name")) {
                JsonNode nameNode = obj.get("name");
                if (nameNode.isArray() && nameNode.size() > 0) {
                    name = nameNode.get(0).asText();
                } else {
                    name = nameNode.asText();
                }
            }
            
            if (name != null && !name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }
        
        if (names.size() >= 2) {
            return String.join(" --> ", names);
        }
        
        return "";
    }
} 
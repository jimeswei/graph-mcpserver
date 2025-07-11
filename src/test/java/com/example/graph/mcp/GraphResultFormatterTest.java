package com.example.graph.mcp;

import com.example.graph.mcp.util.GraphResultFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GraphResultFormatterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSimilarityFormatWithData() throws Exception {
        // 模拟完整的Gremlin响应结构
        String mockResponse = """
            {
                "data": {
                    "json_view": {
                        "data": [
                            {
                                "person1": {
                                    "name": ["刘德华"],
                                    "profession": ["演员、歌手、制片人、作词人"],
                                    "gender": ["男"],
                                    "company": ["东亚唱片、映艺娱乐"],
                                    "nationality": ["中国"],
                                    "education": ["可立中学、第十期无线艺员训练班"]
                                },
                                "person2": {
                                    "name": ["周杰伦"],
                                    "profession": ["演员、歌手、制片人、作词人"],
                                    "gender": ["男"],
                                    "company": ["东亚唱片、映艺娱乐"],
                                    "nationality": ["中国"],
                                    "education": ["可立中学、第十期无线艺员训练班"]
                                },
                                "similarity": 0.5
                            }
                        ]
                    }
                }
            }
            """;

        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        String result = GraphResultFormatter.formatSimilarity(response);
        
        // 验证返回的JSON结构
        JsonNode resultNode = mapper.readTree(result);
        
        assertNotNull(resultNode, "Result should not be null");
        assertTrue(resultNode.has("vertices"), "Should contain vertices array");
        assertTrue(resultNode.has("edges"), "Should contain edges array");
        assertTrue(resultNode.has("vertexCount"), "Should contain vertexCount");
        assertTrue(resultNode.has("edgeCount"), "Should contain edgeCount");
        assertTrue(resultNode.has("summary"), "Should contain summary");
        
        // 验证vertices结构
        JsonNode vertices = resultNode.get("vertices");
        assertTrue(vertices.isArray(), "Vertices should be an array");
        assertEquals(2, vertices.size(), "Should have 2 vertices");
        
        JsonNode vertex1 = vertices.get(0);
        assertTrue(vertex1.has("name"), "Vertex should have name");
        assertTrue(vertex1.has("profession"), "Vertex should have profession");
        assertTrue(vertex1.has("gender"), "Vertex should have gender");
        assertTrue(vertex1.has("company"), "Vertex should have company");
        assertTrue(vertex1.has("nationality"), "Vertex should have nationality");
        assertTrue(vertex1.has("education"), "Vertex should have education");
        
        assertEquals("刘德华", vertex1.get("name").asText());
        assertEquals("男", vertex1.get("gender").asText());
        
        // 验证edges结构
        JsonNode edges = resultNode.get("edges");
        assertTrue(edges.isArray(), "Edges should be an array");
        assertEquals(1, edges.size(), "Should have 1 edge");
        
        JsonNode edge = edges.get(0);
        assertTrue(edge.has("label"), "Edge should have label");
        assertTrue(edge.has("properties"), "Edge should have properties");
        
        assertEquals("celebrity_celebrity", edge.get("label").asText());
        
        JsonNode properties = edge.get("properties");
        assertTrue(properties.has("from"), "Properties should have from");
        assertTrue(properties.has("to"), "Properties should have to");
        assertTrue(properties.has("e_type"), "Properties should have e_type");
        assertTrue(properties.has("similarity"), "Properties should have similarity");
        
        assertEquals("刘德华", properties.get("from").asText());
        assertEquals("周杰伦", properties.get("to").asText());
        assertEquals("相似", properties.get("e_type").asText());
        assertEquals(0.5, properties.get("similarity").asDouble());
        
        // 验证统计信息
        assertEquals(2, resultNode.get("vertexCount").asInt());
        assertEquals(1, resultNode.get("edgeCount").asInt());
        assertEquals("找到 1 个相似关系，涉及 2 个明星", resultNode.get("summary").asText());
        
        System.out.println("测试通过！格式化后的JSON:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultNode));
    }
    
    @Test
    void testSimilarityFormatEmpty() throws Exception {
        String mockResponse = """
            {
                "data": {
                    "json_view": {
                        "data": []
                    }
                }
            }
            """;
        
        ResponseEntity<String> response = ResponseEntity.ok(mockResponse);
        String result = GraphResultFormatter.formatSimilarity(response);
        
        JsonNode resultNode = mapper.readTree(result);
        
        assertEquals(0, resultNode.get("vertices").size());
        assertEquals(0, resultNode.get("edges").size());
        assertEquals(0, resultNode.get("vertexCount").asInt());
        assertEquals(0, resultNode.get("edgeCount").asInt());
        assertEquals("找到 0 个相似关系，涉及 0 个明星", resultNode.get("summary").asText());
    }
}
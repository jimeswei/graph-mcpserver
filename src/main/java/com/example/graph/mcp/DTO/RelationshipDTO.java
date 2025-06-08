package com.example.graph.mcp.DTO;

import lombok.Data;

import java.util.Map;

@Data
public class RelationshipDTO {
    private String sourceId;
    private String targetId;
    private String eType;
    private Map<String, String> properties;
}
// 在解析时直接映射为DTO对象
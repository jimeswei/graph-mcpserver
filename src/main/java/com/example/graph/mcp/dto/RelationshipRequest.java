package com.example.graph.mcp.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RelationshipRequest {
    private String sourceId;
    private String targetId;
    private String eType;
    private Map<String, String> properties;
}
// 在解析时直接映射为DTO对象
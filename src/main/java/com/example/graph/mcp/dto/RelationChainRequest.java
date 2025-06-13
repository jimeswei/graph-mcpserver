package com.example.graph.mcp.dto;

import lombok.Data;

@Data
public class RelationChainRequest {
    private String sourceName;
    private String targetName;
}
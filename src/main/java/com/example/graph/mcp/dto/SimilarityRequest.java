package com.example.graph.mcp.dto;

import lombok.Data;
import java.util.List;

@Data
public class SimilarityRequest {
    private List<String> names;
    private String relationshipType;
}
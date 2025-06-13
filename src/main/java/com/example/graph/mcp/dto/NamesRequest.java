package com.example.graph.mcp.dto;

import lombok.Data;
import java.util.List;

@Data
public class NamesRequest {
    private List<String> names;
}
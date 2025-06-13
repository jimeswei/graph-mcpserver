package com.example.graph.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphApiConfig {
    @Value("${graph.api.base-url:http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query}")
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }
}
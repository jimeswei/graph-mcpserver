package com.example.graph.mcp.config;

import com.example.graph.mcp.service.GraphServiceOptimized;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP配置类
 * 配置Spring AI的Tool回调提供器
 */
@Configuration
public class MCPConfig {

    /**
     * 注册MCP工具回调提供器
     * 将GraphServiceOptimized中的@Tool注解方法注册为MCP工具
     */
    @Bean
    public ToolCallbackProvider taskTools(GraphServiceOptimized graphServiceOptimized) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(graphServiceOptimized)
                .build();
    }
}
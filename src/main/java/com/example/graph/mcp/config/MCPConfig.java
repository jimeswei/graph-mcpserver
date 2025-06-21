package com.example.graph.mcp.config;

import com.example.graph.mcp.service.GraphServiceOptimized;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPConfig {

    // @Bean
    // public ToolCallbackProvider taskTools(GraphServiceOptimized
    // graphServiceOptimized, AlgorithmService algorithmService) {
    // return
    // MethodToolCallbackProvider.builder().toolObjects(graphServiceOptimized,
    // algorithmService).build();
    // }

     @Bean
     public ToolCallbackProvider taskTools(GraphServiceOptimized graphServiceOptimized) {
          return MethodToolCallbackProvider.builder().toolObjects(graphServiceOptimized).build();
     }




    /* @Bean
     public List<ToolCallback> tools(GraphServiceOptimized graphServiceOptimized)
     {
      return List.of(ToolCallbacks.from(graphServiceOptimized));
     }*/


}
package com.example.graph.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Gremlin 查询模板配置类
 * 从 gremlin-queries.yml 文件加载所有查询模板
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gremlin")
public class GremlinQueryProperties {

    private Map<String, String> queries = new HashMap<>();

    /**
     * 获取查询模板
     * @param queryName 查询名称
     * @return 查询模板字符串
     */
    public String getQuery(String queryName) {
        String query = queries.get(queryName);
        if (query == null) {
            throw new IllegalArgumentException("未找到查询模板: " + queryName);
        }
        return query;
    }

    /**
     * 获取共同作品查询模板
     */
    public String getCommonWorksQuery() {
        return getQuery("common-works");
    }

    /**
     * 获取相似度分析查询模板
     */
    public String getSimilarityAnalysisQuery() {
        return getQuery("similarity-analysis");
    }

    /**
     * 获取共同祖先查询模板
     */
    public String getCommonAncestorQuery() {
        return getQuery("common-ancestor");
    }

    /**
     * 获取增强版共同好友查询模板
     */
    public String getEnhancedMutualFriendsQuery() {
        return getQuery("enhanced-mutual-friends");
    }

    /**
     * 获取关系链查询模板
     */
    public String getRelationChainQuery() {
        return getQuery("relation-chain");
    }

    /**
     * 获取明星关系网络查询模板
     */
    public String getCelebrityRelationshipsQuery() {
        return getQuery("celebrity-relationships");
    }

    /**
     * 获取共同活动查询模板
     */
    public String getCommonEventQuery() {
        return getQuery("common-event");
    }
}

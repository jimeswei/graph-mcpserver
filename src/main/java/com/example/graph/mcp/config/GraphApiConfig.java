package com.example.graph.mcp.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hugegraph.driver.HugeClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HugeGraph 图数据库配置类
 * @author claude
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "hugegraph")
public class GraphApiConfig {

    /**
     * HugeGraph 服务器地址
     */
    private String url = "http://192.168.3.78:8080";

    /**
     * 图空间名称
     */
    private String graph = "hugegraph";

    /**
     * 用户名
     */
    private String username = "";

    /**
     * 密码
     */
    private String password = "";

    /**
     * 连接超时时间（秒）
     */
    private int timeout = 30;

    /**
     * 最大连接数
     */
    private int maxTotal = 100;

    /**
     * 每个路由的最大连接数
     */
    private int maxPerRoute = 50;

    /**
     * 创建 HugeGraph Client Bean
     * @return HugeClient 实例
     */
    @Bean(destroyMethod = "close")
    public HugeClient hugeClient() {
        log.info("初始化 HugeGraph Client: url={}, graph={}", url, graph);

        try {
            // HugeClient 使用 Builder 模式构建
            HugeClient client;

            // 如果配置了用户名和密码，则使用认证构建客户端
            if (username != null && !username.trim().isEmpty()) {
                log.info("使用用户名密码认证方式连接 HugeGraph");
                client = HugeClient.builder(url, graph)
                        .configTimeout(timeout)
                        .configPool(maxTotal, maxPerRoute)
                        .configUser(username, password)
                        .build();
            } else {
                log.info("使用无认证方式连接 HugeGraph");
                client = HugeClient.builder(url, graph)
                        .configTimeout(timeout)
                        .configPool(maxTotal, maxPerRoute)
                        .build();
            }

            log.info("HugeGraph Client 初始化成功");
            return client;

        } catch (Exception e) {
            log.error("初始化 HugeGraph Client 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize HugeGraph Client", e);
        }
    }

    /**
     * 保留旧的配置方法以向后兼容
     * @deprecated 请使用 HugeClient Bean 代替
     * @return Gremlin 端点 URL
     */
    @Deprecated(since = "0.0.2", forRemoval = true)
    public String getBaseUrl() {
        return url + "/gremlin";
    }
}
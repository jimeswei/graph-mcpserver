package com.example.graph;

import com.example.graph.mcp.config.GraphApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * HugeGraph Client 集成测试
 * @author claude
 */
@Slf4j
@SpringBootTest
public class HugeGraphClientTest {

    @Autowired
    private HugeClient hugeClient;

    @Autowired
    private GraphApiConfig graphApiConfig;

    /**
     * 测试 HugeGraph Client 连接
     */
    @Test
    public void testHugeGraphConnection() {
        log.info("测试 HugeGraph Client 连接");
        log.info("配置信息: url={}, graph={}", graphApiConfig.getUrl(), graphApiConfig.getGraph());

        try {
            // 执行简单的查询测试连接
            ResultSet resultSet = hugeClient.gremlin()
                    .gremlin("g.V().limit(1)")
                    .execute();

            log.info("查询成功，返回结果数量: {}", resultSet.size());
            log.info("HugeGraph Client 连接正常");

        } catch (Exception e) {
            log.error("HugeGraph Client 连接失败: {}", e.getMessage(), e);
            throw new RuntimeException("HugeGraph Client 测试失败", e);
        }
    }

    /**
     * 测试执行 Gremlin 查询
     */
    @Test
    public void testGremlinQuery() {
        log.info("测试执行 Gremlin 查询");

        try {
            // 查询图中顶点数量
            String gremlin = "g.V().count()";
            ResultSet resultSet = hugeClient.gremlin()
                    .gremlin(gremlin)
                    .execute();

            log.info("图中顶点数量查询结果: {}", resultSet.data());

            // 查询前5个顶点
            gremlin = "g.V().limit(5).valueMap()";
            resultSet = hugeClient.gremlin()
                    .gremlin(gremlin)
                    .execute();

            log.info("前5个顶点查询结果: {}", resultSet.data());

        } catch (Exception e) {
            log.error("Gremlin 查询失败: {}", e.getMessage(), e);
            throw new RuntimeException("Gremlin 查询测试失败", e);
        }
    }
}

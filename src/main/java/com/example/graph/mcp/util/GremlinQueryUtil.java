package com.example.graph.mcp.util;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.hugegraph.driver.GremlinManager;
import org.apache.hugegraph.driver.HugeClient;
import org.apache.hugegraph.structure.gremlin.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gremlin 查询工具类
 * 使用 HugeGraph Client 执行 Gremlin 查询
 * @author claude
 */
@Slf4j
@Component
@ConditionalOnBean(HugeClient.class)
public class GremlinQueryUtil implements GremlinQueryExecutor {

    @Autowired
    private HugeClient hugeClient;

    /**
     * 执行 Gremlin 查询
     * @param query Gremlin 查询模板
     * @param params 查询参数
     * @return 查询结果的 ResponseEntity
     * @throws JsonProcessingException JSON 处理异常
     */
    @Override
    public ResponseEntity<String> executeGremlinRequest(String query, Map<String, Object> params)
            throws JsonProcessingException {
        try {
            // 使用参数替换查询模板
            log.info("Original query template: {}", query);
            log.info("Parameters for substitution: {}", params);

            StringSubstitutor substitutor = new StringSubstitutor(params);
            String gremlin = substitutor.replace(query);

            log.info("Final substituted gremlin query: {}", gremlin);

            // 使用 HugeGraph Client 执行 Gremlin 查询
            GremlinManager gremlinManager = hugeClient.gremlin();
            ResultSet resultSet = gremlinManager.gremlin(gremlin).execute();

            // 将结果转换为 JSON 字符串
            String resultJson = JSON.toJSONString(resultSet.data());
            log.info("Query result size: {}", resultSet.size());
            log.debug("Query result: {}", resultJson);

            // 包装为 ResponseEntity 以保持与原有接口兼容
            return ResponseEntity.ok(resultJson);

        } catch (Exception e) {
            log.error("执行 Gremlin 查询失败: {}", e.getMessage(), e);

            // 构建错误响应
            String errorJson = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                    e.getClass().getSimpleName(),
                    e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorJson);
        }
    }

    /**
     * 验证输入的名字列表
     * @param names 名字列表
     */
    public static void validateInput(List<String> names) {
        if (names == null || names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个有效用户名");
        }
    }
}
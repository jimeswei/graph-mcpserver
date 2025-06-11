package com.example.graph.mcp.service;

import com.example.graph.mcp.util.JsonExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class GraphServiceOptimized extends BaseGraphServiceOptimized {

        private static final int MAX_RELATION_CHAIN_DEPTH = 4;
        private static final String CELEBRITY_LABEL = "celebrity";
        private static final String CELEBRITY_RELATIONSHIP = "celebrity_celebrity";
        private static final String WORK_LABEL = "work";
        private static final String CELEBRITY_WORK_RELATIONSHIP = "celebrity_work";
        private static final String CELEBRITY_EVENT_RELATIONSHIP = "celebrity_event";

        /**
         * A 与 B的关系链
         *
         * @param sourceName 源节点名称
         * @param targetName 目标节点名称
         * @return 关系链的JSON响应
         * @throws IOException 如果执行查询时发生错误
         */
        @Tool(name = "relation_chain", description = "好友关系链")
        public String relationChain(String sourceName, String targetName) throws IOException {
                log.debug("Finding relation chain between {} and {}", sourceName, targetName);

                Map<String, Object> params = createParams(
                                Map.of("sourceName", sourceName, "targetName", targetName));

                String gremlinQuery = String.format(
                                "g.V().has('%s', 'name', '${sourceName}')" +
                                                ".repeat(both('%s').simplePath().where(without('visited')).aggregate('visited'))"
                                                +
                                                ".until(has('%s', 'name', '${targetName}').or().loops().is(%d)).dedup().path()",
                                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL, MAX_RELATION_CHAIN_DEPTH);

                return executeAndProcessQuery(gremlinQuery, params);
        }

        /**
         * 共同好友
         *
         * @param names 需要查询共同好友的节点名称列表
         * @return 共同好友的JSON响应
         * @throws IOException 如果执行查询时发生错误
         */
        @Tool(name = "mutual_friend", description = "共同好友")
        public String mutualFriend(List<String> names) throws IOException {
                validateInput(names);
                log.debug("Finding mutual friends for {}", names);

                Map<String, Object> params = createParams(
                                Map.of("name0", "'" + names.get(0) + "'",
                                                "name1", "'" + names.get(1) + "'"));

                String gremlinQuery = String.format(
                                "g.V().has('%s', 'name', ${name0}).as('a')" +
                                                ".both('%s').as('commonFriend')" +
                                                ".where(__.both('%s').has('%s', 'name', ${name0}))" +
                                                ".select('commonFriend').dedup().path()",
                                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);

                return executeAndProcessQuery(gremlinQuery, params);
        }

        /**
         * 共同参演电影
         *
         * @param names 需要查询共同参演电影的节点名称列表
         * @return 共同参演电影的JSON响应
         * @throws IOException 如果执行查询时发生错误
         */
        @Tool(name = "dream_team", description = "一起合作参演的电影")
        public String dreamTeam(List<String> names) throws IOException {
                validateInput(names);
                log.debug("Finding common works for {}", names);

                Map<String, Object> params = createParams(
                                Map.of("names", "'" + String.join("','", names) + "'"));

                String gremlinQuery = String.format(
                                "g.V().has('%s', 'name', within([${names}])).aggregate('stars')" +
                                                ".V().hasLabel('%s')" +
                                                ".where(__.in('%s', '%s').where(within('stars')).count().is(%d)).dedup().path()",
                                CELEBRITY_LABEL, WORK_LABEL, CELEBRITY_WORK_RELATIONSHIP,
                                CELEBRITY_EVENT_RELATIONSHIP, names.size());

                return executeAndProcessQuery(gremlinQuery, params);
        }

        @Tool(name = "friend_similarity", description = "好友相似度")
        public String friendSimilarity(List<String> names, String relationshipType) throws IOException {
                validateInput(names);
                log.debug("Finding friend similarity for {} with relationship type {}", names, relationshipType);

                Map<String, Object> params = createParams(
                                Map.of("name0", "'" + names.get(0) + "'",
                                                "name1", "'" + names.get(1) + "'",
                                                "relationshipType", "'" + relationshipType + "'"));

                String gremlinQuery = String.format(
                                "g.V().has('%s', 'name', ${name0}).as('a')" +
                                                ".bothE().has('e_type', ${relationshipType}).otherV().as('a_friends')" +
                                                ".V().has('%s', 'name', ${name1}).as('b')" +
                                                ".bothE().has('e_type', ${relationshipType}).otherV().as('b_friends')" +
                                                ".where('a_friends', eq('b_friends'))" +
                                                ".dedup().path()",
                                CELEBRITY_LABEL, CELEBRITY_LABEL);

                return executeAndProcessQuery(gremlinQuery, params);
        }

        /**
         * 创建参数映射
         *
         * @param params 参数映射
         * @return 包含所有必要参数的映射
         */
        private Map<String, Object> createParams(Map<String, Object> params) {
                Map<String, Object> allParams = new HashMap<>(params);
                allParams.put("relationshipType", "好友");
                return allParams;
        }

        /**
         * 执行Gremlin查询并处理响应
         *
         * @param gremlinQuery Gremlin查询语句
         * @param params       查询参数
         * @return 处理后的JSON响应
         * @throws IOException 如果执行查询时发生错误
         */
        private String executeAndProcessQuery(String gremlinQuery, Map<String, Object> params) throws IOException {
                try {
                        ResponseEntity<String> response = executeGremlinRequest(gremlinQuery, params);
                        String relationsJson = JsonExtractor.parseGraphView(response.getBody());
                        String detailsJson = JsonExtractor.parseResponse(response.getBody());
                        return JsonExtractor.buildFinalResponseSimple(detailsJson, relationsJson);
                } catch (Exception e) {
                        log.error("Error executing Gremlin query: {}", gremlinQuery, e);
                        throw new IOException("执行查询时发生错误: " + e.getMessage(), e);
                }
        }

}
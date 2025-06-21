package com.example.graph.mcp.service;

import com.example.graph.mcp.util.GremlinQueryUtil;
import com.example.graph.mcp.util.QueryResultHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import static com.example.graph.mcp.constant.GraphConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphServiceOptimized {
        private final GremlinQueryUtil gremlinQueryUtil;

        @Tool(name = "relation_chain_between_stars", description = "查询两个明星之间的好友关系链，返回从源明星到目标明星的路径，最多支持4层关系, 参数格式：1.sourceName: 人名1，2.targetName: 人名2")
        public String relationChain(String sourceName, String targetName) throws IOException {
                log.debug("Finding relation chain between {} and {}", sourceName, targetName);

                Map<String, Object> params = Map.of(
                                "sourceName", sourceName,
                                "targetName", targetName);

                String gremlinQuery = String.format(RELATION_CHAIN_QUERY,
                                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL, MAX_RELATION_CHAIN_DEPTH);

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                return QueryResultHandler.truncateResult(QueryResultHandler.processGraphQueryResult(response));
        }

        @Tool(name = "mutual_friend_between_stars", description = "查询两个明星之间的共同好友，返回他们共同的好友列表, 参数格式：names: [人名1, 人名2]")
        public String mutualFriend(List<String> names) throws IOException {
                GremlinQueryUtil.validateInput(names);
                log.debug("Finding mutual friends for {}", names);


                Map<String, Object> params = Map.of(
                "name0", "'" + names.get(0) + "'",
                "name1", "'" + names.get(1) + "'");


              /*  Map<String, Object> params = Map.of(
                                "names", "'" + String.join("','", names) + "'");
*/
                String gremlinQuery = String.format(MUTUAL_FRIEND_QUERY,
                                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                return QueryResultHandler.truncateResult(QueryResultHandler.processGraphQueryResult(response));
        }

        @Tool(name = "dream_team_common_works", description = "查询多个明星共同参演的电影，返回他们一起合作的作品列表，参数格式：1.names: [人名1, 人名2],2.relationshipType: 合作")
        public String dreamTeam(List<String> names) throws IOException {
                GremlinQueryUtil.validateInput(names);
                log.debug("Finding common works for {}", names);

                Map<String, Object> params = Map.of(
                                "names", "'" + String.join("','", names) + "'");

                String gremlinQuery = String.format(DREAM_TEAM_QUERY,
                                CELEBRITY_LABEL, WORK_LABEL, CELEBRITY_WORK_RELATIONSHIP,
                                CELEBRITY_EVENT_RELATIONSHIP, names.size());

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                return QueryResultHandler.truncateResult(QueryResultHandler.processGraphQueryResult(response));
        }

        @Tool(name = "similarity_between_stars", description = "查询多个明星之间的相似度，基于指定的关系类型，返回他们之间的相似关系,参数格式：1.names: [周星驰, 吴孟达], 2.relationshipType: 合作")
        public String similarity(List<String> names, String relationshipType) throws IOException {
                GremlinQueryUtil.validateInput(names);
                log.debug("Finding similarity for {} with relationship type {}", names, relationshipType);

                Map<String, Object> params = Map.of(
                                "names", "'" + String.join("','", names) + "'",
                                "relationshipType", relationshipType);

                String gremlinQuery = String.format(SIMILARITY_QUERY, CELEBRITY_LABEL);

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                return QueryResultHandler.truncateResult(QueryResultHandler.processGraphQueryResult(response));
        }

        @Tool(name = "most_recent_common_ancestor", description = "查询多个明星之间最近共同祖先,参数格式：1.names: [周星驰, 吴孟达], 2.maxDepth: 3 (可选,默认3层)")
        public String commonAncestor(List<String> names, Integer maxDepth) throws IOException {
                GremlinQueryUtil.validateInput(names);

                // 设置默认深度并验证
                int depth = (maxDepth != null && maxDepth > 0 && maxDepth <= MAX_ANCESTOR_DEPTH) ? maxDepth
                                : DEFAULT_ANCESTOR_DEPTH;
                log.debug("Finding common ancestors for {} within {} layers", names, depth);

                String gremlinQuery = buildCommonAncestorQuery(names, depth);
                Map<String, Object> params = buildQueryParams(names);

                try {
                        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                        String result = QueryResultHandler.processGraphQueryResult(response);

                        if (result == null || result.trim().isEmpty() || "[]".equals(result.trim())) {
                                log.info("No common ancestors found for {} within {} layers", names, depth);
                                return "未找到共同祖先，可能原因：1) 在" + depth + "层内无共同祖先 2) 数据中无祖先关系 3) 节点名称不存在";
                        }

                        return QueryResultHandler.truncateResult(result);
                } catch (Exception e) {
                        log.error("Error finding common ancestors for {}: {}", names, e.getMessage());
                        throw new IOException("查询共同祖先时发生错误: " + e.getMessage(), e);
                }
        }

        /**
         * 构建共同祖先查询语句 - 优化版
         */
        private String buildCommonAncestorQuery(List<String> names, int depth) {
                if (names.size() == 2) {
                        // 两人共同祖先的高效查询
                        return String.format(COMMON_ANCESTOR_TWO_PERSON_QUERY,
                                        CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, depth,
                                        CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, depth);
                } else {
                        // 多人共同祖先查询 - 使用更高效的交集算法
                        StringBuilder query = new StringBuilder();

                        // 收集所有人的祖先ID
                        for (int i = 0; i < names.size(); i++) {
                                if (i > 0)
                                        query.append(".");
                                query.append(String.format(COMMON_ANCESTOR_MULTI_PERSON_QUERY_PREFIX,
                                                CELEBRITY_LABEL, i, CELEBRITY_RELATIONSHIP, depth, i));
                        }

                        // 查找交集 - 从第一个集合开始，逐个过滤
                        StringBuilder filterConditions = new StringBuilder();
                        for (int i = 1; i < names.size(); i++) {
                                filterConditions.append(".where(__.id().is(within('ancestors").append(i).append("')))");
                        }
                        query.append(String.format(COMMON_ANCESTOR_MULTI_PERSON_QUERY_SUFFIX,
                                        filterConditions.toString()));

                        return query.toString();
                }
        }

        /**
         * 构建查询参数
         */
        private Map<String, Object> buildQueryParams(List<String> names) {
                Map<String, Object> params = new HashMap<>();
                if (names.size() == 2) {
                        // 两人查询使用 person1, person2
                        params.put("person1", names.get(0));
                        params.put("person2", names.get(1));
                } else {
                        // 多人查询使用 person0, person1, person2...
                        for (int i = 0; i < names.size(); i++) {
                                params.put("person" + i, names.get(i));
                        }
                }
                return params;
        }
}
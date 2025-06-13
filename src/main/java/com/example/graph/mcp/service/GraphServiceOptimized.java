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
                return QueryResultHandler.processGraphQueryResult(response);
        }

        @Tool(name = "mutual_friend_between_stars", description = "查询两个明星之间的共同好友，返回他们共同的好友列表, 参数格式：names: [人名1, 人名2]")
        public String mutualFriend(List<String> names) throws IOException {
                GremlinQueryUtil.validateInput(names);
                log.debug("Finding mutual friends for {}", names);

                Map<String, Object> params = Map.of(
                                "name0", "'" + names.get(0) + "'",
                                "name1", "'" + names.get(1) + "'");

                String gremlinQuery = String.format(MUTUAL_FRIEND_QUERY,
                                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);

                ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
                return QueryResultHandler.processGraphQueryResult(response);
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
                return QueryResultHandler.processGraphQueryResult(response);
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
                return QueryResultHandler.processGraphQueryResult(response);
        }
}
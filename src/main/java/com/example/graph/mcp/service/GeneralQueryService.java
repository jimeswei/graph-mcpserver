package com.example.graph.mcp.service;

import com.example.graph.mcp.constant.GraphConstants;
import com.example.graph.mcp.util.GremlinQueryUtil;
import com.example.graph.mcp.util.QueryResultHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.example.graph.mcp.constant.GraphConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneralQueryService {

    private final GremlinQueryUtil gremlinQueryUtil;

    @Tool(name = "query_celebrity_relationships", description = "查询多个明星之间的关系网络，返回这些明星及其直接关系。参数格式：names: [人名1, 人名2, ...]，支持查询任意数量的明星")
    public String getNodeEdgeByNames(List<String> names) throws IOException {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("需要至少一个有效用户名");
        }
        log.debug("Finding relationships for celebrities: {}", names);

        Map<String, Object> params = Map.of(
                "names", "'" + String.join("','", names) + "'");

        String gremlinQuery = String.format(NODES_EDGES_BY_NAMES_QUERY,
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP);

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        return QueryResultHandler.truncateResult(QueryResultHandler.processGraphQueryResult(response));
    }
}

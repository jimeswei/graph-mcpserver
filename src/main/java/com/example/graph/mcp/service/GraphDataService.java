package com.example.graph.mcp.service;

import com.example.graph.core.Graph;
import com.example.graph.core.Node;
import com.example.graph.core.Edge;
import com.example.graph.mcp.util.GremlinQueryUtil;
import com.example.graph.mcp.util.QueryResultHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.example.graph.mcp.constant.GraphConstants.*;

@Service
@RequiredArgsConstructor
public class GraphDataService {
    private final GremlinQueryUtil gremlinQueryUtil;

    /**
     * 根据指定节点名称集合，查询这些节点及其搭档关系，组装为Graph对象
     */
    public Graph getSubgraphByNames(List<String> names) {
        List<Node> nodes = queryNodesByNames(names);
        List<Edge> edges = queryEdgesByNames(names);
        return buildGraph(nodes, edges);
    }

    public List<Node> queryNodesByNames(List<String> names) {
        GremlinQueryUtil.validateInput(names);
        try {
            Map<String, Object> params = Map.of(
                    "names", "'" + String.join("','", names) + "'");
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(NODES_BY_NAMES_QUERY, params);
            return QueryResultHandler.extractNodes(response);
        } catch (Exception e) {
            throw new RuntimeException("queryNodesByNames error", e);
        }
    }

    public List<Edge> queryEdgesByNames(List<String> names) {
        GremlinQueryUtil.validateInput(names);
        try {
            Map<String, Object> params = Map.of(
                    "names", "'" + String.join("','", names) + "'");
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(EDGES_BY_NAMES_QUERY, params);
            return QueryResultHandler.extractEdges(response);
        } catch (Exception e) {
            throw new RuntimeException("queryEdgesByNames error", e);
        }
    }

    public Graph querySubgraphByNames(List<String> names) {
        List<Node> nodes = queryNodesByNames(names);
        List<Edge> edges = queryEdgesByNames(names);
        return buildGraph(nodes, edges);
    }

    private Graph buildGraph(List<Node> nodes, List<Edge> edges) {
        Graph graph = new Graph(false, false);
        for (Node node : nodes) {
            graph.addNode(node);
        }
        for (Edge edge : edges) {
            graph.addEdge(edge.getSource(), edge.getDestination(), edge.getWeight());
        }
        return graph;
    }
}
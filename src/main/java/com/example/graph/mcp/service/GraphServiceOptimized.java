package com.example.graph.mcp.service;

import com.example.graph.mcp.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;


import static com.example.graph.mcp.constant.GraphConstants.*;

@Service
public class GraphServiceOptimized {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GraphServiceOptimized.class);

    @Autowired
    private GremlinQueryUtil gremlinQueryUtil;

    @Autowired
    private GraphAnalysisService graphAnalysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为结果添加id字段
     */
    private String addIdToResult(String originalResult, String threadId) {
        try {
            if (originalResult == null || originalResult.trim().isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", threadId);
                result.put("data", null);
                return objectMapper.writeValueAsString(result);
            }
            
            // 尝试解析为JSON对象
            try {
                Map<String, Object> resultMap = objectMapper.readValue(originalResult, Map.class);
                resultMap.put("id", threadId);
                return objectMapper.writeValueAsString(resultMap);
            } catch (Exception e) {
                // 如果不是JSON对象，包装在data字段中
                Map<String, Object> wrapper = new HashMap<>();
                wrapper.put("id", threadId);
                wrapper.put("data", originalResult);
                return objectMapper.writeValueAsString(wrapper);
            }
        } catch (Exception e) {
            log.warn("Failed to add id to result: {}", e.getMessage());
            return originalResult; // 返回原始结果作为备用
        }
    }

    @Tool(name = "relation_chain_between_stars", description = "查询两个明星之间的好友关系链，返回从源明星到目标明星的路径，最多支持4层关系, 参数格式：1.sourceName: 人名1，2.targetName: 人名2")
    public String relationChain(@ToolParam(description = "人名1") String sourceName,
                              @ToolParam(description = "人名2") String targetName) throws IOException {
        if (sourceName == null || targetName == null || sourceName.trim().isEmpty() || targetName.trim().isEmpty()) {
            throw new IllegalArgumentException("源名字和目标名字都不能为空");
        }
        log.debug("Finding relation chain between {} and {}", sourceName, targetName);

        String threadId = UUID.randomUUID().toString();
        
        // 异步调用GraphAnalysisService保存完整图数据到MySQL
        try {
            graphAnalysisService.relationChain(sourceName, targetName, threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for relationChain: {}", e.getMessage());
        }

        // 执行简短数据查询
        Map<String, Object> params = Map.of(
            "sourceName", sourceName,
            "targetName", targetName
        );

        String gremlinQuery = String.format(RELATION_CHAIN_QUERY,
            CELEBRITY_LABEL,           // 起点标签
            CELEBRITY_RELATIONSHIP,    // 关系类型
            CELEBRITY_LABEL,          // 终点标签
            MAX_RELATION_CHAIN_DEPTH,  // 最大深度
            CELEBRITY_LABEL           // 终点标签（用于最后的过滤）
        );

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        
        // 使用专门的路径结果处理器返回简短数据
        String optimizedResult = QueryResultHandler.processPathQueryResult(response);
        String result = QueryResultHandler.truncateResult(optimizedResult);
        return addIdToResult(result, threadId);
    }

    @Tool(name = "mutual_friend_between_stars", description = "查询两个明星之间的共同好友，返回他们共同的好友列表, 参数格式：names: [人名1, 人名2]")
    public String mutualFriend(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names) throws IOException {
        validateInput(names);
        if (names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个人名");
        }
        log.debug("Finding mutual friends for {}", names);

        String threadId = UUID.randomUUID().toString();
        
        // 异步调用GraphAnalysisService保存完整图数据到MySQL
        try {
            graphAnalysisService.mutualFriend(names, threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for mutualFriend: {}", e.getMessage());
        }

        // 执行简短数据查询
        Map<String, Object> params = Map.of(
            "name0", "'" + names.get(0) + "'",
            "name1", "'" + names.get(1) + "'"
        );

        String gremlinQuery = String.format(MUTUAL_FRIEND_QUERY,
                        CELEBRITY_LABEL,           // 节点标签
                        CELEBRITY_RELATIONSHIP,    // 第一个both关系
                        CELEBRITY_RELATIONSHIP,    // where中的both关系
                        CELEBRITY_LABEL,          // where中的标签
                        CELEBRITY_RELATIONSHIP,    // 第一个inE关系
                        CELEBRITY_LABEL,          // 第一个where条件的标签
                        CELEBRITY_RELATIONSHIP,    // 第二个inE关系
                        CELEBRITY_LABEL           // 第二个where条件的标签
        );

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        
        // 使用专门的共同好友结果处理器返回简短数据
        String optimizedResult = QueryResultHandler.processMutualFriendsResult(response);
        String result = QueryResultHandler.truncateResult(optimizedResult);
        return addIdToResult(result, threadId);
    }

    @Tool(name = "dream_team_common_works", description = "查询多个明星共同参演的电影，返回他们一起合作的作品列表，参数格式：1.names: [人名1, 人名2],2.relationshipType: 合作")
    public String dreamTeam(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names) throws IOException {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("名字列表不能为空");
        }
        if (names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个名字才能查询共同作品");
        }
        
        log.debug("Finding common works for {}", names);

        String threadId = UUID.randomUUID().toString();
        
        // 异步调用GraphAnalysisService保存完整图数据到MySQL
        try {
            graphAnalysisService.dreamTeam(names, threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for dreamTeam: {}", e.getMessage());
        }

        // 执行简短数据查询
        Map<String, Object> params = Map.of(
                        "names", "'" + String.join("','", names) + "'");

        String gremlinQuery = String.format(DREAM_TEAM_QUERY,
                        CELEBRITY_LABEL, WORK_LABEL, CELEBRITY_WORK_RELATIONSHIP,
                        CELEBRITY_EVENT_RELATIONSHIP, names.size());

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        
        // 使用通用的图查询结果处理器返回简短数据
        String optimizedResult = QueryResultHandler.processGraphQueryResult(response);
        String result = QueryResultHandler.truncateResult(optimizedResult);
        return addIdToResult(result, threadId);
    }

    @Tool(name = "similarity_between_stars", description = "查询多个明星之间的相似度，基于指定的关系类型，返回他们之间的相似关系,参数格式：1.names: [周星驰, 吴孟达], 2.relationshipType: 合作")
    public String similarity(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names, @ToolParam(description = "边类型，如：合作，好友，搭档等") String relationshipType) throws IOException {
        validateInput(names);
        if (names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个人名进行相似度分析");
        }
        log.debug("Finding similarity for {} with relationship type {}", names, relationshipType);

        String threadId = UUID.randomUUID().toString();
        
        // 异步调用GraphAnalysisService保存完整图数据到MySQL
        try {
            graphAnalysisService.similarity(names, relationshipType, threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for similarity: {}", e.getMessage());
        }

        try {
            // 构建简化的相似度查询 - 查找两个人的共同连接
            if (names.size() != 2) {
                return addIdToResult("{\"details\": [], \"relations\": [], \"message\": \"相似度分析仅支持两个人\"}"  , threadId);
            }
            
            String gremlinQuery = String.format(
                "g.V().has('%s', 'name', '${person1}').as('p1')" +
                ".bothE('%s').has('e_type', '${relationshipType}').otherV()" +
                ".where(__.bothE('%s').has('e_type', '${relationshipType}').otherV()" +
                ".has('%s', 'name', '${person2}')).as('common')" +
                ".project('person1', 'person2', 'commonConnection', 'relationshipType')" +
                ".by(select('p1').values('name'))" +
                ".by(constant('${person2}'))" +
                ".by(select('common').values('name'))" +
                ".by(constant('${relationshipType}'))",
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL
            );
             
            Map<String, Object> params = Map.of(
                "person1", names.get(0),
                "person2", names.get(1),
                "relationshipType", relationshipType
            );
            
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            
            // 使用专门的相似度结果处理器
            String result = QueryResultHandler.processSimilarityQueryResult(response);
            return addIdToResult(result, threadId);
        } catch (Exception e) {
            log.error("Error finding similarity for {}: {}", names, e.getMessage());
            return addIdToResult(buildErrorResponse("相似度查询失败: " + e.getMessage()), threadId);
        }
    }

    @Tool(name = "most_recent_common_ancestor", description = "查询多个明星之间最近共同祖先,参数格式：1.names: [周星驰, 吴孟达], 2.maxDepth: 3 (可选,默认3层)")
    public String commonAncestor(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names,
            @ToolParam(description = "最大深度") Integer maxDepth) throws IOException {
        validateInput(names);
        if (names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个人名进行共同祖先查询");
        }

        String threadId = UUID.randomUUID().toString();
        
        // 异步调用GraphAnalysisService保存完整图数据到MySQL
        try {
            graphAnalysisService.commonAncestor(names, maxDepth, threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for commonAncestor: {}", e.getMessage());
        }

        int depth = (maxDepth != null && maxDepth > 0 && maxDepth <= MAX_ANCESTOR_DEPTH) ? maxDepth : DEFAULT_ANCESTOR_DEPTH;
        log.debug("Finding common ancestors for {} within {} layers", names, depth);

        try {
            String gremlinQuery = buildCommonAncestorQueryOptimized(names, depth);
            Map<String, Object> params = buildAncestorQueryParams(names);
            
            log.info("构建的共同祖先查询: {}", gremlinQuery);
            log.info("查询参数: {}", params);
            
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            
            // 使用专门的共同祖先结果处理器
            String result = QueryResultHandler.processCommonAncestorQueryResult(response);
            return addIdToResult(result, threadId);
        } catch (Exception e) {
            log.error("Error finding common ancestors for {}: {}", names, e.getMessage());
            return addIdToResult(buildErrorResponse("共同祖先查询失败: " + e.getMessage()), threadId);
        }
    }






    /**
     * 构建无祖先结果响应
     */
    private String buildNoAncestorFoundResponse(List<String> names, int depth) {
            return String.format("""
                    {
                      "message": "未找到共同祖先",
                      "query": %s,
                      "searchDepth": %d,
                      "suggestions": [
                        "在%d层内无共同祖先",
                        "数据中可能无祖先关系",
                        "尝试增加搜索深度"
                      ]
                    }""", names.toString(), depth, depth);
    }



    
    /**
     * 构建优化的共同祖先查询
     */
    private String buildCommonAncestorQueryOptimized(List<String> names, int depth) {
        if (names.size() == 2) {
            // 使用标准Gremlin语法的更简单方法
            return String.format(
                "g.V().has('%s', 'name', '${person0}')"
                + ".repeat(__.in('%s')).emit().times(%d).as('ancestors1')"
                + ".V().has('%s', 'name', '${person1}')"
                + ".repeat(__.in('%s')).emit().times(%d)"
                + ".where(eq('ancestors1'))"
                + ".dedup()"
                + ".limit(10)"
                + ".elementMap()",
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, depth,
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, depth
            );
        } else {
            // 多人查询使用迭代式交集
            StringBuilder query = new StringBuilder("g");
            
            // 为每个人收集祖先
            for (int i = 0; i < names.size(); i++) {
                query.append(".V().has('").append(CELEBRITY_LABEL).append("', 'name', '${person").append(i).append("}')");
                query.append(".repeat(__.in('").append(CELEBRITY_RELATIONSHIP).append("').simplePath()).emit().times(").append(depth).append(")");
                query.append(".id().fold().as('ancestors").append(i).append("')");
            }
            
            // 查找交集
            query.append(".V().where(__.id().is(within('ancestors0')))");
            for (int i = 1; i < names.size(); i++) {
                query.append(".where(__.id().is(within('ancestors").append(i).append("')))");
            }
            query.append(".dedup().limit(5).elementMap()");
            
            return query.toString();
        }
    }
    
    
    
    /**
     * 构建祖先查询参数
     */
    private Map<String, Object> buildAncestorQueryParams(List<String> names) {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            params.put("person" + i, names.get(i));
        }
        return params;
    }
    
    /**
     * 构建错误响应
     */
    private String buildErrorResponse(String errorMessage) {
        return String.format(
            "{\"error\": true, \"message\": \"%s\", \"timestamp\": \"%s\"}",
            errorMessage, java.time.Instant.now().toString()
        );
    }

    /**
     * 验证输入参数
     */
    private void validateInput(List<String> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("名字列表不能为空");
        }
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException(String.format("第%d个名字不能为空", i + 1));
            }
        }
    }
}
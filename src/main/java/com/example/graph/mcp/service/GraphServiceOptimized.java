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
import java.util.stream.Collectors;

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
     * 在JSON结果中添加id字段
     */
    private String addIdToResult(String originalResult, String threadId) throws IOException {
        log.debug("Adding id to result. ThreadId: {}, OriginalResult length: {}", threadId, 
                  originalResult != null ? originalResult.length() : 0);
        
        if (originalResult == null || originalResult.trim().isEmpty()) {
            log.warn("Original result is null or empty, creating default response");
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("id", threadId);
            defaultResult.put("message", "No data found");
            defaultResult.put("data", new ArrayList<>());
            return objectMapper.writeValueAsString(defaultResult);
        }
        
        try {
            Map<String, Object> resultMap = objectMapper.readValue(originalResult, Map.class);
            resultMap.put("id", threadId);
            String finalResult = objectMapper.writeValueAsString(resultMap);
            log.debug("Successfully added id to JSON result");
            return finalResult;
        } catch (Exception e) {
            log.warn("Failed to parse original result as JSON, wrapping as data: {}", e.getMessage());
            log.debug("Original result that failed to parse: {}", originalResult);
            
            // 尝试解析为数组或直接返回
            try {
                Object parsedData = objectMapper.readValue(originalResult, Object.class);
                Map<String, Object> wrapperResult = new HashMap<>();
                wrapperResult.put("id", threadId);
                wrapperResult.put("data", parsedData);
                return objectMapper.writeValueAsString(wrapperResult);
            } catch (Exception ex) {
                // 最后的备选方案：直接包装字符串
                Map<String, Object> wrapperResult = new HashMap<>();
                wrapperResult.put("id", threadId);
                wrapperResult.put("data", originalResult);
                return objectMapper.writeValueAsString(wrapperResult);
            }
        }
    }



    @Tool(name = "relation_chain_between_stars", description = "查询两个明星之间的好友关系链，返回从源明星到目标明星的路径，最多支持4层关系, 参数格式：1.sourceName: 人名1，2.targetName: 人名2")
    public String relationChain(@ToolParam(description = "人名1") String sourceName,
                                @ToolParam(description = "人名2") String targetName) throws IOException {
        validateNames(sourceName, targetName);
        log.debug("Finding relation chain between {} and {}", sourceName, targetName);

        Map<String, Object> params = new HashMap<>();
        params.put("sourceName", sourceName);
        params.put("targetName", targetName);
        String gremlinQuery = buildRelationChainQuery();

        log.debug("Generated Gremlin query: {}", gremlinQuery);
        log.debug("Query parameters: {}", params);

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);

        log.debug("Raw Gremlin response: {}", response.getBody());

        String threadId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        
        // 直接调用 GraphAnalysisService，它会自动保存到MySQL并返回标准vertices/edges格式
        String originalResult = graphAnalysisService.relationChain(sourceName, targetName, sessionId, threadId);
        
        return addIdToResult(originalResult, threadId);
    }

    @Tool(name = "mutual_friend_between_stars", description = "查询两个明星之间的共同好友，返回他们共同的好友列表, 参数格式：names: [人名1, 人名2]")
    public String mutualFriend(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names) throws IOException {
        validateMinimumNames(names, 2);
        log.debug("Finding mutual friends for {}", names);

        Map<String, Object> params = new HashMap<>();
        params.put("name0", "'" + names.get(0) + "'");
        params.put("name1", "'" + names.get(1) + "'");

        String gremlinQuery = buildMutualFriendQuery();
        
        log.debug("Generated mutual friend query: {}", gremlinQuery);
        log.debug("Query parameters: {}", params);
        
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        
        log.debug("Raw mutual friend response: {}", response.getBody());

        String threadId = UUID.randomUUID().toString();
        String originalResult = GraphResultFormatter.formatMutualFriends(response);
        
        // 异步调用 GraphAnalysisService，不影响主查询结果
        try {
            graphAnalysisService.mutualFriend(names, UUID.randomUUID().toString(), threadId);
        } catch (Exception e) {
            log.warn("Failed to save graph analysis data for mutualFriend: {}", e.getMessage());
        }
        
        return addIdToResult(originalResult, threadId);
    }


    @Tool(name = "dream_team_common_works", description = "查询多个明星共同参演的电影，返回他们一起合作的作品列表，参数格式：1.names: [人名1, 人名2],2.relationshipType: 合作")
    public String dreamTeam(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names) throws IOException {
        validateMinimumNames(names, 2);
        log.debug("Finding common works for {}", names);

        Map<String, Object> params = buildDreamTeamParams(names);
        String gremlinQuery = buildDreamTeamQuery(names);
        
        log.debug("Generated dream team query: {}", gremlinQuery);
        log.debug("Query parameters: {}", params);
        
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        
        log.debug("Dream team raw response: {}", response.getBody());
        
        String threadId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        
        // 直接调用 GraphAnalysisService，它会自动保存到MySQL并返回标准vertices/edges格式
        String originalResult = graphAnalysisService.dreamTeam(names, sessionId, threadId);
        
        return addIdToResult(originalResult, threadId);
    }

    @Tool(name = "similarity_between_stars", description = "查询多个明星之间的相似度，基于指定的关系类型，返回他们之间的相似关系,参数格式：1.names: [周星驰, 吴孟达], 2.relationshipType: 合作")
    public String similarity(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names, 
                           @ToolParam(description = "边类型，如：合作，好友，搭档等") String relationshipType) throws IOException {
        validateMinimumNames(names, 2);
        
        if (relationshipType == null || relationshipType.trim().isEmpty()) {
            relationshipType = "合作";
        }
        
        log.debug("Finding similarity for {} with relationship type {}", names, relationshipType);

        Map<String, Object> params = new HashMap<>();
        // 构建names数组字符串
        String namesArray = "['" + names.get(0) + "', '" + names.get(1) + "']";
        params.put("names", namesArray);
        params.put("relationshipType", relationshipType);

        String gremlinQuery = buildSimilarityQuery();
        log.debug("Generated similarity query: {}", gremlinQuery);
        log.debug("Query parameters: {}", params);
        
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        log.debug("Raw similarity response: {}", response.getBody());
        
        String threadId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        
        // 直接调用 GraphAnalysisService，它会自动保存到MySQL并返回标准vertices/edges格式
        String originalResult = graphAnalysisService.similarity(names, relationshipType, sessionId, threadId);
        
        return addIdToResult(originalResult, threadId);
    }

    @Tool(name = "most_recent_common_ancestor", description = "查询多个明星之间最近共同祖先,参数格式：1.names: [周星驰, 吴孟达], 2.maxDepth: 3 (可选,默认3层)")
    public String commonAncestor(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names,
                                 @ToolParam(description = "最大深度") Integer maxDepth) throws IOException {
        validateMinimumNames(names, 2);

        String threadId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        
        // 直接调用 GraphAnalysisService，它会自动保存到MySQL并返回标准vertices/edges格式
        String originalResult = graphAnalysisService.commonAncestor(names, maxDepth, sessionId, threadId);
        
        return addIdToResult(originalResult, threadId);
    }

    /**
     * 优化的两人共同祖先查询
     */
    private String findCommonAncestorForTwo(String person1, String person2) throws IOException {
        try {
            // 先尝试使用名字查询
            String gremlinQuery = String.format(COMMON_ANCESTOR_TWO_PERSON_QUERY,
                    "celebrity", "celebrity_celebrity", DEFAULT_ANCESTOR_DEPTH,
                    "celebrity", "celebrity_celebrity", DEFAULT_ANCESTOR_DEPTH);

            Map<String, Object> params = new HashMap<>();
            params.put("person1", person1);
            params.put("person2", person2);

            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String detailsJson = JsonExtractor.parseResponse(response.getBody());
            
            // 检查是否有结果
            if (detailsJson == null || detailsJson.trim().isEmpty() || "[]".equals(detailsJson.trim())) {
                return buildNoAncestorFoundResponse(Arrays.asList(person1, person2), DEFAULT_ANCESTOR_DEPTH);
            }
            
            // 解析查询结果
            List<?> ancestorsList = objectMapper.readValue(detailsJson, List.class);
            if (ancestorsList == null || ancestorsList.isEmpty()) {
                return buildNoAncestorFoundResponse(Arrays.asList(person1, person2), DEFAULT_ANCESTOR_DEPTH);
            }
            
            // 构建成功的响应
            Map<String, Object> result = new HashMap<>();
            result.put("query_type", "common_ancestor");
            result.put("persons", Arrays.asList(person1, person2));
            result.put("ancestors", ancestorsList);
            result.put("search_depth", DEFAULT_ANCESTOR_DEPTH);
            result.put("status", "success");
            
            String resultJson = objectMapper.writeValueAsString(result);

            if (isValidResult(resultJson)) {
                return resultJson;
            }

            // 如果名字查询失败，尝试获取ID后用ID查询
            String person1Id = getPersonId(person1);
            String person2Id = getPersonId(person2);
            if (person1Id != null && person2Id != null) {
                return findCommonAncestorByIdString(person1Id, person2Id);
            }

            return buildNoAncestorFoundResponse(Arrays.asList(person1, person2), DEFAULT_ANCESTOR_DEPTH);
        } catch (Exception e) {
            log.error("Error finding common ancestors for {} and {}: {}", person1, person2, e.getMessage());
            throw new IOException("查询共同祖先时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 使用ID查询共同祖先 - 返回String格式
     */
    private String findCommonAncestorByIdString(String person1Id, String person2Id) throws IOException {
        String gremlinQuery = String.format(COMMON_ANCESTOR_TWO_PERSON_BY_ID_QUERY,
                "celebrity_celebrity", DEFAULT_ANCESTOR_DEPTH,
                "celebrity_celebrity", DEFAULT_ANCESTOR_DEPTH);

        Map<String, Object> params = new HashMap<>();
        params.put("person1_id", person1Id);
        params.put("person2_id", person2Id);

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        String result = QueryResultHandler.processGraphQueryResult(response);

        return isValidResult(result) ? result
                : buildNoAncestorFoundResponse(Arrays.asList(person1Id, person2Id), DEFAULT_ANCESTOR_DEPTH);
    }

    /**
     * 获取人物的ID
     */
    private String getPersonId(String name) {
        try {
            String query = String.format("g.V().has('celebrity', 'name', '%s').id()", name);
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(query, new HashMap<>());
            String result = QueryResultHandler.processGraphQueryResult(response);
            return extractId(result);
        } catch (Exception e) {
            log.error("Error getting ID for {}: {}", name, e.getMessage());
            return null;
        }
    }

    private boolean isValidResult(String result) {
        return result != null && !result.trim().isEmpty() && !"[]".equals(result.trim());
    }

    private String extractId(String result) {
        if (result == null || result.trim().isEmpty()) {
            return null;
        }

        try {
            // 移除多余的空格和换行符
            result = result.trim();
            // 移除开头的 [ 和结尾的 ]
            if (result.startsWith("[") && result.endsWith("]")) {
                result = result.substring(1, result.length() - 1);
            }
            // 如果结果为空，返回null
            if (result.isEmpty()) {
                return null;
            }
            // 返回处理后的ID
            return result.trim();
        } catch (Exception e) {
            log.error("Error extracting ID from result: {}", e.getMessage());
            return null;
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







    // ====== 新增的优化辅助方法 ======

    /**
     * 验证两个名字参数
     */
    private void validateNames(String sourceName, String targetName) {
        if (sourceName == null || targetName == null ||
                sourceName.trim().isEmpty() || targetName.trim().isEmpty()) {
            throw new IllegalArgumentException("源名字和目标名字都不能为空");
        }
    }

    /**
     * 验证名字列表的最小数量
     */
    private void validateMinimumNames(List<String> names, int minCount) {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("名字列表不能为空");
        }

        // 检查每个名字是否有效
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException(String.format("第%d个名字不能为空", i + 1));
            }
        }

        if (names.size() < minCount) {
            throw new IllegalArgumentException(String.format("需要至少 %d 个人名", minCount));
        }
    }

    /**
     * 构建关系链查询语句
     */
    private String buildRelationChainQuery() {
        return String.format(RELATION_CHAIN_QUERY,
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);
    }

    /**
     * 构建共同好友查询语句
     */
    private String buildMutualFriendQuery() {
        return String.format(MUTUAL_FRIEND_QUERY,
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);
    }

    /**
     * 构建共同作品查询语句
     */
    private String buildDreamTeamQuery(List<String> names) {
        return DREAM_TEAM_QUERY;
    }

    /**
     * 构建共同作品查询参数
     */
    private Map<String, Object> buildDreamTeamParams(List<String> names) {
        if (names.size() < 2) {
            throw new IllegalArgumentException("需要至少两个人名");
        }

        Map<String, Object> params = new HashMap<>();

        // 设置第一个明星名字
        params.put("name1", names.get(0));

        // 构建其他明星的名字列表
        List<String> otherNames = names.subList(1, names.size());
        String otherNamesStr = "[" + otherNames.stream()
                .map(name -> "'" + name + "'")  // 添加单引号
                .collect(Collectors.joining(",")) + "]";

        params.put("other_names", otherNamesStr);
        params.put("other_names_count", String.valueOf(otherNames.size()));

        return params;
    }

    /**
     * 构建相似度查询语句
     */
    private String buildSimilarityQuery() {
        return String.format(SIMILARITY_QUERY,
                CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP);
    }

    /**
     * 执行共同祖先查询
     */
    private String executeCommonAncestorQuery(List<String> names, int depth) throws IOException {
        String gremlinQuery = buildCommonAncestorQuery(names, depth);
        Map<String, Object> params = buildQueryParams(names);

        try {
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            return GraphResultFormatter.formatCommonAncestor(response);
        } catch (Exception e) {
            log.error("Error finding common ancestors for {}: {}", names, e.getMessage());
            return buildNoAncestorFoundResponse(names, depth);
        }
    }
}
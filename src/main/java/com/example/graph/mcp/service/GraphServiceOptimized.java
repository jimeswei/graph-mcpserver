package com.example.graph.mcp.service;

import com.example.graph.mcp.constant.GraphConstants;
import com.example.graph.mcp.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.graph.mcp.constant.GraphConstants.*;

@Service
public class GraphServiceOptimized {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GraphServiceOptimized.class);

    @Autowired
    private GremlinQueryUtil gremlinQueryUtil;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(name = "relation_chain_between_stars", description = "查询两个明星之间的好友关系链，返回从源明星到目标明星的路径，最多支持4层关系, 参数格式：1.sourceName: 人名1，2.targetName: 人名2")
    public String relationChain(@ToolParam(description = "人名1") String sourceName,
                              @ToolParam(description = "人名2") String targetName) throws IOException {
        validateNames(sourceName, targetName);
        log.debug("Finding relation chain between {} and {}", sourceName, targetName);

        Map<String, Object> params = new HashMap<>();
        params.put("sourceName", sourceName);
        params.put("targetName", targetName);
        String gremlinQuery = buildRelationChainQuery();
        
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        return QueryResultHandler.truncateResult(GraphResultFormatter.formatRelationChain(response));
    }

    @Tool(name = "mutual_friend_between_stars", description = "查询两个明星之间的共同好友，返回他们共同的好友列表, 参数格式：names: [人名1, 人名2]")
    public String mutualFriend(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names) throws IOException {
        validateMinimumNames(names, 2);
        log.debug("Finding mutual friends for {}", names);

        Map<String, Object> params = new HashMap<>();
        params.put("name0", "'" + names.get(0) + "'");
        params.put("name1", "'" + names.get(1) + "'");

        String gremlinQuery = buildMutualFriendQuery();
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        return QueryResultHandler.truncateResult(GraphResultFormatter.formatMutualFriends(response));
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
        
        // 调试原始响应数据
        log.debug("Dream team raw response: {}", response.getBody());
        
        return QueryResultHandler.truncateResult(GraphResultFormatter.formatCommonWorks(response));
    }

    @Tool(name = "similarity_between_stars", description = "查询多个明星之间的相似度，基于指定的关系类型，返回他们之间的相似关系,参数格式：1.names: [周星驰, 吴孟达], 2.relationshipType: 合作")
    public String similarity(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names, 
                           @ToolParam(description = "边类型，如：合作，好友，搭档等") String relationshipType) throws IOException {
        validateMinimumNames(names, 2);
        
        // 修复null参数问题
        if (relationshipType == null || relationshipType.trim().isEmpty()) {
            relationshipType = "合作"; // 默认关系类型
        }
        
        log.debug("Finding similarity for {} with relationship type {}", names, relationshipType);

        Map<String, Object> params = new HashMap<>();
        params.put("name1", "'" + names.get(0) + "'");
        params.put("name2", "'" + names.get(1) + "'");
        params.put("relationshipType", relationshipType);

        String gremlinQuery = buildSimilarityQuery();
        log.debug("Generated similarity query: {}", gremlinQuery);
        log.debug("Query parameters: {}", params);
        
        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        log.debug("Raw similarity response: {}", response.getBody());
        
        // 直接返回图数据库的原始查询结果，不进行格式化处理
        return response.getBody();
    }

    @Tool(name = "most_recent_common_ancestor", description = "查询多个明星之间最近共同祖先,参数格式：1.names: [周星驰, 吴孟达], 2.maxDepth: 3 (可选,默认3层)")
    public String commonAncestor(@ToolParam(description = "多个人名，逗号分开，用方括号括起来") List<String> names,
            @ToolParam(description = "最大深度") Integer maxDepth) throws IOException {
        validateMinimumNames(names, 2);

        // 如果只有两个人，使用优化的两人查询
        if (names.size() == 2) {
            return findCommonAncestorForTwo(names.get(0), names.get(1));
        }

        // 多人查询使用原有逻辑
        int depth = (maxDepth != null && maxDepth > 0 && maxDepth <= MAX_ANCESTOR_DEPTH) ? maxDepth : DEFAULT_ANCESTOR_DEPTH;
        log.debug("Finding common ancestors for {} within {} layers", names, depth);

        return executeCommonAncestorQuery(names, depth);
    }

    /**
     * 优化的两人共同祖先查询
     */
    private String findCommonAncestorForTwo(String person1, String person2) throws IOException {
        try {
            // 先尝试使用名字查询
            String gremlinQuery = String.format(COMMON_ANCESTOR_TWO_PERSON_QUERY,
                    "celebrity", "celebrity_celebrity",
                    "celebrity_celebrity",
                    "celebrity", "celebrity_celebrity",
                    "celebrity_celebrity");

            Map<String, Object> params = new HashMap<>();
            params.put("person1", person1);
            params.put("person2", person2);

            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            String result = QueryResultHandler.processGraphQueryResult(response);

            if (isValidResult(result)) {
                return QueryResultHandler.truncateResult(result);
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
                "celebrity_celebrity",
                "celebrity_celebrity");

        Map<String, Object> params = new HashMap<>();
        params.put("person1_id", person1Id);
        params.put("person2_id", person2Id);

        ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
        String result = QueryResultHandler.processGraphQueryResult(response);

        return isValidResult(result) ? QueryResultHandler.truncateResult(result)
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

    // 新添加的方法 - 返回Mono格式用于REST API
    public Mono<Map<String, Object>> findCommonAncestorByNames(String person1, String person2) {
        // 先尝试查找祖父母级别的共同祖先
        return findCommonAncestorAtLevel(person1, person2, "grandparent")
                .flatMap(result -> {
                    if ((Boolean) result.get("found")) {
                        return Mono.just(result);
                    }
                    // 如果没找到祖父母级别的，查找父母级别的
                    return findCommonAncestorAtLevel(person1, person2, "parent");
                });
    }

    /**
     * 在指定层级查找共同祖先
     */
    private Mono<Map<String, Object>> findCommonAncestorAtLevel(String person1, String person2, String level) {
        // 首先调试查看两个人的关系类型
        debugPersonRelationships(person1);
        debugPersonRelationships(person2);
        
        String query;
        String ancestorField;
        
        if ("grandparent".equals(level)) {
            // 先尝试家庭关系过滤的查询
            query = String.format(GraphConstants.FAMILY_COMMON_ANCESTOR_GRANDPARENT_QUERY,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_RELATIONSHIP);
            ancestorField = "grandparent1";
        } else {
            query = String.format(GraphConstants.FAMILY_COMMON_ANCESTOR_PARENT_QUERY,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP);
            ancestorField = "parent1";
        }
        
        query = query.replace("${person1}", person1).replace("${person2}", person2);
        
        log.info("Executing family-filtered {} level query: {}", level, query);

        try {
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(query, Collections.emptyMap());
            String responseBody = response.getBody();
            
            log.info("Raw response for family-filtered {} level query: {}", level, responseBody);
            
            // 检查响应是否为空
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Empty response for family-filtered {} level ancestor query, falling back to original query", level);
                return executeOriginalQuery(person1, person2, level);
            }
            
            String jsonResult = JsonExtractor.parseResponse(responseBody);
            log.info("Parsed JSON result for family-filtered {} level: {}", level, jsonResult);
            
            // 检查解析结果是否为空
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.info("No family-filtered {} level common ancestors found, falling back to original query", level);
                return executeOriginalQuery(person1, person2, level);
            }
            
            List<Map<String, Object>> results = objectMapper.readValue(jsonResult, List.class);
       
            return Mono.just(processLevelAncestorResult(results, person1, person2, ancestorField, level));
        } catch (Exception e) {
            log.error("Error executing family-filtered {} level ancestor query: {}, falling back to original query", level, e.getMessage());
            return executeOriginalQuery(person1, person2, level);
        }
    }
    
    /**
     * 执行原始的查询（不过滤关系类型）作为后备方案
     */
    private Mono<Map<String, Object>> executeOriginalQuery(String person1, String person2, String level) {
        String query;
        String ancestorField;
        
        if ("grandparent".equals(level)) {
            query = String.format(GraphConstants.COMMON_ANCESTOR_TWO_PERSON_QUERY,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_RELATIONSHIP);
            ancestorField = "grandparent1";
        } else {
            query = String.format(GraphConstants.COMMON_ANCESTOR_PARENT_LEVEL_QUERY,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP);
            ancestorField = "parent1";
        }
        
        query = query.replace("${person1}", person1).replace("${person2}", person2);
        
        log.info("Executing original {} level query: {}", level, query);

        try {
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(query, Collections.emptyMap());
            String responseBody = response.getBody();
            
            log.info("Raw response for original {} level query: {}", level, responseBody);
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("Empty response for original {} level ancestor query", level);
                return Mono.just(createEmptyAncestorResult(person1, person2, "查询返回空结果"));
            }
            
            String jsonResult = JsonExtractor.parseResponse(responseBody);
            log.info("Parsed JSON result for original {} level: {}", level, jsonResult);
            
            if (jsonResult == null || jsonResult.trim().isEmpty() || "[]".equals(jsonResult.trim())) {
                log.info("No {} level common ancestors found for {} and {}", level, person1, person2);
                return Mono.just(createEmptyAncestorResult(person1, person2, "未找到" + level + "级别共同祖先"));
            }
            
            List<Map<String, Object>> results = objectMapper.readValue(jsonResult, List.class);
       
            return Mono.just(processLevelAncestorResult(results, person1, person2, ancestorField, level));
        } catch (Exception e) {
            log.error("Error executing original {} level ancestor query: {}", level, e.getMessage());
            return Mono.just(createEmptyAncestorResult(person1, person2, "查询失败: " + e.getMessage()));
        }
    }

    /**
     * 调试某人的所有关系类型
     */
    private void debugPersonRelationships(String person) {
        try {
            String query = String.format(GraphConstants.DEBUG_PERSON_RELATIONSHIPS_QUERY,
                    GraphConstants.CELEBRITY_LABEL,
                    GraphConstants.CELEBRITY_RELATIONSHIP,
                    GraphConstants.CELEBRITY_LABEL);
            
            query = query.replace("${person}", person);
            
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(query, Collections.emptyMap());
            String responseBody = response.getBody();
            String jsonResult = JsonExtractor.parseResponse(responseBody);
            
            log.info("=== {} 的所有关系类型 ===", person);
            log.info("Raw response: {}", responseBody);
            log.info("Parsed JSON: {}", jsonResult);
            
        } catch (Exception e) {
            log.error("Error debugging relationships for {}: {}", person, e.getMessage());
        }
    }

    /**
     * 处理特定层级的祖先结果，优先选择真正的家庭成员
     */
    private Map<String, Object> processLevelAncestorResult(List<Map<String, Object>> results, String person1, String person2, String ancestorField, String level) {
        if (results == null || results.isEmpty()) {
            return createEmptyAncestorResult(person1, person2, "未找到共同祖先");
        }

        log.info("Processing {} results for level {}: {}", results.size(), level, results);

        // 优先选择策略：
        // 1. 首先寻找姓氏匹配的祖先（真正的家庭成员）
        // 2. 然后选择profession为空的（可能是家庭成员）
        // 3. 最后选择第一个结果
        
        Map<String, Object> selectedAncestor = null;
        String person1Surname = extractSurname(person1);
        String person2Surname = extractSurname(person2);
        
        // 策略1：寻找姓氏匹配的祖先
        for (Map<String, Object> result : results) {
            Map<String, Object> ancestor = (Map<String, Object>) result.get(ancestorField);
            if (ancestor != null && ancestor.get("name") != null) {
                String ancestorName = ancestor.get("name").toString();
                String ancestorSurname = extractSurname(ancestorName);
                
                // 如果祖先的姓氏与两个人中任一人匹配，优先选择
                if (person1Surname.equals(ancestorSurname) || person2Surname.equals(ancestorSurname)) {
                    log.info("Found surname-matching ancestor: {} (matches {} or {})", ancestorName, person1Surname, person2Surname);
                    selectedAncestor = result;
                    break;
                }
            }
        }
        
        // 策略2：如果没找到姓氏匹配的，选择profession为空的
        if (selectedAncestor == null) {
            for (Map<String, Object> result : results) {
                Map<String, Object> ancestor = (Map<String, Object>) result.get(ancestorField);
                if (ancestor != null && (ancestor.get("profession") == null || ancestor.get("profession").toString().trim().isEmpty())) {
                    log.info("Found ancestor without profession (likely family member): {}", ancestor.get("name"));
                    selectedAncestor = result;
                    break;
                }
            }
        }
        
        // 策略3：使用第一个结果作为后备
        if (selectedAncestor == null) {
            selectedAncestor = results.get(0);
            log.info("Using first result as fallback: {}", selectedAncestor);
        }

        // 构建最终结果
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("person1", person1);
        finalResult.put("person2", person2);
        finalResult.put("found", true);
        finalResult.put("level", level);
        finalResult.put("message", "找到" + level + "级别共同祖先");
        
        Map<String, Object> commonAncestor = (Map<String, Object>) selectedAncestor.get(ancestorField);
        finalResult.put("common_ancestor", commonAncestor);

        log.info("Final selected ancestor result: {}", finalResult);
        return finalResult;
    }
    
    /**
     * 提取姓氏（中文名字的第一个字符）
     */
    private String extractSurname(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        return fullName.trim().substring(0, 1);
    }

    /**
     * 创建空的祖先查询结果
     */
    private Map<String, Object> createEmptyAncestorResult(String person1, String person2, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("person1", person1);
        result.put("person2", person2);
        result.put("common_ancestor", null);
        result.put("found", false);
        result.put("message", message);
        return result;
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
            CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL,
            MAX_RELATION_CHAIN_DEPTH, CELEBRITY_LABEL);
    }
    
    /**
     * 构建共同好友查询语句
     */
    private String buildMutualFriendQuery() {
        return String.format(MUTUAL_FRIEND_QUERY,
            CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_RELATIONSHIP,
            CELEBRITY_LABEL, CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL,
            CELEBRITY_RELATIONSHIP, CELEBRITY_LABEL);
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
        params.put("names[0]", names.get(0));
        
        // 构建其他明星的名字列表
        List<String> otherNames = names.subList(1, names.size());
        String otherNamesStr = otherNames.stream()
                .map(name -> "'" + name + "'")  // 添加单引号
                .collect(Collectors.joining(","));
        
        params.put("other_names", otherNamesStr);
        params.put("other_names_count", String.valueOf(otherNames.size()));
        
        return params;
    }
    
    /**
     * 构建相似度查询语句
     */
    private String buildSimilarityQuery() {
        return SIMILARITY_QUERY;
    }
    
    /**
     * 执行共同祖先查询
     */
    private String executeCommonAncestorQuery(List<String> names, int depth) throws IOException {
        String gremlinQuery = buildCommonAncestorQuery(names, depth);
        Map<String, Object> params = buildQueryParams(names);

        try {
            ResponseEntity<String> response = gremlinQueryUtil.executeGremlinRequest(gremlinQuery, params);
            return QueryResultHandler.truncateResult(GraphResultFormatter.formatCommonAncestor(response));
        } catch (Exception e) {
            log.error("Error finding common ancestors for {}: {}", names, e.getMessage());
            return buildNoAncestorFoundResponse(names, depth);
        }
    }
}
package com.example.graph.mcp.agent;

import com.example.graph.mcp.service.GraphServiceOptimized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图查询Agent - 理解用户查询意图并执行相应的图查询
 */
@Slf4j
@Component
public class GraphQueryAgent {

    @Autowired
    private GraphServiceOptimized graphService;

    // 意图识别模式
    private static final Map<Pattern, QueryIntent> INTENT_PATTERNS = new LinkedHashMap<>();

    static {
        // 共同好友查询
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)和(.+?)的?共同好友"),
                QueryIntent.MUTUAL_FRIENDS);
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)与(.+?)的?共同好友"),
                QueryIntent.MUTUAL_FRIENDS);

        // 关系链查询
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)和(.+?)的?关系"),
                QueryIntent.RELATIONSHIP_CHAIN);
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)与(.+?)怎么认识"),
                QueryIntent.RELATIONSHIP_CHAIN);

        // 共同作品查询
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)和(.+?)合作的?电影"),
                QueryIntent.COLLABORATION);
        INTENT_PATTERNS.put(
                Pattern.compile("(.+?)和(.+?)共同参演"),
                QueryIntent.COLLABORATION);
    }

    /**
     * 处理用户查询
     */
    /*
     * public QueryResult processQuery(String userQuery) {
     * log.info("处理用户查询: {}", userQuery);
     * 
     * try {
     * // 1. 识别查询意图
     * IntentResult intentResult = identifyIntent(userQuery);
     * if (intentResult == null) {
     * return QueryResult.error("无法理解您的查询，请尝试询问共同好友、关系链或合作信息");
     * }
     * 
     * // 2. 执行相应的查询
     * String result = executeQuery(intentResult);
     * 
     * // 3. 构建返回结果
     * return QueryResult.success(intentResult.intent, intentResult.entities,
     * result);
     * 
     * } catch (Exception e) {
     * log.error("查询处理失败", e);
     * return QueryResult.error("查询处理失败: " + e.getMessage());
     * }
     * }
     */

    /**
     * 识别查询意图
     */
    private IntentResult identifyIntent(String query) {
        for (Map.Entry<Pattern, QueryIntent> entry : INTENT_PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(query);
            if (matcher.find()) {
                List<String> entities = new ArrayList<>();
                // 提取所有捕获组（人名）
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    entities.add(matcher.group(i).trim());
                }
                return new IntentResult(entry.getValue(), entities);
            }
        }
        return null;
    }

    /**
     * 执行查询
     */
    // private String executeQuery(IntentResult intentResult) throws IOException {
    // switch (intentResult.intent) {
    // case MUTUAL_FRIENDS:
    // return graphService.mutualFriend(intentResult.entities);
    //
    // case RELATIONSHIP_CHAIN:
    // if (intentResult.entities.size() >= 2) {
    // return graphService.relationChain(
    // intentResult.entities.get(0),
    // intentResult.entities.get(1));
    // }
    // break;
    //
    // case COLLABORATION:
    // return graphService.dreamTeam(intentResult.entities);
    //
    // default:
    // throw new UnsupportedOperationException("不支持的查询类型: " + intentResult.intent);
    // }
    // return null;
    // }

    /**
     * 查询意图枚举
     */
    public enum QueryIntent {
        MUTUAL_FRIENDS("共同好友"),
        RELATIONSHIP_CHAIN("关系链"),
        COLLABORATION("合作信息"),
        GENERAL_QUERY("通用查询");

        private final String description;

        QueryIntent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 意图识别结果
     */
    private static class IntentResult {
        final QueryIntent intent;
        final List<String> entities;

        IntentResult(QueryIntent intent, List<String> entities) {
            this.intent = intent;
            this.entities = entities;
        }
    }

    /**
     * 查询结果
     */
    public static class QueryResult {
        private final boolean success;
        private final QueryIntent intent;
        private final List<String> entities;
        private final String data;
        private final String message;

        private QueryResult(boolean success, QueryIntent intent, List<String> entities, String data, String message) {
            this.success = success;
            this.intent = intent;
            this.entities = entities;
            this.data = data;
            this.message = message;
        }

        public static QueryResult success(QueryIntent intent, List<String> entities, String data) {
            return new QueryResult(true, intent, entities, data, null);
        }

        public static QueryResult error(String message) {
            return new QueryResult(false, null, null, null, message);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public QueryIntent getIntent() {
            return intent;
        }

        public List<String> getEntities() {
            return entities;
        }

        public String getData() {
            return data;
        }

        public String getMessage() {
            return message;
        }
    }
}
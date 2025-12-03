package com.example.graph.mcp.constant;

public class GraphConstants {
        // 节点标签
        public static final String CELEBRITY_LABEL = "celebrity";
        public static final String WORK_LABEL = "work";
        public static final String EVENT_LABEL = "event";

        // 关系类型
        public static final String CELEBRITY_RELATIONSHIP = "celebrity_celebrity";
        public static final String CELEBRITY_WORK_RELATIONSHIP = "celebrity_work";
        public static final String CELEBRITY_EVENT_RELATIONSHIP = "celebrity_event";

        // 属性名
        public static final String NAME_PROPERTY = "name";
        public static final String CELEBRITY_ID_PROPERTY = "celebrity_id";
        public static final String PROFESSION_PROPERTY = "profession";
        public static final String COMPANY_PROPERTY = "company";
        public static final String NATIONALITY_PROPERTY = "nationality";
        public static final String WEIGHT_PROPERTY = "weight";

        // 查询参数
        public static final int MAX_RELATION_CHAIN_DEPTH = 4;

        // 默认查询深度
        public static final int DEFAULT_ANCESTOR_DEPTH = 3;
        public static final int MAX_ANCESTOR_DEPTH = 6;

        // ====== Gremlin 查询模板已移至 gremlin-queries.yml 配置文件 ======
        // 如需添加或修改查询模板,请编辑 src/main/resources/gremlin-queries.yml

        // 以下查询模板已废弃,保留仅用于向后兼容
        @Deprecated
        public static final String RELATION_CHAIN_QUERY = "请使用 GremlinQueryProperties.getRelationChainQuery()";
        @Deprecated
        public static final String MUTUAL_FRIEND_QUERY = "请使用 GremlinQueryProperties.getEnhancedMutualFriendsQuery()";
        @Deprecated
        public static final String DREAM_TEAM_QUERY = "请使用 GremlinQueryProperties.getCommonWorksQuery()";
        @Deprecated
        public static final String SIMILARITY_QUERY = "请使用 GremlinQueryProperties.getSimilarityAnalysisQuery()";
        @Deprecated
        public static final String NODES_BY_NAMES_QUERY = "请使用 GremlinQueryProperties.getCelebrityRelationshipsQuery()";
        @Deprecated
        public static final String EDGES_BY_NAMES_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String NODES_EDGES_BY_NAMES_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String COMMON_ANCESTOR_TWO_PERSON_QUERY = "请使用 GremlinQueryProperties.getCommonAncestorQuery()";
        @Deprecated
        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_PREFIX = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_SUFFIX = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String COMMON_ANCESTOR_TWO_PERSON_BY_ID_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String FAMILY_COMMON_ANCESTOR_GRANDPARENT_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String FAMILY_COMMON_ANCESTOR_PARENT_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String COMMON_ANCESTOR_PARENT_LEVEL_QUERY = "请使用 GremlinQueryProperties 配置类";
        @Deprecated
        public static final String DEBUG_PERSON_RELATIONSHIPS_QUERY = "请使用 GremlinQueryProperties 配置类";
}
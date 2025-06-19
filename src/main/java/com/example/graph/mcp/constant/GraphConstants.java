package com.example.graph.mcp.constant;

public class GraphConstants {
        // 节点标签
        public static final String CELEBRITY_LABEL = "celebrity";
        public static final String WORK_LABEL = "work";

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

        // Gremlin 查询模板
        public static final String RELATION_CHAIN_QUERY = "g.V().has('%s', 'name', '${sourceName}')" +
                        ".repeat(both('%s').simplePath().where(without('visited')).aggregate('visited'))" +
                        ".until(has('%s', 'name', '${targetName}').or().loops().is(%d)).dedup().path()";

        public static final String MUTUAL_FRIEND_QUERY = "g.V().has('%s', 'name', ${name0}).as('a')" +
                        ".both('%s').as('commonFriend')" +
                        ".where(__.both('%s').has('%s', 'name', ${name0}))" +
                        ".select('commonFriend').dedup().path()";

        public static final String DREAM_TEAM_QUERY = "g.V().has('%s', 'name', within([${names}])).aggregate('stars')" +
                        ".V().hasLabel('%s')" +
                        ".where(__.in('%s', '%s').where(within('stars')).count().is(%d)).dedup().path()";

        public static final String SIMILARITY_QUERY = "g.V().has('%s', 'name', within([${names}])).as('o')" +
                        ".bothE().has('e_type', '${relationshipType}').otherV().aggregate('x')" +
                        ".bothE().has('e_type', '${relationshipType}').otherV().where(neq('o'))" +
                        ".where(bothE().has('e_type', '${relationshipType}').otherV().where(within('x')).dedup().count().is(gt(5))).path()";

        public static final String NODES_BY_NAMES_QUERY = "g.V().has('celebrity', 'name', within([${names}])).as('center')"
                        +
                        ".both('celebrity_celebrity').as('partner')" +
                        ".select('center','partner')" +
                        ".by(valueMap('celebrity_id','name','profession','company','nationality'))";

        public static final String EDGES_BY_NAMES_QUERY = "g.V().has('celebrity', 'name', within([${names}])).bothE('celebrity_celebrity')"
                        +
                        ".as('e').otherV().as('other')" +
                        ".select('e','other').by(valueMap()).by(valueMap('name'))";

        public static final String NODES_EDGES_BY_NAMES_QUERY = "g.V().has('celebrity', 'name', within([${names}])).bothE('celebrity_celebrity')";

        // 共同祖先查询模板
        public static final String COMMON_ANCESTOR_TWO_PERSON_QUERY = "g.V().has('%s', 'name', '${person1}')" +
                        ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                        ".id().fold().as('ancestors1')" +
                        ".V().has('%s', 'name', '${person2}')" +
                        ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                        ".where(__.id().is(within('ancestors1')))" +
                        ".dedup().elementMap()";

        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_PREFIX = "g.V().has('%s', 'name', '${person%d}')"
                        +
                        ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                        ".id().fold().as('ancestors%d')";

        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_SUFFIX = ".V().where(__.id().is(within('ancestors0')))%s"
                        +
                        ".dedup().elementMap()";

        // 默认查询深度
        public static final int DEFAULT_ANCESTOR_DEPTH = 3;
        public static final int MAX_ANCESTOR_DEPTH = 6;
}
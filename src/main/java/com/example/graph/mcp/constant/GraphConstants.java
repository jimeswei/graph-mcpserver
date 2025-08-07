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

        // Gremlin 查询模板
        public static final String RELATION_CHAIN_QUERY = "g.V().has('%s', 'name', '${sourceName}')" +
                        ".repeat(both('%s').simplePath())" +
                        ".until(has('%s', 'name', '${targetName}'))" +
                        ".limit(1).path().by('name')";

        public static final String MUTUAL_FRIEND_QUERY = "g.V().has('%s', 'name', ${name0})" +
                        ".both('%s').as('commonFriend')" +
                        ".where(__.both('%s').has('%s', 'name', ${name1}))" +
                        ".select('commonFriend').dedup().values('name')";

        public static final String DREAM_TEAM_QUERY = "g.V().has('%s', 'name', within([${names}])).aggregate('stars')" +
                        ".V().hasLabel('%s')" +
                        ".where(__.in('%s', '%s').where(within('stars')).count().is(%d)).dedup().path()";

        public static final String SIMILARITY_QUERY = "g.V().has('%s', 'name', within([${names}]))" +
                        ".as('person').bothE('%s').has('e_type', '${relationshipType}').otherV().as('connected')" +
                        ".select('person', 'connected').by('name').by('name')" +
                        ".groupCount().unfold()" +
                        ".where(select(Column.values).is(gte(2)))" +
                        ".project('source', 'target', 'type', 'strength')" +
                        ".by(select(Column.keys).unfold().limit(1))" +
                        ".by(select(Column.keys).unfold().tail(1))" +
                        ".by(constant('${relationshipType}'))" +
                        ".by(select(Column.values))";

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

        // ID查询共同祖先模板
        public static final String COMMON_ANCESTOR_TWO_PERSON_BY_ID_QUERY = "g.V('${person1_id}')" +
                ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                ".id().fold().as('ancestors1')" +
                ".V('${person2_id}')" +
                ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                ".where(__.id().is(within('ancestors1')))" +
                ".dedup().elementMap()";

        // 家庭关系过滤的祖父母级查询
        public static final String FAMILY_COMMON_ANCESTOR_GRANDPARENT_QUERY = "g.V().has('%s', 'name', '${person1}')" +
                ".repeat(__.in('%s').where(__.bothE('%s').has('e_type', within(['父子', '母子', '家庭']))).simplePath()).emit().times(2)" +
                ".as('grandparent1')" +
                ".V().has('%s', 'name', '${person2}')" +
                ".repeat(__.in('%s').where(__.bothE('%s').has('e_type', within(['父子', '母子', '家庭']))).simplePath()).emit().times(2)" +
                ".where(__.id().is(within(select('grandparent1').id().fold())))" +
                ".dedup().elementMap()";

        // 家庭关系过滤的父母级查询
        public static final String FAMILY_COMMON_ANCESTOR_PARENT_QUERY = "g.V().has('%s', 'name', '${person1}')" +
                ".repeat(__.in('%s').where(__.bothE().has('e_type', within(['父子', '母子', '家庭']))).simplePath()).emit().times(1)" +
                ".as('parent1')" +
                ".V().has('%s', 'name', '${person2}')" +
                ".repeat(__.in('%s').where(__.bothE().has('e_type', within(['父子', '母子', '家庭']))).simplePath()).emit().times(1)" +
                ".where(__.id().is(within(select('parent1').id().fold())))" +
                ".dedup().elementMap()";

        // 父母级通用查询
        public static final String COMMON_ANCESTOR_PARENT_LEVEL_QUERY = "g.V().has('%s', 'name', '${person1}')" +
                ".repeat(__.in('%s').simplePath()).emit().times(1)" +
                ".as('parent1')" +
                ".V().has('%s', 'name', '${person2}')" +
                ".repeat(__.in('%s').simplePath()).emit().times(1)" +
                ".where(__.id().is(within(select('parent1').id().fold())))" +
                ".dedup().elementMap()";

        // 调试人员关系查询
        public static final String DEBUG_PERSON_RELATIONSHIPS_QUERY = "g.V().has('%s', 'name', '${person}')" +
                ".bothE('%s').group().by('e_type').by(__.otherV().has('%s', 'name').values('name').fold())";
}
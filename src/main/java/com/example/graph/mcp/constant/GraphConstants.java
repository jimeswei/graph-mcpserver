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
                        ".repeat(both('%s')" +
                        ".simplePath()" +  // 避免环路
                        ".where(without('visited'))" +
                        ".aggregate('visited'))" +
                        ".until(has('%s', 'name', '${targetName}')" +
                        ".or()" +
                        ".loops().is(%d))" +
                        ".has('%s', 'name', '${targetName}')" +  // 确保最后一个节点是目标
                        ".path()" +
                        ".by(valueMap('name', 'profession'))" +  // 返回节点的基本信息
                        ".by(valueMap('weight', 'relationship_type'))" +  // 返回边的信息
                        ".limit(5)";  // 限制返回的路径数量

        public static final String MUTUAL_FRIEND_QUERY = "g.V().has('%s', 'name', ${name0})" +
                        ".both('%s').as('friends')" +
                        ".both('%s').has('%s', 'name', ${name1})" +
                        ".select('friends')" +
                        ".dedup()" +
                        ".project('commonFriend', 'relationships')" +
                        ".by(valueMap('name', 'profession'))" +
                        ".by(union(__.inE('%s').where(outV().has('%s', 'name', ${name0}))," +
                        "__.inE('%s').where(outV().has('%s', 'name', ${name1})))" +
                        ".valueMap('weight', 'relationship_type'))";

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

        // 共同祖先查询模板 - 优先查找祖父母级别的祖先
        public static final String COMMON_ANCESTOR_TWO_PERSON_QUERY = 
            "g.V().has('%s', 'name', '${person1}').as('p1')" +
            ".in('%s').in('%s').as('grandparent1')" +  // 查找祖父母
            ".V().has('%s', 'name', '${person2}').as('p2')" +
            ".in('%s').in('%s').as('grandparent2')" +  // 查找祖父母
            ".where('grandparent1', eq('grandparent2'))" +
            ".select('p1', 'p2', 'grandparent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('name', 'profession', 'celebrity_id').by(unfold()))" +
            ".dedup()";

        // 如果没找到祖父母级别的，查找父母级别的共同祖先
        public static final String COMMON_ANCESTOR_PARENT_LEVEL_QUERY = 
            "g.V().has('%s', 'name', '${person1}').as('p1')" +
            ".in('%s').as('parent1')" +
            ".V().has('%s', 'name', '${person2}').as('p2')" +
            ".in('%s').as('parent2')" +
            ".where('parent1', eq('parent2'))" +
            ".select('p1', 'p2', 'parent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('name', 'profession', 'celebrity_id').by(unfold()))" +
            ".dedup()";

        // 添加新的查询模板用于ID查询
        public static final String COMMON_ANCESTOR_TWO_PERSON_BY_ID_QUERY =
            "g.V(['${person1_id}', '${person2_id}']).as('start')" +
            ".in('%s').dedup().as('parent')" +
            ".in('%s').dedup().as('grandparent')" +
            ".project('children', 'ancestor')" +
            ".by(select('start').values('name').fold())" +
            ".by(coalesce(" +
                "select('parent').valueMap('name', 'profession', 'celebrity_id').by(unfold())," +
                "select('grandparent').valueMap('name', 'profession', 'celebrity_id').by(unfold())" +
            "))";

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

        // 家庭关系过滤的共同祖先查询模板 - 只使用e_type属性
        public static final String FAMILY_COMMON_ANCESTOR_GRANDPARENT_QUERY = 
            "g.V().has('%s', 'name', '${person1}').as('p1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('grandparent1')" +
            ".V().has('%s', 'name', '${person2}').as('p2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('grandparent2')" +
            ".where('grandparent1', eq('grandparent2'))" +
            ".select('p1', 'p2', 'grandparent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('name', 'profession', 'celebrity_id').by(unfold()))" +
            ".dedup()";

        public static final String FAMILY_COMMON_ANCESTOR_PARENT_QUERY = 
            "g.V().has('%s', 'name', '${person1}').as('p1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent1')" +
            ".V().has('%s', 'name', '${person2}').as('p2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent2')" +
            ".where('parent1', eq('parent2'))" +
            ".select('p1', 'p2', 'parent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('name', 'profession', 'celebrity_id').by(unfold()))" +
            ".dedup()";

        // 调试查询 - 检查某人的所有关系类型
        public static final String DEBUG_PERSON_RELATIONSHIPS_QUERY = 
            "g.V().has('%s', 'name', '${person}')" +
            ".bothE('%s')" +
            ".project('direction', 'otherPerson', 'edgeProperties')" +
            ".by(choose(inV().has('%s', 'name', '${person}'), constant('incoming'), constant('outgoing')))" +
            ".by(otherV().values('name'))" +
            ".by(valueMap())";
}
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
        public static final String RELATION_CHAIN_QUERY = "g.V().hasLabel('%s').where(values('name').is(within([${sourceName}])))" +
                        ".repeat(both('%s')" +
                        ".simplePath()" +  // 避免环路
                        ".where(without('visited'))" +
                        ".aggregate('visited'))" +
                        ".until(hasLabel('%s').where(values('name').is(within([${targetName}]))).or().loops().is(%d))" +
                        ".hasLabel('%s').where(values('name').is(within([${targetName}])))" +  // 确保最后一个节点是目标
                        ".path()" +
                        ".by(values('name'))" +  // 只返回名字，简洁输出
                        ".order().by(count(local))" +  // 按路径长度排序
                        ".limit(1)" +  // 只返回最短的路径
                        ".project('path', 'length')" +  // 返回路径和长度
                        ".by(identity())" +  // 保持路径原样
                        ".by(count(local).math('_ - 1'))";  // 计算中间经过的人数（路径长度减1）

        public static final String MUTUAL_FRIEND_QUERY = "g.V().hasLabel('%s').where(values('name').is(within([${name0}])))" +
                        ".both('%s').as('friends')" +
                        ".both('%s').hasLabel('%s').where(values('name').is(within([${name1}])))" +
                        ".select('friends')" +
                        ".dedup()" +
                        ".values('name')";

        public static final String DREAM_TEAM_QUERY = 
            "g.V().hasLabel('celebrity').where(values('name').is('${name1}'))" +  // 从第一个明星开始
            ".out('celebrity_work').as('work')" +  // 进入作品节点
            ".where(" +
            "__.in('celebrity_work')" +  // 查找所有参与该作品的明星
            ".where(values('name').is(within(${other_names})))" +  // 必须包含列表中的所有其他明星
            ".count().is(${other_names_count})" +  // 确保所有其他明星都参与了
            ")" +
            ".valueMap('title', 'release_date', 'work_type')" +  // 获取作品的核心信息
            ".dedup()";  // 去重

        public static final String SIMILARITY_QUERY = 
            "g.V().hasLabel('celebrity').where(values('name').is(within([${name1}]))).as('p1')" +
            ".union(" +
                // 共同作品
                "out('celebrity_work').as('works')" +
                ".in('celebrity_work').where(values('name').is(within([${name2}])))" +
                ".select('works').valueMap('title', 'release_date', 'work_type')," +
                
                // 共同活动
                "out('celebrity_event').as('events')" +
                ".in('celebrity_event').where(values('name').is(within([${name2}])))" +
                ".select('events').valueMap('event_name', 'event_date', 'event_type')," +
                
                // 共同好友
                "both('celebrity_celebrity').as('friends')" +
                ".both('celebrity_celebrity').where(values('name').is(within([${name2}])))" +
                ".select('friends').valueMap('celebrity_id', 'name', 'profession')" +
            ")";

        public static final String NODES_BY_NAMES_QUERY = "g.V().hasLabel('celebrity').where(values('name').is(within([${names}]))).as('center')"
                        +
                        ".both('celebrity_celebrity').as('partner')" +
                        ".select('center','partner')" +
                        ".by(valueMap('celebrity_id','name','profession'))";

        public static final String EDGES_BY_NAMES_QUERY = "g.V().hasLabel('celebrity').where(values('name').is(within([${names}]))).bothE('celebrity_celebrity')"
                        +
                        ".as('e').otherV().as('other')" +
                        ".select('e','other').by(valueMap()).by(valueMap('name'))";

        public static final String NODES_EDGES_BY_NAMES_QUERY = "g.V().hasLabel('celebrity').where(values('name').is(within([${names}]))).bothE('celebrity_celebrity')";

        // 共同祖先查询模板 - 优先查找祖父母级别的祖先
        public static final String COMMON_ANCESTOR_TWO_PERSON_QUERY = 
            "g.V().hasLabel('%s').where(values('name').is(within([${person1}]))).as('p1')" +
            ".in('%s').in('%s').as('grandparent1')" +  // 查找祖父母
            ".V().hasLabel('%s').where(values('name').is(within([${person2}]))).as('p2')" +
            ".in('%s').in('%s').as('grandparent2')" +  // 查找祖父母
            ".where('grandparent1', eq('grandparent2'))" +
            ".select('p1', 'p2', 'grandparent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('celebrity_id', 'name', 'profession').by(unfold()))" +
            ".dedup()";

        // 如果没找到祖父母级别的，查找父母级别的共同祖先
        public static final String COMMON_ANCESTOR_PARENT_LEVEL_QUERY = 
            "g.V().hasLabel('%s').where(values('name').is(within([${person1}]))).as('p1')" +
            ".in('%s').as('parent1')" +
            ".V().hasLabel('%s').where(values('name').is(within([${person2}]))).as('p2')" +
            ".in('%s').as('parent2')" +
            ".where('parent1', eq('parent2'))" +
            ".select('p1', 'p2', 'parent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('celebrity_id', 'name', 'profession').by(unfold()))" +
            ".dedup()";

        // 添加新的查询模板用于ID查询
        public static final String COMMON_ANCESTOR_TWO_PERSON_BY_ID_QUERY =
            "g.V([${person1_id}, ${person2_id}]).as('start')" +
            ".in('%s').dedup().as('parent')" +
            ".in('%s').dedup().as('grandparent')" +
            ".project('children', 'ancestor')" +
            ".by(select('start').values('name').fold())" +
            ".by(coalesce(" +
                "select('parent').valueMap('celebrity_id', 'name', 'profession').by(unfold())," +
                "select('grandparent').valueMap('celebrity_id', 'name', 'profession').by(unfold())" +
            "))";

        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_PREFIX = "g.V().hasLabel('%s').where(values('name').is(within([${person%d}])))"
                        +
                        ".repeat(__.in('%s').simplePath()).emit().times(%d)" +
                        ".id().fold().as('ancestors%d')";

        public static final String COMMON_ANCESTOR_MULTI_PERSON_QUERY_SUFFIX = "g.V().where(__.id().is(within('ancestors0')))%s"
                        +
                        ".dedup().elementMap()";

        // 默认查询深度
        public static final int DEFAULT_ANCESTOR_DEPTH = 3;
        public static final int MAX_ANCESTOR_DEPTH = 6;

        // 家庭关系过滤的共同祖先查询模板 - 只使用e_type属性
        public static final String FAMILY_COMMON_ANCESTOR_GRANDPARENT_QUERY = 
            "g.V().hasLabel('%s').where(values('name').is('${person1}')).as('p1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('grandparent1')" +
            ".V().hasLabel('%s').where(values('name').is('${person2}')).as('p2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('grandparent2')" +
            ".where('grandparent1', eq('grandparent2'))" +
            ".select('p1', 'p2', 'grandparent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('celebrity_id', 'name', 'profession').by(unfold()))" +
            ".dedup()";

        public static final String FAMILY_COMMON_ANCESTOR_PARENT_QUERY = 
            "g.V().hasLabel('%s').where(values('name').is('${person1}')).as('p1')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent1')" +
            ".V().hasLabel('%s').where(values('name').is('${person2}')).as('p2')" +
            ".inE('%s').where(has('e_type', within(['父子', '母子', '父女', '母女', '亲子', '家人', 'family', '儿子', '女儿', '父亲', '母亲', '爸爸', '妈妈'])))" +
            ".outV().as('parent2')" +
            ".where('parent1', eq('parent2'))" +
            ".select('p1', 'p2', 'parent1')" +
            ".by(values('name'))" +
            ".by(values('name'))" +
            ".by(valueMap('celebrity_id', 'name', 'profession').by(unfold()))" +
            ".dedup()";

        // 调试查询 - 检查某人的所有关系类型
        public static final String DEBUG_PERSON_RELATIONSHIPS_QUERY = 
            "g.V().hasLabel('%s').where(values('name').is('${person}'))" +
            ".bothE('%s')" +
            ".project('direction', 'otherPerson', 'edgeProperties')" +
            ".by(choose(inV().hasLabel('%s').where(values('name').is('${person}')), constant('incoming'), constant('outgoing')))" +
            ".by(otherV().values('name'))" +
            ".by(valueMap())";
}
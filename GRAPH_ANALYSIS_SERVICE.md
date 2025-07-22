# Graph Analysis Service 图分析服务

本服务新增了四个核心图分析功能，每个功能调用时都会自动将结果缓存到MySQL数据库中。所有方法都已重新实现，不依赖于GraphServiceOptimized，返回统一的关系图格式。

## 新增功能

### 1. mutualFriend - 共同好友查询
- **功能**: 查询两个明星之间的共同好友，返回关系图
- **接口**: `/mcp/analysis/mutual_friend`
- **参数**: 
  ```json
  {
    "names": ["明星1", "明星2"]
  }
  ```

### 2. dreamTeam - 共同作品查询
- **功能**: 查询多个明星共同参演的电影，返回合作作品关系图
- **接口**: `/mcp/analysis/dream_team`
- **参数**: 
  ```json
  {
    "names": ["明星1", "明星2", "明星3"]
  }
  ```

### 3. similarity - 相似度分析
- **功能**: 查询多个明星之间的相似度，基于指定关系类型
- **接口**: `/mcp/analysis/similarity`
- **参数**: 
  ```json
  {
    "names": ["周星驰", "吴孟达"],
    "relationshipType": "合作"
  }
  ```

### 4. commonAncestor - 共同祖先查询
- **功能**: 查询多个明星之间最近共同祖先，返回关系图
- **接口**: `/mcp/analysis/common_ancestor`
- **参数**: 
  ```json
  {
    "names": ["明星1", "明星2"],
    "maxDepth": 3
  }
  ```

## 数据库缓存

所有分析结果都会自动保存到MySQL数据库的`graph-cache`表中：

### 表结构
- `id`: 主键ID
- `session_id`: 会话ID (UUID生成)
- `thread_id`: 线程ID (UUID生成) 
- `content`: 分析结果内容(TEXT)
- `created_at`: 创建时间
- `updated_at`: 更新时间

### 特性
- 每次调用自动生成UUID作为session_id和thread_id
- 支持按会话、线程查询缓存历史记录
- 自动记录创建和更新时间

## 技术实现

### 核心组件
1. **GraphCache** - JPA实体类
2. **GraphCacheRepository** - 数据访问层
3. **GraphCacheService** - 缓存服务层，严格要求传入sessionId和threadId
4. **GraphAnalysisService** - 图分析服务层，完全重写，直接使用Gremlin查询
5. **GraphAnalysisHandler** - REST控制器，自动生成UUID

### 重要变更
- ✅ **GraphAnalysisService完全重写**: 不再依赖GraphServiceOptimized
- ✅ **统一返回格式**: 所有方法返回结构化的关系图JSON
- ✅ **参数化缓存**: 每个方法都需要传入sessionId和threadId
- ✅ **UUID生成位置**: 在Handler层生成，Service层接收参数

### 方法签名更新
```java
// 所有方法都需要sessionId和threadId参数
public String mutualFriend(List<String> names, String sessionId, String threadId)
public String dreamTeam(List<String> names, String sessionId, String threadId) 
public String similarity(List<String> names, String relationshipType, String sessionId, String threadId)
public String commonAncestor(List<String> names, Integer maxDepth, String sessionId, String threadId)
public Mono<Map<String, Object>> findCommonAncestorByNames(String person1, String person2, String sessionId, String threadId)
```

### 返回数据格式
所有方法返回统一的JSON格式：
```json
{
  "query_type": "mutual_friend|dream_team|similarity|common_ancestor",
  "persons": ["明星1", "明星2"],
  "data_field": [...], // 具体数据字段
  "status": "success|no_result|error"
}
```

### 依赖配置
已自动添加到`pom.xml`:
- `spring-boot-starter-data-jpa`
- `mysql-connector-java`

### 数据库配置
已配置在`application.yml`:
- 连接URL: `jdbc:mysql://192.168.3.78:3307/graph-agent`
- 用户名/密码: 支持环境变量配置
- 连接池: HikariCP优化配置

## 使用说明

1. 确保MySQL数据库运行并且`graph-cache`表已创建
2. 启动应用服务
3. 调用分析接口进行图分析
4. 分析结果会自动缓存到数据库
5. 可通过GraphCacheService查询历史缓存记录

## 架构优势

1. **独立实现**: GraphAnalysisService不依赖其他服务，降低耦合
2. **一致性**: 所有返回结果格式统一，便于前端处理
3. **可追踪性**: 每次调用都有唯一的sessionId和threadId
4. **灵活缓存**: 支持按会话和线程查询历史记录
5. **错误处理**: 完善的异常处理和错误缓存机制
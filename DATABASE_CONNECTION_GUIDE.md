# 数据库连接恢复指南

## 当前状态

✅ **JPA配置已恢复**: 移除了exclude配置
✅ **编译成功**: 所有代码正常编译
⚠️ **数据库连接待测试**: 需要验证MySQL连接参数

## 数据库配置

当前配置的连接参数：
```yaml
datasource:
  url: jdbc:mysql://192.168.3.78:3307/graph-agent
  username: ${DB_USERNAME:graph_user}  # 默认: graph_user
  password: ${DB_PASSWORD:graph_password}  # 默认: graph_password
```

## 连接问题排查

### 1. 检查MySQL服务状态
```bash
# 检查MySQL服务是否运行
telnet 192.168.3.78 3307
```

### 2. 验证用户权限
需要确保MySQL用户具有以下权限：
- 连接权限：`graph_user@'%'` 或 `graph_user@'192.168.3.57'`
- 数据库权限：对 `graph-agent` 数据库的SELECT, INSERT, UPDATE, DELETE权限

### 3. 可能的解决方案

#### 方案A: 使用环境变量
```bash
export DB_USERNAME=your_actual_username
export DB_PASSWORD=your_actual_password
mvn spring-boot:run
```

#### 方案B: 修改默认配置
在`application.yml`中直接修改用户名密码：
```yaml
datasource:
  username: your_actual_username
  password: your_actual_password
```

#### 方案C: 创建正确的MySQL用户
```sql
-- 在MySQL中执行
CREATE USER 'graph_user'@'%' IDENTIFIED BY 'graph_password';
GRANT ALL PRIVILEGES ON `graph-agent`.* TO 'graph_user'@'%';
FLUSH PRIVILEGES;
```

## 测试缓存功能

当数据库连接成功后，可以通过以下方式测试缓存：

1. **启动应用**
2. **调用任意图分析API**，例如：
   ```bash
   curl -X POST http://localhost:5821/mcp/analysis/mutual_friend \
   -H "Content-Type: application/json" \
   -d '{"names": ["明星1", "明星2"]}'
   ```

3. **检查数据库**：
   ```sql
   SELECT * FROM `graph-cache` ORDER BY created_at DESC;
   ```

## 验证成功标志

✅ **应用正常启动**（无JDBCConnectionException）
✅ **API调用成功**
✅ **graph-cache表中有新数据**，包含：
   - session_id (UUID)
   - thread_id (UUID) 
   - content (JSON格式的分析结果)
   - created_at, updated_at

## 故障排除

如果仍然无法连接数据库，请检查：
1. MySQL服务器是否运行在`192.168.3.78:3307`
2. `graph-agent`数据库是否存在
3. `graph-cache`表是否已创建（使用提供的SQL脚本）
4. 网络连接是否正常
5. 防火墙设置是否阻止连接
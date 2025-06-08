#!/bin/bash

# 远程图数据库服务地址
BASE_URL="http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query"

# 构建查询共同好友的Gremlin查询
# 查询周杰伦和刘德华的共同好友
GREMLIN_QUERY="g.V().has('celebrity', 'name', within(['周杰伦','刘德华'])).both('celebrity_celebrity').where(both('celebrity_celebrity').has('celebrity', 'name', within(['周杰伦','刘德华']))).valueMap('celebrity_id', 'name', 'education', 'birthdate', 'position', 'profession', 'gender', 'company', 'nationality')"

# 构建请求体
REQUEST_BODY=$(cat <<EOF
{
  "content": "$GREMLIN_QUERY"
}
EOF
)

echo "查询周杰伦和刘德华的共同好友..."
echo "发送Gremlin查询到远程图数据库..."
echo ""

# 发送请求
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_BODY" \
  | jq '.data.json_view.data' 2>/dev/null || cat

echo ""
echo "查询完成" 
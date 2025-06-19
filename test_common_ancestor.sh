#!/bin/bash

# 测试共同祖先查询功能
echo "=== 测试共同祖先查询功能 ==="

BASE_URL="http://localhost:8080"

# 测试1: 两人共同祖先（默认深度）
echo "测试1: 查询刘德华和周杰伦的共同祖先（默认3层）"
curl -X POST "${BASE_URL}/mcp/most_recent_common_ancestor" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["刘德华", "周杰伦"]
  }' | jq .

echo -e "\n" 

# 测试2: 两人共同祖先（指定深度）
echo "测试2: 查询刘德华和周杰伦的共同祖先（5层深度）"
curl -X POST "${BASE_URL}/mcp/most_recent_common_ancestor" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["刘德华", "周杰伦"],
    "maxDepth": 5
  }' | jq .

echo -e "\n"

# 测试3: 三人共同祖先
echo "测试3: 查询刘德华、周杰伦、周星驰的共同祖先"
curl -X POST "${BASE_URL}/mcp/most_recent_common_ancestor" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["刘德华", "周杰伦", "周星驰"],
    "maxDepth": 4
  }' | jq .

echo -e "\n"

# 测试4: 测试不存在的名字
echo "测试4: 查询不存在名字的共同祖先"
curl -X POST "${BASE_URL}/mcp/most_recent_common_ancestor" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["不存在的人1", "不存在的人2"]
  }' | jq .

echo -e "\n=== 测试完成 ===" 
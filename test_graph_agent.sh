#!/bin/bash

echo "=== 图谱Agent智能查询测试 ==="
echo ""

# Agent服务地址
AGENT_URL="http://localhost:5821/agent"

# 测试1: 查询共同好友
echo "1. 测试自然语言查询 - 共同好友"
echo "查询: '周杰伦和刘德华的共同好友'"
curl -X POST "${AGENT_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "周杰伦和刘德华的共同好友"
  }' | jq .

echo ""
echo "---"
echo ""

# 测试2: 查询关系链
echo "2. 测试自然语言查询 - 关系链"
echo "查询: '周杰伦与刘德华怎么认识'"
curl -X POST "${AGENT_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "周杰伦与刘德华怎么认识"
  }' | jq .

echo ""
echo "---"
echo ""

# 测试3: 查询合作信息
echo "3. 测试自然语言查询 - 合作信息"
echo "查询: '周杰伦和刘德华合作的电影'"
curl -X POST "${AGENT_URL}/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "周杰伦和刘德华合作的电影"
  }' | jq .

echo ""
echo "---"
echo ""

# 测试4: 获取查询示例
echo "4. 获取支持的查询示例"
curl -X GET "${AGENT_URL}/examples" | jq .

echo ""
echo "=== 测试完成 ===" 
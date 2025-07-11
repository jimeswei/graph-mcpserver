#!/bin/bash

# HTTP测试脚本 - 相似度查询API

BASE_URL="http://localhost:5821"
API_ENDPOINT="/mcp/similarity_between_stars"

echo "=========================================="
echo "          相似度API HTTP测试"
echo "=========================================="

# 测试1: 正常请求 - 周星驰和吴孟达
echo ""
echo "测试1: 正常请求 - 周星驰和吴孟达 (合作关系)"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [\"周星驰\", \"吴孟达\"], \"relationshipType\": \"合作\"}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达"],
    "relationshipType": "合作"
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达"],
    "relationshipType": "合作"
  }'

echo ""
echo "----------------------------------------"

# 测试2: 正常请求 - 成龙和李连杰
echo ""
echo "测试2: 正常请求 - 成龙和李连杰 (演出关系)"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [\"成龙\", \"李连杰\"], \"relationshipType\": \"演出\"}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["成龙", "李连杰"],
    "relationshipType": "演出"
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["成龙", "李连杰"],
    "relationshipType": "演出"
  }'

echo ""
echo "----------------------------------------"

# 测试3: 三个人名
echo ""
echo "测试3: 多人查询 - 周星驰、吴孟达、梁朝伟"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [\"周星驰\", \"吴孟达\", \"梁朝伟\"], \"relationshipType\": \"合作\"}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达", "梁朝伟"],
    "relationshipType": "合作"
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达", "梁朝伟"],
    "relationshipType": "合作"
  }'

echo ""
echo "----------------------------------------"

# 测试4: 错误情况 - 只有一个人名
echo ""
echo "测试4: 错误情况 - 参数不足（只有一个人名）"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [\"成龙\"], \"relationshipType\": \"演出\"}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["成龙"],
    "relationshipType": "演出"
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["成龙"],
    "relationshipType": "演出"
  }'

echo ""
echo "----------------------------------------"

# 测试5: 错误情况 - 空的人名列表
echo ""
echo "测试5: 错误情况 - 空的人名列表"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [], \"relationshipType\": \"合作\"}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": [],
    "relationshipType": "合作"
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": [],
    "relationshipType": "合作"
  }'

echo ""
echo "----------------------------------------"

# 测试6: 默认关系类型（null）
echo ""
echo "测试6: 默认关系类型（null） - 应该使用默认值'合作'"
echo "请求: POST $BASE_URL$API_ENDPOINT"
echo "参数: {\"names\": [\"周星驰\", \"吴孟达\"], \"relationshipType\": null}"
echo ""

curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达"],
    "relationshipType": null
  }' | jq '.' 2>/dev/null || curl -X POST "$BASE_URL$API_ENDPOINT" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["周星驰", "吴孟达"],
    "relationshipType": null
  }'

echo ""
echo "=========================================="
echo "               测试完成"
echo "=========================================="
#!/bin/bash

# 测试新格式的相似度API

echo "========================================"
echo "    测试新格式的相似度API响应"
echo "========================================"

echo ""
echo "测试: 刘德华 + 周杰伦 (搭档关系)"
echo "期望: 返回包含vertices和edges的JSON结构"
echo ""

response=$(curl -s -X POST "http://localhost:5821/mcp/similarity_between_stars" \
  -H "Content-Type: application/json" \
  -d '{
    "names": ["刘德华", "周杰伦"],
    "relationshipType": "搭档"
  }')

echo "完整响应:"
echo "$response"

echo ""
echo "----------------------------------------"

echo ""
echo "提取最终data字段:"
final_data=$(echo "$response" | grep '"status":"COMPLETED"' | jq -r '.data')
echo "$final_data"

echo ""
echo "格式化显示:"
echo "$final_data" | jq '.' 2>/dev/null || echo "解析JSON失败"

echo ""
echo "========================================"
#!/bin/bash

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 查询周杰伦和刘德华的共同好友
echo "查询周杰伦和刘德华的共同好友..."
curl -X POST http://localhost:5821/mutualFriend \
  -H "Content-Type: application/json" \
  -d '{"content": ["周杰伦", "刘德华"]}' \
  | jq .

echo ""
echo "查询完成" 
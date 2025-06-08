#!/bin/bash

echo "查询周杰伦和刘德华的共同好友..."

curl -X POST 'http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query' \
  -H 'Content-Type: application/json' \
  -d '{
    "content": "g.V().has('\''celebrity'\'', '\''name'\'', within(['\''周杰伦'\'','\''刘德华'\''])).both('\''celebrity_celebrity'\'').where(both('\''celebrity_celebrity'\'').has('\''celebrity'\'', '\''name'\'', within(['\''周杰伦'\'','\''刘德华'\'']))).valueMap('\''celebrity_id'\'', '\''name'\'', '\''education'\'', '\''birthdate'\'', '\''position'\'', '\''profession'\'', '\''gender'\'', '\''company'\'', '\''nationality'\'')"
  }' | python3 -m json.tool 
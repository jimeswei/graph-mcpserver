#!/usr/bin/env python3
import requests
import json

# 远程图数据库服务地址
BASE_URL = "http://192.168.3.78:28080/api/v1.2/graph-connections/1/gremlin-query"

def query_mutual_friends(names):
    """查询共同好友"""
    # 构建名称列表字符串
    names_str = "'" + "','".join(names) + "'"
    
    # 构建Gremlin查询
    gremlin_query = f"g.V().has('celebrity', 'name', within([{names_str}])).both('celebrity_celebrity').where(both('celebrity_celebrity').has('celebrity', 'name', within([{names_str}]))).valueMap('celebrity_id', 'name', 'education', 'birthdate', 'position', 'profession', 'gender', 'company', 'nationality')"
    
    # 构建请求体
    request_body = {
        "content": gremlin_query
    }
    
    print(f"查询 {names[0]} 和 {names[1]} 的共同好友...")
    print(f"发送Gremlin查询: {gremlin_query[:100]}...")
    
    try:
        # 发送请求
        response = requests.post(
            BASE_URL,
            headers={"Content-Type": "application/json"},
            json=request_body
        )
        
        if response.status_code == 200:
            data = response.json()
            # 提取结果
            if 'data' in data and 'json_view' in data['data'] and 'data' in data['data']['json_view']:
                results = data['data']['json_view']['data']
                
                print(f"\n找到 {len(results)} 个共同好友：\n")
                
                for friend in results:
                    # 提取名字
                    name = friend.get('name', ['未知'])[0] if isinstance(friend.get('name'), list) else friend.get('name', '未知')
                    profession = friend.get('profession', ['未知'])[0] if isinstance(friend.get('profession'), list) else friend.get('profession', '未知')
                    company = friend.get('company', ['未知'])[0] if isinstance(friend.get('company'), list) else friend.get('company', '未知')
                    
                    print(f"姓名: {name}")
                    print(f"职业: {profession}")
                    print(f"公司: {company}")
                    print("-" * 40)
            else:
                print("未找到共同好友")
        else:
            print(f"请求失败，状态码: {response.status_code}")
            print(f"响应: {response.text}")
            
    except Exception as e:
        print(f"查询出错: {str(e)}")

if __name__ == "__main__":
    # 查询周杰伦和刘德华的共同好友
    query_mutual_friends(["周杰伦", "刘德华"]) 
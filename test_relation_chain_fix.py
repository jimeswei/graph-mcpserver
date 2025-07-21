#!/usr/bin/env python3
import requests
import json
import time

def test_relation_chain_fix():
    """测试关系链查询人名单引号修复效果"""
    
    # 等待应用启动
    print("⏳ 等待应用程序启动...")
    time.sleep(10)
    
    url = "http://localhost:5821/mcp/similarity_between_stars"
    
    test_data = {
        "names": ["刘德华", "周杰伦"],
        "relationshipType": "好友"
    }
    
    print("🔧 测试关系链查询人名单引号修复")
    print("=" * 60)
    print(f"请求 URL: {url}")
    print(f"请求数据: {json.dumps(test_data, ensure_ascii=False, indent=2)}")
    print("-" * 60)
    
    try:
        response = requests.post(url, 
                               json=test_data, 
                               timeout=30)
        
        print(f"响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            print("✅ API调用成功!")
            print("响应内容:")
            print("-" * 40)
            
            try:
                # 解析JSON响应
                data = response.json()
                print(json.dumps(data, ensure_ascii=False, indent=2))
                
                # 检查是否有真实数据
                if 'data' in data and data['data'] and len(data['data']) > 0:
                    print("\n✅ 成功获取到图数据库返回的真实数据！")
                    print(f"   找到 {len(data['data'])} 条记录")
                else:
                    print("\n⚠️  响应成功但数据为空，可能是查询条件没有匹配到数据")
                    
            except json.JSONDecodeError:
                print("响应内容（非JSON格式）:")
                print(response.text)
                
        else:
            print(f"❌ API调用失败!")
            print("错误信息:")
            print(response.text)
            
    except requests.exceptions.Timeout:
        print("❌ 请求超时!")
    except requests.exceptions.ConnectionError:
        print("❌ 连接错误! 请确保应用程序正在运行在端口5821")
    except Exception as e:
        print(f"❌ 测试异常: {e}")

if __name__ == "__main__":
    test_relation_chain_fix() 
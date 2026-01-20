import requests
import json
import time

URL = "http://localhost:8888/memories"

def final_test():
    # 1. 增加 timeout 到 30 秒，因为 LLM 提取比较慢
    payload = {
        "messages": [{"role": "user", "content": "我的信息工程导师推荐我去参加那个 OPPO 比赛的"}],
        "user_id": "xwj_student"
    }
    
    print("⏳ 正在发送请求（包含 LLM 提取，可能较慢）...")
    try:
        start_time = time.time()
        # 调大 timeout
        resp = requests.post(URL, json=payload, timeout=30)
        end_time = time.time()
        
        print(f"✅ Docker 响应成功！耗时: {end_time - start_time:.2f}秒")
        print("响应内容:", resp.json())
        
        # 2. 紧接着做一个 GET 请求验证数据是否真的在里面了
        print("\n🔍 正在从 Docker 数据库检索刚才的记忆...")
        # 注意：根据文档，查询通常是 GET /memories?user_id=...
        get_resp = requests.get(f"{URL}?user_id=xwj_student", timeout=10)
        print("当前用户所有记忆:", json.dumps(get_resp.json(), indent=2, ensure_ascii=False))
        
    except Exception as e:
        print(f"❌ 还是出错了: {e}")

if __name__ == "__main__":
    final_test()
import requests
import json

URL = "http://localhost:8888/search"

def final_search_test():
    payload = {
        "query": "关于 OPPO 比赛和我的专业有什么信息？",
        "user_id": "xwj_student",
        "limit": 5
    }

    try:
        response = requests.post(URL, json=payload, timeout=15)
        data = response.json() # 此时 data 是一个字典

        # 1. 提取语义记忆 (Vector Search Results)
        memories = data.get('results', [])
        print(f"✅ 找到 {len(memories)} 条相关语义记忆：")
        for item in memories:
            content = item.get('memory')
            score = item.get('score')
            print(f"  - [{score:.4f}] {content}")

        # 2. 提取图谱关联 (Graph Relations) —— 重点！
        relations = data.get('relations', [])
        if relations:
            print(f"\n🕸️ 发现图谱关联串联：")
            for rel in relations:
                s = rel.get('source')
                r = rel.get('relationship')
                d = rel.get('destination')
                print(f"  - {s} --({r})--> {d}")

    except Exception as e:
        print(f"❌ 解析出错: {e}")

if __name__ == "__main__":
    final_search_test()
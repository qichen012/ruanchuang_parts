from ddgs import (DDGS)
import time


def search_online_content(keywords, limit=3):
    print(f"📡 [调试模式] 准备搜索: {keywords}")
    results = []

    # ⚠️ 如果你有代理，请填这里，没有则留 None
    # proxies = "http://127.0.0.1:7890"
    proxies = None

    # 实例化 DDGS
    with DDGS(proxy=proxies, timeout=20) as ddgs:
        for key in keywords:
            if len(results) >= limit: break

            print(f"   -> 正在尝试搜索单词: '{key}' ...", end=" ")

            try:
                # 【关键修改 1】 backend='html': 使用网页后端，抗封锁能力更强
                # 【关键修改 2】 region='wt-wt': 不限制地区，防止因没中文结果而返回空
                gen = ddgs.text(
                    key,
                    region='wt-wt',  # 尝试改为全球范围
                    backend='html',  # 强制使用 html 后端
                    max_results=3
                )

                # 转换生成器为列表，检测是否有内容
                items = list(gen)

                if not items:
                    print("❌ 空结果 (DuckDuckGo 没返回数据)")
                else:
                    print(f"✅ 成功! 获取到 {len(items)} 条")
                    for res in items:
                        results.append({
                            "title": res['title'],
                            "url": res['href'],
                            "snippet": res['body']
                        })
                        if len(results) >= limit: break

            except Exception as e:
                # 【关键修改 3】 打印具体报错信息
                print(f"❌ 报错: {type(e).__name__} - {e}")

            # 稍微停顿一下，防止请求过快被封
            time.sleep(1)

    return results


# --- 运行测试 ---
if __name__ == "__main__":
    # 测试关键词
    test_keywords = ['Tech', 'Python', 'Coding']

    recs = search_online_content(test_keywords)

    print("\n🎉 最终结果：")
    for item in recs:
        print(f"-> {item['title']}")
        print(f"   {item['url']}\n")
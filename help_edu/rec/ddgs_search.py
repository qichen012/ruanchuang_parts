from ddgs import DDGS
import time


def search_ddgs(keywords, limit=3):
    """
    修正版: 修复参数拼写错误 (backend='html')
    """
    print(f"📡 [DuckDuckGo] 收到关键词列表: {keywords}")

    results = []

    try:
        # timeout 设置为 20 秒，给网络更多缓冲
        with DDGS(timeout=20) as ddgs:

            # 遍历列表中的每一个词 (例如: 先搜 'Tech', 再搜 'Python')
            for key in keywords:
                # 如果结果够了，就停止
                if len(results) >= limit:
                    break

                print(f"   -> 正在尝试搜索单词: '{key}' ...", end=" ")

                try:
                    # 【关键修正】 backend='html' (单数!)
                    # region='wt-wt' (全球搜索，避免中文区无结果)
                    gen = ddgs.text(
                        key,
                        region='zh-CN',
                        max_results=3,
                        backend='html',
                        timelimit='w'  # 'd' 表示过去一天,'w'表示过去一周,'m'表示过去一个月,''表示不限制时间
                    )

                    # 转换生成器，获取数据
                    found_items = list(gen)

                    if found_items:
                        print(f"✅ 获取到 {len(found_items)} 条")
                        for res in found_items:
                            results.append({
                                "title": res['title'],
                                "url": res['href'],
                                "snippet": res['body']
                            })
                            if len(results) >= limit: break
                    else:
                        print("⚠️ 无结果")

                except Exception as inner_e:
                    print(f"⚠️ 跳过 (原因: {inner_e})")

                # 稍微停顿，防止请求过快被封
                time.sleep(1)

    except Exception as e:
        print(f"❌ 严重错误: {e}")
        return [
            {"title": "本地保底数据：Python 学习路线", "url": "#", "snippet": "网络不可用..."},
            {"title": "本地保底数据：2026 科技趋势", "url": "#", "snippet": "网络不可用..."}
        ]

    return results
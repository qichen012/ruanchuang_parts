import feedparser
import requests  # 引入 requests 用于伪装


def search_rss(keywords,limit=10):
    print(f"📡 [RSS启动] 正在扫描: {keywords}")
    print("-" * 40)

    KEYWORD_MAP = {
        'Tech': ['科技', '技术', '数码', 'AI', '模型', 'Tech'],
        'Coding': ['编程', '代码', '开发', '程序员', '算法', 'Coding'],
        'Python': ['Python', '爬虫', '数据分析', '脚本'],
        'Hardware': ['硬件', '显卡', '芯片', '处理器', 'Hardware'],
        'Music': ['音乐', '歌曲', 'Music'],
        'Jazz': ['爵士', 'Jazz']
    }

    # 1. 优化源列表：加入一些对开发者更友好的源 (IT之家, OSChina)
    # 36Kr 如果反爬太严，建议暂时注释掉
    rss_sources = [
        {"name": "OSChina", "url": "https://www.oschina.net/news/rss"},  # 编程必中
        {"name": "少数派", "url": "https://sspai.com/feed"},
        {"name": "IT之家", "url": "https://www.ithome.com/rss/"},
        {"name": "36氪", "url": "https://www.36kr.com/feed"},
    ]

    # 2. 浏览器伪装头 (关键！)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }

    results = []

    for src in rss_sources:
        try:
            # print(f"   -> 连接 [{src['name']}]...", end=" ")
            response = requests.get(src['url'], headers=headers, timeout=5)
            feed = feedparser.parse(response.text)

            if not feed.entries:
                continue

            # print(f"✅")

            found_in_source = False

            for entry in feed.entries:
                title = entry.title
                link = entry.link

                # ==========================================
                # 2. 核心修改：双重循环匹配
                # ==========================================
                # 遍历用户给的每一个英文关键词 (比如 'Tech')
                for user_key in keywords:

                    # 获取这个英文词对应的中文同义词列表
                    # 如果字典里没有，就只搜它自己 [user_key]
                    search_terms = KEYWORD_MAP.get(user_key, [user_key])

                    # 检查标题里是否包含同义词列表里的【任意一个】
                    # 例如：标题是 "2026年科技趋势"
                    # user_key是"Tech" -> search_terms是['科技', 'Tech'...] -> 命中 '科技'！
                    if any(term.lower() in title.lower() for term in search_terms):
                        results.append({
                            "title": title,
                            "url": link,
                            "source": src['name'],
                            "match_reason": f"{user_key} -> 命中词"  # 记录一下是因为谁命中的
                        })
                        found_in_source = True

                        # 打印出来让我们爽一下
                        print(f"✅ [{src['name']}] 命中: {title[:20]}...")
                        print(f"   (原词: {user_key} -> 扩展搜索: {search_terms})")
                        break  # 这篇文章匹配上了，就跳出关键词循环，看下一篇文章

                if len(results) >= limit:
                    print("-" * 50)
                    return results

            # 保底逻辑 (略微简化，只在没找到时触发)
            if not found_in_source and len(feed.entries) > 0:
                # print(f"   ⚠️ {src['name']} 无匹配，跳过...")
                pass

        except Exception as e:
            print(f"❌ {src['name']} 出错: {e}")
            continue

    print("-" * 50)
    # 如果实在还是空的，最后再加个硬保底
    if not results:
        print("⚠️ 还是没搜到，可能关键词太偏。返回IT之家最新头条作为兜底。")
        try:
            feed = feedparser.parse("https://www.ithome.com/rss/")
            if feed.entries:
                return [{"title": feed.entries[0].title, "url": feed.entries[0].link, "source": "兜底推荐",
                         "match_reason": "无匹配"}]
        except:
            pass

    return results

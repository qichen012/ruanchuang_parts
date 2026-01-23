import feedparser
import requests
import random


def search_rss(keywords,limit=10):
    print(f"📡 [RSS启动] 正在扫描: {keywords}")
    print("-" * 40)


    KEYWORD_MAP = {
        # --------------------------------------
        # 1. 核心技术栈 (Development & Coding)
        # --------------------------------------
        'Coding':   ['编程', '代码', '开发', '程序员', '源码', 'Coding'],
        'Python':   ['Python', '爬虫', '数据分析', 'Pandas', '自动化', '脚本'],
        'Java':     ['Java', 'Spring', 'JVM', '后端', '微服务', '并发'],
        'Go':       ['Go语言', 'Golang', '云原生', '微服务', 'Gin'],
        'Frontend': ['前端', 'Web', 'JavaScript', 'Vue', 'React', 'CSS', 'Node'],
        'Algorithm':['算法', '数据结构', 'LeetCode', '深度学习', '模型训练'],

        # --------------------------------------
        # 2. 前沿科技 (High Tech & AI)
        # --------------------------------------
        'Tech':     ['科技', '技术', '黑科技', '创新', 'Tech'],
        'AI':       ['AI', '人工智能', '大模型', 'ChatGPT', '生成式', 'AIGC', 'LLM'],
        'Data':     ['大数据', '数据库', 'SQL', '数据挖掘', 'BI', '数据可视化'],
        'Cloud':    ['云计算', '服务器', 'AWS', '阿里云', 'Docker', 'Kubernetes', 'K8s'],

        # --------------------------------------
        # 3. 硬件与数码 (Hardware & Gadgets)
        # --------------------------------------
        'Hardware': ['硬件', '显卡', 'GPU', '芯片', '半导体', '处理器', 'Hardware'],
        'Mobile':   ['手机', '安卓', 'Android', 'iOS', '鸿蒙', 'App', '智能手机'],
        'Apple':    ['Apple', '苹果', 'iPhone', 'Mac', 'iPad', 'Vision Pro'],
        'SmartHome':['智能家居', '米家', 'HomeKit', '物联网', 'IoT', '穿戴设备'],

        # --------------------------------------
        # 4. 职场与商业 (Career & Business)
        # --------------------------------------
        'Career':   ['职场', '面试', '招聘', '简历', '内推', '薪资', '大厂'],
        'Startup':  ['创业', '融资', '独角兽', '商业模式', '出海', 'IPO'],
        'Finance':  ['金融', '理财', '股票', '基金', '经济', 'A股', '美股'],

        # --------------------------------------
        # 5. 泛娱乐与生活 (Lifestyle & Entertainment)
        # --------------------------------------
        'Game':     ['游戏', '电竞', 'Steam', '原神', 'Switch', 'PS5', '黑神话'],
        'Movie':    ['电影', '影视', '票房', '影评', 'Netflix', '剧集'],
        'Music':    ['音乐', '歌曲', '演唱会', '专辑', 'Music'],
        'Jazz':     ['爵士', '蓝调', 'Jazz', '乐理'],
        'Reading':  ['阅读', '书单', '文学', 'kindle', '读书'],
        'Design':   ['设计', 'UI', '交互', '排版', '字体', '素材']
        }

    # 1. 优化源列表：加入一些对开发者更友好的源 (IT之家, OSChina)
    # 36Kr 如果反爬太严，建议暂时注释掉
    rss_sources = [
        # --- 💻 核心编程与技术架构 ---
        # OSChina: 开源技术、综合编程新闻 (覆盖 Coding, Java, Python)
        {"name": "OSChina", "url": "https://www.oschina.net/news/rss"},
        # InfoQ CN: 架构、AI、后端、深度技术文章 (覆盖 Java, Go, AI, Career)
        {"name": "InfoQ", "url": "https://feed.infoq.cn/"},
        # V2EX: 程序员社区热门讨论 (覆盖 Apple, Python, 职场, 酷工作)
        {"name": "V2EX", "url": "https://www.v2ex.com/index.xml"},
        # Solidot: 硬核科技新闻、Linux、开源 (覆盖 Linux, Security)
        {"name": "Solidot", "url": "https://www.solidot.org/index.rss"},

        # --- 📱 数码硬件与评测 ---
        # IT之家: 消费电子、手机、电脑资讯 (覆盖 Hardware, Mobile, Windows)
        {"name": "IT之家", "url": "https://www.ithome.com/rss/"},
        # 爱范儿: 聚焦产品设计、苹果、新硬件 (覆盖 Apple, Design, SmartHome)
        {"name": "爱范儿", "url": "https://www.ifanr.com/feed"},

        # --- 🤖 前沿 AI 与商业 ---
        # 36氪: 创投、商业模式、AI公司融资 (覆盖 Startup, Finance, AI)
        {"name": "36氪", "url": "https://www.36kr.com/feed"},
        # 虎嗅: 深度商业评论 (覆盖 Career, Business)
        {"name": "虎嗅", "url": "https://www.huxiu.com/rss/0.xml"},

        # --- 🎮 游戏与生活 ---
        # 机核网 (G-Cores): 游戏文化、主机游戏 (覆盖 Game, Console, Anime)
        {"name": "机核网", "url": "https://www.gcores.com/rss"},
        # 少数派: 数字生活、效率工具、App推荐 (覆盖 Mobile, Apple, Reading)
        {"name": "少数派", "url": "https://sspai.com/feed"},
    ]

    # 2. 浏览器伪装头 (关键！)
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }

    all_candidates = []

    for src in rss_sources:
        try:
            # print(f"   -> 扫描 [{src['name']}]...", end=" ")
            response = requests.get(src['url'], headers=headers, timeout=4)
            feed = feedparser.parse(response.text)

            if not feed.entries: continue

            # 这里的 count 用来限制每个源最多贡献多少条
            # 设为 limit 意味着：如果运气好，一个源可以包揽所有推荐
            count_per_source = 0

            for entry in feed.entries:
                title = entry.title

                # 关键词匹配
                is_match = False
                for user_key in keywords:
                    search_terms = KEYWORD_MAP.get(user_key, [user_key])
                    if any(term.lower() in title.lower() for term in search_terms):
                        is_match = True
                        break

                if is_match:
                    all_candidates.append({
                        "title": title,
                        "url": entry.link,
                        "source": src['name']
                    })
                    count_per_source += 1

                    # 为了防止某个源太强势（比如IT之家一天发100条），
                    # 我们限制每个源最多只能往大池子里扔 4 条
                    if count_per_source >= 4:
                        break

            # print(f"入围 {count_per_source} 条")

        except Exception as e:
            print(f"❌ {src['name']} 异常: {e}")
            continue

    print(f"📊 候选池共有 {len(all_candidates)} 条新闻")

    # --- 第二步：彻底洗牌 (Shuffle) ---
    # 如果候选池为空，触发保底
    if not all_candidates:
        print("⚠️ 候选池为空，无法随机。启动保底逻辑...")
        # (此处省略保底代码，直接返回空或特定数据)
        return []

    # 【关键】打乱顺序
    random.shuffle(all_candidates)

    # --- 第三步：截取结果 (Slice) ---
    # 如果候选不够 limit，就返回所有；如果够了，就切片
    final_results = all_candidates[:limit]

    print("-" * 50)
    return final_results

import pandas as pd
import numpy as np
from ddgs_search import search_ddgs
from RSS_search import search_rss
import random

# ===========================
# 1. 核心评分配置
# ===========================
WEIGHT_CLICK = 0.7
WEIGHT_TIME = 0.3
MAX_DURATION = 300.0  # 秒


# ===========================
# 2. 第一步：计算内容得分 (和之前一样)
# ===========================
def calculate_scores(logs):
    df = pd.DataFrame(logs)

    # 归一化
    df['time_norm'] = np.clip(df['duration'], 0, MAX_DURATION) / MAX_DURATION
    df['click_norm'] = df['clicks'].clip(upper=5) / 5.0

    # 计算得分
    df['score'] = (df['click_norm'] * WEIGHT_CLICK) + (df['time_norm'] * WEIGHT_TIME)
    df['score'] = (df['score'] * 100).round(2)

    return df


def score_sigmoid(clicks, duration, w_click=0.6, w_time=0.4):
    """
    方案二：Sigmoid 函数
    特点：非线性极强，自动过滤短时误触，自动平滑长时挂机
    """
    # 1. 点击权重：依然推荐用 log，或者简单的分段函数
    # 这里用一种渐进公式：1 - 1/(1+x) -> x越大越接近1
    click_norm = 1 - (1 / (1 + clicks))

    # 2. 时间权重：Sigmoid S型曲线
    # 参数调整技巧：
    # midpoint: 曲线的中心点 (比如 60秒)，在此处分数为 0.5
    # slope: 曲线的陡峭程度 (比如 0.05)，越小越平缓
    midpoint = 60.0
    slope = 0.05

    # Sigmoid 公式
    time_norm = 1 / (1 + np.exp(-slope * (duration - midpoint)))

    # 3. 综合打分
    final_score = (w_click * click_norm + w_time * time_norm) * 100
    return round(final_score, 2)


def select_keywords_with_randomness(user_profile, limit=2, epsilon=0.1):
    """
    输入 user_profile 必须是排好序的列表: [('TagA', 100), ('TagB', 80)...]
    """
    if not user_profile: return []

    selected = [user_profile[0][0]]  # 永远保留第一名
    pool = user_profile[1:]  # 剩下的池子

    while len(selected) < limit and pool:
        if random.random() < epsilon:
            # 随机选
            choice = random.choice(pool)
        else:
            # 选分数最高的
            choice = pool[0]

        selected.append(choice[0])
        pool.remove(choice)

    return selected


# ===========================
# 3. 第二步：生成用户关键词画像 (User Keyword Profile)
# ===========================
def generate_user_keywords(scored_df):
    """
    将内容的分数“过继”给它身上的标签
    """
    keyword_scores = {}

    for _, row in scored_df.iterrows():
        score = row['score']
        tags = row['tags']  # 假设每条内容都有标签列表

        # 简单的线性累加：如果你喜欢这篇文章，你就喜欢它包含的标签
        for tag in tags:
            if tag in keyword_scores:
                keyword_scores[tag] += score
            else:
                keyword_scores[tag] = score

    # 排序：按分数从高到低
    sorted_keywords = sorted(keyword_scores.items(), key=lambda x: x[1], reverse=True)
    return sorted_keywords


# ===========================
# 4. 主程序运行
# ===========================
if __name__ == "__main__":
    # 1. 用户的历史行为 (注意：这里必须包含 tags)
    user_history = [
        # 用户在这个 Python 视频上停留了很久(300s)，点击多
        {'title': 'Python入门', 'clicks': 3, 'duration': 300, 'tags': ['Python', 'Coding','Tech']},
        # 用户在这个 爵士乐 视频上只停留了 10s，误触
        {'title': '爵士乐欣赏', 'clicks': 1, 'duration': 10, 'tags': ['Music', 'Jazz']},
        # 用户看了很久科技新闻
        {'title': '英伟达显卡测评', 'clicks': 1, 'duration': 200, 'tags': ['Tech', 'Hardware']}
    ]

    df_logs = pd.DataFrame(user_history)

    # --- B. 第一步：给“历史文章”打分 ---
    # (这是你原来缺少的步骤)
    print("1. 计算文章得分...")
    df_logs['score'] = score_sigmoid(df_logs['clicks'], df_logs['duration'])
    print(df_logs[['title', 'score']])

    # --- C. 第二步：将分数聚合到“标签” (生成用户画像) ---
    print("\n2. 生成标签画像...")
    tag_scores = {}

    for _, row in df_logs.iterrows():
        # 逻辑：如果你喜欢这篇文章(80分)，那么这篇文章的每一个标签都加80分
        for tag in row['tags']:
            if tag in tag_scores:
                tag_scores[tag] += row['score']
            else:
                tag_scores[tag] = row['score']

    # 转为列表并排序：[('Tech', 159.0), ('Python', 89.0)...]
    user_profile = sorted(tag_scores.items(), key=lambda x: x[1], reverse=True)
    print(f"标签排名: {user_profile}")

    # --- D. 第三步：从标签画像中“随机”选词 ---
    print("\n3. 执行随机策略选词...")
    # 注意：这里传进去的是 user_profile (标签分)，而不是原始 history
    recommend_keywords = select_keywords_with_randomness(user_profile, limit=3, epsilon=0.3)

    print(f"🎉 最终决定去搜这些词: {recommend_keywords}")

    results = search_rss(recommend_keywords)
    for item in results:
        source_name = item.get('source', 'DuckDuckGo')
        print(f"[{source_name}] {item['title']}")
        print(f"链接: {item['url']}\n")



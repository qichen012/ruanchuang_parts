import os
import requests
import time
import json
from generate import extract_keywords_from_report

github_api_key = os.getenv("GITHUB_API_KEY")
headers = {"Authorization": f"Bearer {github_api_key}"} if github_api_key else {}


def get_readme(owner, repo):
    """获取仓库的README内容"""
    url = f"https://api.github.com/repos/{owner}/{repo}/readme"
    response = requests.get(url, headers=headers)
    if response.status_code == 200:
        readme_data = response.json()
        download_url = readme_data.get("download_url")
        if download_url:
            return download_url
        else:
            return "No README found"
    else:
        return f"Error fetching README: {response.status_code}"


def search_repos_by_keyword(keyword, language="python", min_stars=1000, limit=10):
    """根据关键词搜索GitHub仓库（原有功能）"""
    url = "https://api.github.com/search/repositories"
    params = {
        "q": f"{keyword} language:{language} stars:>{min_stars}",
        "sort": "stars",
        "order": "desc",
        "per_page": limit
    }

    try:
        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            results = response.json()
            repos = []
            for repo in results.get("items", [])[:limit]:
                repos.append({
                    "full_name": repo["full_name"],
                    "html_url": repo["html_url"],
                    "description": repo.get("description", ""),
                    "stargazers_count": repo["stargazers_count"],
                    "language": repo.get("language", ""),
                    "owner": repo["owner"]["login"],
                    "name": repo["name"]
                })
                time.sleep(0.5)  # 避免API限流
            return repos
        else:
            print(f"Error: {response.status_code}")
            return []
    except Exception as e:
        print(f"Search error: {e}")
        return []


def search_repos_by_report(report_text, language="", min_stars=1000, limit=10):
    """根据每日简报搜索GitHub仓库（新增功能）"""
    # 1. 提取关键词
    keywords = extract_keywords_from_report(report_text)

    if not keywords:
        print("⚠️ 未能从每日简报中提取到关键词")
        return []

    print(f"🔍 使用关键词搜索: {keywords}")

    # 2. 构建搜索查询
    query_parts = keywords.copy()
    if language:
        query_parts.append(f"language:{language}")

    query = " ".join(query_parts) + f" stars:>{min_stars}"

    url = "https://api.github.com/search/repositories"
    params = {
        "q": query,
        "sort": "stars",
        "order": "desc",
        "per_page": limit
    }

    try:
        response = requests.get(url, headers=headers, params=params)
        if response.status_code == 200:
            results = response.json()
            repos = []
            for repo in results.get("items", [])[:limit]:
                repos.append({
                    "full_name": repo["full_name"],
                    "html_url": repo["html_url"],
                    "description": repo.get("description", ""),
                    "stargazers_count": repo["stargazers_count"],
                    "language": repo.get("language", ""),
                    "owner": repo["owner"]["login"],
                    "name": repo["name"],
                    "keywords_used": keywords  # 记录使用的关键词
                })
                time.sleep(0.5)  # 避免API限流
            return repos
        else:
            print(f"Error: {response.status_code}")
            return []
    except Exception as e:
        print(f"Search error: {e}")
        return []


def search_and_display(keyword, language="python", min_stars=1000, limit=10):
    """搜索并展示结果（保留原有交互方式）"""
    url = "https://api.github.com/search/repositories"
    params = {
        "q": f"{keyword} language:{language} stars:>{min_stars}",
        "sort": "stars",
        "order": "desc",
        "per_page": limit
    }

    response = requests.get(url, headers=headers, params=params)
    if response.status_code == 200:
        for repo in response.json()["items"]:
            print(f"{repo['full_name']} - ⭐ {repo['stargazers_count']}\n{repo['html_url']}")
            time.sleep(1)
            print(get_readme(repo['owner']['login'], repo['name']))
            print("-" * 80)
    else:
        print(f"Error: {response.status_code}")


if __name__ == "__main__":
    # 保留原有的直接运行方式
    language = "python"
    concept = "人工智能"
    sort_type = "stars"
    sort_order = "desc"
    search_and_display(concept, language)

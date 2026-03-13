import os
import requests
import time

github_api_key = os.getenv("GITHUB_API_KEY")
headers = {"Authorization": f"Bearer {github_api_key}"} if github_api_key else {}

language = "python"
concept = "人工智能"
sort_type = "stars"
sort_order = "desc"

def get_readme(owner,repo):
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
    
url = "https://api.github.com/search/repositories"
params = {
    "q": f"{concept} language:{language} stars:>1000",
    "sort": sort_type,
    "order": sort_order,
    "per_page": 10
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
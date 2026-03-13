import os
import requests
from bs4 import BeautifulSoup
import time
from api import get_bocha_api_key


def fetch_page_content(url, timeout=10):
    """抓取网页内容"""
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
        response = requests.get(url, headers=headers, timeout=timeout)
        response.encoding = response.apparent_encoding
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        for s in soup(['script', 'style', 'nav', 'header', 'footer']):
            s.decompose()
        
        text = soup.get_text()
        lines = (line.strip() for line in text.splitlines())
        chunks = (phrase.strip() for line in lines for phrase in line.split('  '))
        text = ' '.join(chunk for chunk in chunks if chunk)
        
        return text[:5000] if text else None
        
    except Exception as e:
        print(f"   抓取失败: {e}")
        return None


def search_news(keywords, limit=5, fetch_full=True):
    """
    检索函数：根据关键词搜索相关新闻（使用博查API）
    
    参数:
        keywords: str 或 list - 搜索关键词
        limit: int - 返回结果数量限制
        fetch_full: bool - 是否抓取完整网页内容（默认False，较慢）
    
    返回:
        list - 新闻结果列表，每个元素包含 title, url, content
    """
    
    print(f"🔍 [博查搜索] 正在检索关键词: {keywords}")
    
    api_key = get_bocha_api_key()
    if not api_key:
        print("❌ 错误：未找到博查API Key，请在 .env 文件中设置 BOCHA_API_KEY")
        return []
    
    url = "https://api.bochaai.com/v1/web-search"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    results = []
    
    for key in keywords:
        if len(results) >= limit:
            break
        
        
        payload = {
            "query": key,
            "summary": True,
            "count": min(limit, 10),
            "page": 1
        }
        
        try:
            response = requests.post(url, headers=headers, json=payload, timeout=20)
            response.raise_for_status()
            data = response.json()
            
            if data.get("code") == 0 or data.get("code") == 200 or data.get("success") is True:
                web_pages = data.get("data", {}).get("webPages", {})
                items = web_pages.get("value", []) if isinstance(web_pages, dict) else web_pages
                
                for item in items:
                    if len(results) >= limit:
                        break
                    
                    title = item.get("name", "无标题")
                    page_url = item.get("url", "#")
                    
                    if fetch_full:
                        print(f"   正在抓取: {title[:30]}...")
                        full_content = fetch_page_content(page_url)
                        content = full_content if full_content else item.get("summary", "无内容")
                    else:
                        content = item.get("summary", "无内容")
                    
                    results.append({
                        "title": title,
                        "url": page_url,
                        "content": content
                    })
                    
                    if fetch_full:
                        time.sleep(1)
                        
                print(f"   -> '{key}' 获取到 {len(items)} 条结果")
            else:
                error_msg = data.get("message", "未知错误")
                print(f"   -> '{key}' API错误: {error_msg}")
                
        except requests.exceptions.Timeout:
            print(f"   -> '{key}' 请求超时")
        except requests.exceptions.RequestException as e:
            print(f"   -> '{key}' 请求失败: {e}")
        except Exception as e:
            print(f"   -> '{key}' 错误: {e}")
    
    print(f"✅ 检索完成，共获取 {len(results)} 条结果")
    return results


if __name__ == "__main__":
    results = search_news("光合作用", limit=2, fetch_full=False)
    for r in results:
        print(f"\n标题: {r['title']}")
        print(f"内容: {r['content'][:300]}...")
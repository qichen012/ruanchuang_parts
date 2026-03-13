import os
import requests
from openai import OpenAI
from bs4 import BeautifulSoup
import time
from api import get_qwen_client



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
        
        return text if text else None
        
    except Exception as e:
        print(f"   抓取失败: {e}")
        return None


def generate_expansion(concept):
    """生成函数：使用Qwen模型进行扩写"""
    if not concept:
        return "未提供课内概念，无法进行扩写。"
    
    
    print(f"📝 [生成] 正在使用Qwen模型生成扩写内容...")
    
    client = get_qwen_client()
    
    url_parts = []
    
    urls_text = "\n\n".join(url_parts)
    
    prompt_text = f"""你是一位教育助手，请根据以下课内概念，写一段联系现实的扩写内容。

课内概念：{concept}

要求：
1. 写一段连贯的文字（100字左右）
2. 简要介绍这个概念的定义
3. 结合搜索到的现实案例说明这个概念在实际中的应用或意义
4. 语言要通俗易懂，适合大学生阅读
5. 在最后标注"参考来源："并列出文章标题

请直接输出扩写内容："""

    prompt_word = f"""你是一个学术搜索助手。请为概念"{concept}"生成 5-8 个搜索关键词。
要求：
1. 包含同义词、专业术语（尽量用中文或英文缩写）、应用场景、前沿概念
2. 仅输出 JSON 数组，不要其他文字
3. 示例：["K-V cache", "自注意力机制", "强化学习"]
        
输出："""
    try:
        if client:
            resp = client.chat.completions.create(
                model="qwen-plus",
                messages=[
                    {"role": "system", "content": "你是一位专业的教育助手，擅长将知识与现实联系起来。"},
                    {"role": "user", "content": prompt_word}
                ],
                temperature=0.7,
                max_tokens=800
            )
            result_text = resp.choices[0].message.content
            result_text += f"\n\n📚 参考来源：\n{urls_text}"
            print("✅ Qwen生成完成")
            return result_text
        else:
            print("⚠️ 未找到Qwen API Key")
            
    except Exception as e:
        print(f"❌ Qwen调用失败: {e}")

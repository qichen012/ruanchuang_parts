import os
from openai import OpenAI
from api import get_qwen_client


def zip_content(text):
    """
    压缩函数：使用Qwen模型将搜索到的的内容简要呈现为一段话
    
    参数:
        text: str - 要压缩的文本
    
    返回:
        str - 简要压缩后的一段话
    """
    if not text:
        return "无内容可供压缩"
    
    print("📦 [压缩] 正在使用Qwen精简内容...")
    
    client = get_qwen_client()
    
    prompt = f"""请将以下新闻精简按照核心要点总结为几段话：

{text}

要求：
1. 只输出几段话，每段话不超过300字，按要点进行分段落，每个段落包含一个要点。
2. 每个段落的要点作为小标题单独列出。
3. 包含概念名称和主要现实联系
4. 语言简洁明了

直接输出精简后的几段话："""

    try:
        if client:
            resp = client.chat.completions.create(
                model="qwen-plus",
                messages=[
                    {"role": "system", "content": "你是一个文字精简助手，擅长将内容压缩为简洁的摘要。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7,
                max_tokens=1000
            )
            result = resp.choices[0].message.content.strip()
            print("✅ 压缩完成")
            return result
        else:
            print("⚠️ 未找到Qwen API Key")
            
    except Exception as e:
        print(f"❌ Qwen调用失败: {e}")
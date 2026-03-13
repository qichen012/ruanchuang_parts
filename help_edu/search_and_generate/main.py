from generate import generate_expansion
from zip import zip_content
from search import search_news


def main(concept, keywords=None, fetch_full=True):
    """
    主函数：整合搜索、生成、压缩三个模块
    
    参数:
        concept: str - 课内概念（必填）
        keywords: str - 搜索关键词（可选，默认使用 concept）
        fetch_full: bool - 是否抓取完整网页内容（可选，默认False）
    
    返回:
        dict - 包含 'full' 和 'zipped' 两个版本的内容
    """
    print("=" * 50)
    print("🚀 学习助手：概念扩写与现实联系")
    print("=" * 50)
    
    if not concept:
        print("❌ 错误：请提供课内概念")
        return None
    
    search_keywords = keywords
    if isinstance(search_keywords, str):
        search_keywords = [search_keywords]
    
    print(f"\n📖 目标概念：{concept}")
    print(f"🔑 搜索内容：{search_keywords}")
    if fetch_full:
        print(f"📄 模式：抓取完整网页内容（较慢）")
    
    print("\n" + "-" * 40)
    print("【第一步】检索相关新闻...")
    print("-" * 40)
    search_results = search_news(search_keywords, limit=5, fetch_full=fetch_full)
    
    if not search_results:
        print("⚠️ 警告：未能获取搜索结果，将使用空结果继续")
    
    print("\n" + "-" * 40)
    print("【第二步】生成扩写内容...")
    print("-" * 40)
    expansion_content = generate_expansion(concept)
    
    print("\n" + "-" * 40)
    print("【第三步】压缩内容...")
    print("-" * 40)
    zipped_content = zip_content(search_results)
    
    print("\n" + "=" * 50)
    print("✅ 处理完成！")
    print("=" * 50)
    
    return {
        "concept": concept,
        "keywords": search_keywords,
        "search_results": search_results,
        "expansion": expansion_content,
        "zipped": zipped_content
    }


def interactive_mode():
    """交互模式"""
    print("\n" + "=" * 50)
    print("🎓 学习助手 - 交互模式")
    print("=" * 50)
    
    concept = input("\n课内概念：").strip()
    
    if not concept:
        print("❌ 未输入概念，程序退出")
        return
    
    # fetch_input = input("是否抓取完整网页内容？(y/n，默认n)：").strip().lower()
    # fetch_full = fetch_input == 'y'
    fetch_full = True
    
    keywords = generate_expansion(concept)

    result = main(concept, keywords, fetch_full)
    
    if result:
        
        print("\n📦 压缩内容：")
        print("-" * 40)
        print(result['zipped'])

if __name__ == "__main__":
    interactive_mode()
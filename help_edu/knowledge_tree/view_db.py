"""
ChromaDB 数据可视化工具
"""
import os
import sys

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(__file__))

from chorma_search import get_chroma_client, get_embedding_function, DEFAULT_CHROMA_PATH


def get_all_collections():
    """获取所有 collection"""
    client = get_chroma_client()
    return client.list_collections()


def view_collection(collection_name: str, limit: int = 100):
    """查看指定 collection 的内容"""
    client = get_chroma_client()
    collection = client.get_or_create_collection(
        name=collection_name,
        embedding_function=get_embedding_function()
    )

    count = collection.count()
    print(f"\n{'='*60}")
    print(f"Collection: {collection_name}")
    print(f"文档数量: {count}")
    print(f"{'='*60}\n")

    if count == 0:
        print("暂无数据")
        return

    # 获取数据
    results = collection.get()
    docs = results.get('documents', [])
    metas = results.get('metadatas', [])
    ids = results.get('ids', [])

    # 显示全部
    for i in range(len(docs)):
        print(f"--- 记录 {i+1} ---")
        print(f"ID: {ids[i]}")
        if metas[i]:
            for k, v in metas[i].items():
                print(f"{k}: {v}")
        print(f"内容:")
        print(docs[i])
        print()


def view_learning_analysis(limit: int = 10):
    """查看学习分析记录"""
    print("\n" + "="*60)
    print("📊 学习分析记录")
    print("="*60)
    view_collection("learning_analysis", limit)


def view_concepts(limit: int = 10):
    """查看知识概念记录"""
    print("\n" + "="*60)
    print("📚 知识概念记录")
    print("="*60)
    view_collection("concepts", limit)


def main():
    print("="*60)
    print("ChromaDB 数据可视化")
    print(f"数据库路径: {DEFAULT_CHROMA_PATH}")
    print("="*60)

    # 显示所有 collection
    collections = get_all_collections()
    print(f"\n所有 Collection ({len(collections)} 个):")
    for c in collections:
        print(f"  - {c.name}")

    # 查看各个 collection（显示全部）
    view_learning_analysis(limit=100)
    view_concepts(limit=100)

    print("\n" + "="*60)
    print("可视化完成")
    print("="*60)


if __name__ == "__main__":
    main()

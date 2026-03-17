"""
ChromaDB 上下文压缩模块 v2
支持对话历史感知的上下文压缩
"""
import chromadb
from chromadb.utils import embedding_functions
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
from datetime import datetime
import os

# ============ 配置 ============
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"
DEFAULT_CHROMA_PATH = "./chroma_db"
DEFAULT_COLLECTION = "concepts"

# 初始化
_embedding_function = None
_chroma_client = None


def get_embedding_function():
    """获取 embedding 函数（延迟加载）"""
    global _embedding_function
    if _embedding_function is None:
        os.environ.setdefault('HF_HUB_OFFLINE', '1')
        _embedding_function = embedding_functions.SentenceTransformerEmbeddingFunction(
            model_name=EMBEDDING_MODEL
        )
    return _embedding_function


def get_chroma_client(path: str = DEFAULT_CHROMA_PATH):
    """获取或创建 ChromaDB 客户端"""
    global _chroma_client
    if _chroma_client is None:
        _chroma_client = chromadb.PersistentClient(path=path)
    return _chroma_client


# ============ 数据类 ============
@dataclass
class Message:
    """对话消息"""
    role: str  # "user" 或 "assistant"
    content: str
    timestamp: datetime = field(default_factory=datetime.now)


@dataclass
class ContextResult:
    """上下文压缩结果"""
    query: str
    documents: List[str]
    scores: List[float]
    conversation_history: List[Message] = None
    reformulated_query: str = None  # 重构后的查询


# ============ 对话历史管理器 ============
class ConversationHistory:
    """
    对话历史管理器
    维护用户和助手之间的对话历史
    """

    def __init__(self, max_turns: int = 10):
        """
        初始化对话历史

        Args:
            max_turns: 最多保存的对话轮数
        """
        self.messages: List[Message] = []
        self.max_turns = max_turns

    def add_user_message(self, content: str):
        """添加用户消息"""
        self.messages.append(Message(role="user", content=content))
        self._trim()

    def add_assistant_message(self, content: str):
        """添加助手消息"""
        self.messages.append(Message(role="assistant", content=content))
        self._trim()

    def _trim(self):
        """裁剪超出的消息"""
        if len(self.messages) > self.max_turns * 2:
            self.messages = self.messages[-(self.max_turns * 2):]

    def get_history(self, last_n: int = None) -> List[Message]:
        """获取对话历史"""
        if last_n is None:
            return self.messages
        return self.messages[-last_n:]

    def get_last_query(self) -> str:
        """获取上一轮用户的问题"""
        for msg in reversed(self.messages):
            if msg.role == "user":
                return msg.content
        return ""

    def get_context_for_compression(self) -> str:
        """获取用于上下文压缩的对话历史字符串"""
        if not self.messages:
            return ""

        history_str = "对话历史:\n"
        for msg in self.messages:
            role = "用户" if msg.role == "user" else "助手"
            history_str += f"{role}: {msg.content}\n"

        return history_str

    def clear(self):
        """清空对话历史"""
        self.messages = []


# ============ 查询重构器 ============
class QueryReformulator:
    """
    查询重构器
    根据对话历史重构当前查询，使其包含上下文信息
    """

    def __init__(self, llm_client=None):
        self.llm_client = llm_client

    def reformulate(self, query: str, history: List[Message]) -> str:
        """
        重构查询

        Args:
            query: 当前查询
            history: 对话历史

        Returns:
            重构后的查询
        """
        if not history:
            return query

        # 构建上下文
        context = self._build_context(query, history)

        # 如果有 LLM，使用 LLM 重构
        if self.llm_client:
            return self._llm_reformulate(query, context)

        # 否则使用简单规则重构
        return self._rule_based_reformulate(query, history)

    def _build_context(self, query: str, history: List[Message]) -> str:
        """构建上下文"""
        recent = history[-6:]  # 最近3轮对话
        context = "最近对话:\n"
        for msg in recent:
            role = "用户" if msg.role == "user" else "助手"
            context += f"{role}: {msg.content}\n"
        context += f"\n当前问题: {query}"
        return context

    def _rule_based_reformulate(self, query: str, history: List) -> str:
        """基于规则的查询重构"""
        # 检测代词指代
        pronouns = ["它", "这个", "那个", "刚才", "之前", "上述"]
        has_pronoun = any(p in query for p in pronouns)

        if has_pronoun and history:
            # 获取上一轮用户问题
            last_query = ""
            for msg in reversed(history):
                if msg.role == "user" and msg.content != query:
                    last_query = msg.content
                    break

            if last_query:
                # 简单拼接上下文
                return f"{last_query} {query}"

        return query

    def _llm_reformulate(self, query: str, context: str) -> str:
        """使用 LLM 重构查询"""
        prompt = f"""请根据对话历史，重构当前问题，使其包含必要的上下文信息。

{context}

请直接给出重构后的问题，不要添加解释。"""

        try:
            response = self.llm_client.chat.completions.create(
                model="qwen-plus",
                messages=[{"role": "user", "content": prompt}],
                max_tokens=200
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            print(f"LLM 重构失败: {e}")
            return query


# ============ 上下文压缩检索器 ============
class ContextualCompressionSearch:
    """
    带上下文压缩的检索器
    支持对话历史感知的检索
    """

    def __init__(
        self,
        collection_name: str = DEFAULT_COLLECTION,
        chroma_path: str = DEFAULT_CHROMA_PATH,
        max_history_turns: int = 10
    ):
        self.client = get_chroma_client(chroma_path)
        self.collection_name = collection_name

        # 获取或创建集合
        self.collection = self.client.get_or_create_collection(
            name=collection_name,
            embedding_function=get_embedding_function()
        )

        # 对话历史
        self.conversation_history = ConversationHistory(max_turns=max_history_turns)

        # 查询重构器
        self.query_reformulator = QueryReformulator()

        # LLM 客户端
        self.llm_client = None

    def set_llm_client(self, client):
        """设置 LLM 客户端"""
        self.llm_client = client
        self.query_reformulator.llm_client = client

    # ============ 文档操作 ============
    def add_documents(
        self,
        documents: List[str],
        metadatas: Optional[List[Dict]] = None,
        ids: Optional[List[str]] = None
    ) -> List[str]:
        """添加文档"""
        if ids is None:
            ids = [f"doc_{i}" for i in range(len(documents))]

        # 如果没有提供元数据，不传递该参数
        if metadatas is None:
            self.collection.add(
                documents=documents,
                ids=ids
            )
        else:
            self.collection.add(
                documents=documents,
                metadatas=metadatas,
                ids=ids
            )
        return ids

    def add_pdf_content(
        self,
        sentences: List[str],
        source: str = "pdf"
    ) -> List[str]:
        """添加 PDF 内容"""
        metadatas = [{"source": source, "index": i} for i in range(len(sentences))]
        return self.add_documents(sentences, metadatas)

    def delete_documents(self, ids: List[str]):
        """删除文档"""
        self.collection.delete(ids=ids)

    def clear_collection(self):
        """清空集合"""
        self.client.delete_collection(name=self.collection_name)
        self.collection = self.client.get_or_create_collection(
            name=self.collection_name,
            embedding_function=get_embedding_function()
        )

    def get_document_count(self) -> int:
        """获取文档数量"""
        return self.collection.count()

    # ============ 搜索功能 ============
    def search(
        self,
        query: str,
        n_results: int = 5,
        include_history: bool = False
    ) -> Dict[str, Any]:
        """
        基础向量搜索

        Args:
            query: 查询文本
            n_results: 返回结果数量
            include_history: 是否包含对话历史

        Returns:
            搜索结果字典
        """
        results = self.collection.query(
            query_texts=[query],
            n_results=n_results
        )

        return {
            "query": query,
            "documents": results.get("documents", [[]])[0],
            "distances": results.get("distances", [[]])[0],
            "metadatas": results.get("metadatas", [[]])[0],
            "ids": results.get("ids", [[]])[0]
        }

    def search_with_context(
        self,
        query: str,
        n_results: int = 5,
        n_after_compression: int = 3,
        use_history: bool = True,
        use_compression: bool = True
    ) -> ContextResult:
        """
        带上下文压缩的搜索（考虑对话历史）

        Args:
            query: 当前查询
            n_results: 初始检索数量
            n_after_compression: 压缩后保留数量
            use_history: 是否使用对话历史
            use_compression: 是否使用上下文压缩

        Returns:
            ContextResult: 包含重构查询和压缩结果
        """
        # 1. 记录用户问题
        if use_history:
            self.conversation_history.add_user_message(query)

        # 2. 重构查询（融入历史上下文）
        reformulated_query = query
        if use_history and self.conversation_history.messages:
            reformulated_query = self.query_reformulator.reformulate(
                query,
                self.conversation_history.messages
            )

        # 3. 执行检索
        results = self.collection.query(
            query_texts=[reformulated_query],
            n_results=n_results
        )

        documents = results.get("documents", [[]])[0]
        distances = results.get("distances", [[]])[0]

        if not documents:
            return ContextResult(
                query=query,
                reformulated_query=reformulated_query,
                documents=[],
                scores=[],
                conversation_history=self.conversation_history.get_history()
            )

        # 4. 上下文压缩
        if use_compression:
            documents = self._compress_documents(
                reformulated_query,
                documents,
                n_after_compression
            )
            scores = [1.0] * len(documents)
        else:
            scores = [1 - d for d in distances[:n_after_compression]]
            documents = documents[:n_after_compression]

        return ContextResult(
            query=query,
            reformulated_query=reformulated_query,
            documents=documents,
            scores=scores,
            conversation_history=self.conversation_history.get_history()
        )

    def _compress_documents(
        self,
        query: str,
        documents: List[str],
        top_k: int = 3
    ) -> List[str]:
        """关键词压缩"""
        import re

        # 提取关键词
        keywords = re.findall(r'\w+', query.lower())
        if not keywords:
            return documents[:top_k]

        compressed = []
        for doc in documents:
            sentences = re.split(r'[。！？\n]', doc)
            sentences = [s.strip() for s in sentences if s.strip()]

            if not sentences:
                compressed.append(doc)
                continue

            # 计算关键词得分
            scored = []
            for sentence in sentences:
                score = sum(1 for kw in keywords if kw in sentence)
                scored.append((sentence, score))

            scored.sort(key=lambda x: x[1], reverse=True)
            top_sentences = [s[0] for s in scored[:top_k]]

            if top_sentences:
                compressed.append('。'.join(top_sentences) + '。')
            else:
                compressed.append(doc)

        return compressed[:top_k]

    # ============ 对话管理 ============
    def answer_with_context(
        self,
        query: str,
        n_results: int = 5,
        include_answer: bool = True
    ) -> Dict[str, Any]:
        """
        带上下文的问答

        Args:
            query: 用户问题
            n_results: 检索数量
            include_answer: 是否将回答加入历史

        Returns:
            包含检索结果和对话历史的字典
        """
        # 执行搜索
        result = self.search_with_context(
            query=query,
            n_results=n_results,
            use_history=True,
            use_compression=True
        )

        if include_answer:
            # 这里可以调用 LLM 生成答案
            # 暂时只返回检索结果
            pass

        return {
            "query": query,
            "reformulated_query": result.reformulated_query,
            "documents": result.documents,
            "scores": result.scores,
            "history": [
                {"role": m.role, "content": m.content}
                for m in result.conversation_history
            ]
        }

    def clear_history(self):
        """清空对话历史"""
        self.conversation_history.clear()

    def get_history(self) -> List[Dict]:
        """获取对话历史"""
        return [
            {"role": m.role, "content": m.content, "time": m.timestamp.isoformat()}
            for m in self.conversation_history.messages
        ]


# ============ 便捷函数 ============
def create_search_instance(
    collection_name: str = DEFAULT_COLLECTION,
    chroma_path: str = DEFAULT_CHROMA_PATH
) -> ContextualCompressionSearch:
    """创建搜索实例"""
    return ContextualCompressionSearch(collection_name, chroma_path)


# ============ API 兼容函数 ============
def add_to_chroma(
    pdf_sentences: list,
    metadatas: Optional[List[Dict]] = None,
    collection_name: str = DEFAULT_COLLECTION
):
    """添加内容到向量库"""
    search = create_search_instance(collection_name)
    return search.add_documents(pdf_sentences, metadatas)


def search_chroma(
    question: str,
    n_results: int = 3,
    collection_name: str = DEFAULT_COLLECTION
):
    """搜索向量库"""
    search = create_search_instance(collection_name)
    result = search.search(question, n_results)
    return {
        "documents": [result["documents"]],
        "distances": [result["distances"]],
        "metadatas": [result["metadatas"]],
        "ids": [result["ids"]]
    }


if __name__ == "__main__":
    print("=== ChromaDB 对话上下文压缩测试 ===\n")

    # 创建搜索实例
    search = create_search_instance("test_context")

    # 添加测试文档
    test_docs = [
        "互信息 I(x;y) 表示两个随机变量之间的关联程度，定义为 I(x;y) = H(X) - H(X|Y)。",
        "条件互信息 I(x;y|z) 表示在已知 z 的条件下，x 和 y 之间的互信息。",
        "平均互信息 I(X;Y) 是互信息的数学期望，表示平均每个符号携带的互信息。",
        "平均条件互信息 I(X;Y|Z) 表示在已知 Z 的条件下，X 和 Y 之间的平均互信息。",
        "熵 H(X) 表示随机变量 X 的不确定性，H(X) = -∑P(x)logP(x)。",
    ]

    search.add_documents(test_docs)
    print(f"已添加 {len(test_docs)} 条文档\n")

    # 第一轮对话
    print("=" * 50)
    print("第一轮: 用户问 '什么是互信息'")
    print("=" * 50)
    result1 = search.answer_with_context("什么是互信息", n_results=3)
    print(f"原始查询: {result1['query']}")
    print(f"重构查询: {result1['reformulated_query']}")
    print("\n检索结果:")
    for i, doc in enumerate(result1['documents']):
        print(f"  {i+1}. {doc}")

    # 手动添加回答到历史（模拟）
    search.conversation_history.add_assistant_message(
        "互信息是信息论中的重要概念，表示两个随机变量之间的关联程度。"
    )

    # 第二轮对话（使用代词指代）
    print("\n" + "=" * 50)
    print("第二轮: 用户问 '它的公式是什么'")
    print("=" * 50)
    result2 = search.answer_with_context("它的公式是什么", n_results=3)
    print(f"原始查询: {result2['query']}")
    print(f"重构查询: {result2['reformulated_query']}")
    print("\n检索结果:")
    for i, doc in enumerate(result2['documents']):
        print(f"  {i+1}. {doc}")

    # 显示对话历史
    print("\n" + "=" * 50)
    print("对话历史")
    print("=" * 50)
    for h in search.get_history():
        print(f"{h['role']}: {h['content']}")

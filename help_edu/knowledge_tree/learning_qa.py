"""
学习问答模块
输入：用户提问 + ChromaDB 中的历史学习分析
输出：回答 + 学习分析（存入 ChromaDB）
"""
from typing import Dict, Any, Optional, List
import os
import json
from datetime import datetime

# 导入 ChromaDB 上下文压缩模块
from chorma_search import (
    ContextualCompressionSearch,
    ConversationHistory,
    get_chroma_client,
    get_embedding_function,
    DEFAULT_CHROMA_PATH
)

# 导入 API
from api_server import get_qwen_client

DEFAULT_COLLECTION = "concepts"


# ============ 配置 ============
# 学习分析 collection 名称
LEARNING_ANALYSIS_COLLECTION = "learning_analysis"


class LearningQASystem:
    """学习问答系统"""

    def __init__(
        self,
        chroma_path: str = DEFAULT_CHROMA_PATH,
        learning_collection: str = LEARNING_ANALYSIS_COLLECTION,
        qa_collection: str = DEFAULT_COLLECTION,
        max_history_turns: int = 10
    ):
        """
        初始化学习问答系统

        Args:
            chroma_path: ChromaDB 路径
            learning_collection: 学习分析 collection 名称
            qa_collection: 问答知识 collection 名称
            max_history_turns: 对话历史最大轮数
        """
        self.chroma_path = chroma_path

        # 学习分析检索器
        self.learning_search = ContextualCompressionSearch(
            collection_name=learning_collection,
            chroma_path=chroma_path,
            max_history_turns=max_history_turns
        )

        # 问答知识检索器
        self.qa_search = ContextualCompressionSearch(
            collection_name=qa_collection,
            chroma_path=chroma_path,
            max_history_turns=max_history_turns
        )

        # 对话历史
        self.conversation_history = ConversationHistory(max_turns=max_history_turns)

        # LLM 客户端
        self.llm_client = None
        self._init_llm_client()

    def _init_llm_client(self):
        """初始化 LLM 客户端"""
        self.llm_client = get_qwen_client()
        if self.llm_client:
            self.learning_search.set_llm_client(self.llm_client)
            self.qa_search.set_llm_client(self.llm_client)
            print("✅ LLM 客户端初始化成功")
        else:
            print("⚠️ LLM 客户端初始化失败，请检查 DASHSCOPE_API_KEY")

    def retrieve_learning_context(self, query: str, n_results: int = 5) -> Dict[str, Any]:
        """
        检索历史学习分析

        Args:
            query: 用户问题
            n_results: 返回结果数量

        Returns:
            检索结果字典
        """
        result = self.learning_search.search_with_context(
            query=query,
            n_results=n_results,
            use_history=True,
            use_compression=True
        )
        return {
            "query": result.query,
            "documents": result.documents,
            "scores": result.scores,
            "reformulated_query": result.reformulated_query
        }

    def retrieve_qa_context(self, query: str, n_results: int = 5) -> Dict[str, Any]:
        """
        检索问答知识

        Args:
            query: 用户问题
            n_results: 返回结果数量

        Returns:
            检索结果字典
        """
        result = self.qa_search.search_with_context(
            query=query,
            n_results=n_results,
            use_history=True,
            use_compression=True
        )
        return {
            "query": result.query,
            "documents": result.documents,
            "scores": result.scores,
            "reformulated_query": result.reformulated_query
        }

    def generate_answer(
        self,
        query: str,
        learning_context: List[str],
        qa_context: List[str]
    ) -> str:
        """
        生成回答

        Args:
            query: 用户问题
            learning_context: 学习分析上下文
            qa_context: 问答知识上下文

        Returns:
            生成的回答
        """
        if not self.llm_client:
            return "LLM 客户端未初始化"

        # 构建上下文
        context_parts = []

        if learning_context:
            context_parts.append("【学习过程分析】\n" + "\n".join(learning_context))

        if qa_context:
            context_parts.append("【相关知识】\n" + "\n".join(qa_context))

        context_str = "\n\n".join(context_parts) if context_parts else "无相关上下文"

        prompt = f"""你是一位教育助手，请根据以下上下文信息回答用户的问题。

{context_str}

用户问题：{query}

要求：
1. 根据上下文给出准确、详细的回答
2. 如果上下文不足以回答，请基于你的知识给出合理回答
3. 语言要通俗易懂，适合大学生阅读
4. 直接给出回答，不需要额外解释

回答："""

        try:
            response = self.llm_client.chat.completions.create(
                model="qwen-plus",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.7,
                max_tokens=1000
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            return f"生成回答失败: {str(e)}"

    def generate_learning_analysis(
        self,
        query: str,
        answer: str,
        learning_context: List[str],
        qa_context: List[str]
    ) -> str:
        """
        生成学习过程分析

        Args:
            query: 用户问题
            answer: 生成的回答
            learning_context: 学习分析上下文
            qa_context: 问答知识上下文

        Returns:
            学习分析内容
        """
        if not self.llm_client:
            return "LLM 客户端未初始化"

        context_parts = []
        if learning_context:
            context_parts.append("【之前的学习分析】\n" + "\n".join(learning_context))
        if qa_context:
            context_parts.append("【相关知识】\n" + "\n".join(qa_context))

        context_str = "\n\n".join(context_parts) if context_parts else "无"

        prompt = f"""你是一位学习分析师，请分析用户的学习过程。

【当前对话】
用户问题：{query}
回答：{answer}

【历史上下文】
{context_str}

请分析以下内容：
1. 用户当前问题的知识领域和难度 level
2. 用户可能存在的困惑点或误解
3. 用户的提问模式（是深入追问、跨领域联想、还是基础概念询问）
4. 建议的后续学习方向

要求：
1. 分析要简洁、准确，50-100字
2. 使用中文
3. 直接输出分析内容，不要添加标题或格式

分析："""

        try:
            response = self.llm_client.chat.completions.create(
                model="qwen-plus",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.7,
                max_tokens=300
            )
            return response.choices[0].message.content.strip()
        except Exception as e:
            return f"生成学习分析失败: {str(e)}"

    def save_learning_analysis(
        self,
        query: str,
        learning_analysis: str,
        metadata: Optional[Dict] = None
    ) -> str:
        """
        保存学习分析到 ChromaDB

        Args:
            query: 用户问题
            learning_analysis: 学习分析内容
            metadata: 额外的元数据

        Returns:
            文档 ID
        """
        import uuid

        # 只保存学习分析内容
        doc_content = learning_analysis

        doc_metadata = metadata or {}
        doc_metadata.update({
            "query": query,
            "timestamp": datetime.now().isoformat(),
            "type": "learning_analysis"
        })

        doc_id = str(uuid.uuid4())
        self.learning_search.add_documents(
            documents=[doc_content],
            metadatas=[doc_metadata],
            ids=[doc_id]
        )

        return doc_id

    def answer(
        self,
        query: str,
        n_learning_results: int = 3,
        n_qa_results: int = 5,
        save_analysis: bool = True
    ) -> Dict[str, Any]:
        """
        回答用户问题（主入口）

        Args:
            query: 用户问题
            n_learning_results: 学习分析检索数量
            n_qa_results: 问答知识检索数量
            save_analysis: 是否保存学习分析

        Returns:
            包含回答和学习分析的字典
        """
        # 1. 记录用户问题到对话历史
        self.conversation_history.add_user_message(query)

        # 2. 检索相关上下文
        learning_result = self.retrieve_learning_context(query, n_learning_results)
        qa_result = self.retrieve_qa_context(query, n_qa_results)

        # 3. 生成回答
        answer = self.generate_answer(
            query=query,
            learning_context=learning_result["documents"],
            qa_context=qa_result["documents"]
        )

        # 4. 生成学习分析
        learning_analysis = self.generate_learning_analysis(
            query=query,
            answer=answer,
            learning_context=learning_result["documents"],
            qa_context=qa_result["documents"]
        )

        # 5. 保存学习分析（可选）
        analysis_id = None
        if save_analysis:
            analysis_id = self.save_learning_analysis(
                query=query,
                learning_analysis=learning_analysis
            )

        # 6. 记录回答到对话历史
        self.conversation_history.add_assistant_message(answer)

        return {
            "query": query,
            "answer": answer,
            "learning_analysis": learning_analysis,
            "analysis_id": analysis_id,
            "learning_context": learning_result["documents"],
            "qa_context": qa_result["documents"]
        }

    def get_conversation_history(self) -> List[Dict]:
        """获取对话历史"""
        return [
            {"role": m.role, "content": m.content, "time": m.timestamp.isoformat()}
            for m in self.conversation_history.messages
        ]

    def clear_history(self):
        """清空对话历史"""
        self.conversation_history.clear()


# ============ 便捷函数 ============
def create_qa_system(
    chroma_path: str = DEFAULT_CHROMA_PATH,
    learning_collection: str = LEARNING_ANALYSIS_COLLECTION,
    qa_collection: str = DEFAULT_COLLECTION
) -> LearningQASystem:
    """创建学习问答系统实例"""
    return LearningQASystem(
        chroma_path=chroma_path,
        learning_collection=learning_collection,
        qa_collection=qa_collection
    )


def ask_with_learning(
    query: str,
    chroma_path: str = DEFAULT_CHROMA_PATH,
    learning_collection: str = LEARNING_ANALYSIS_COLLECTION,
    qa_collection: str = DEFAULT_COLLECTION,
    save_analysis: bool = True
) -> Dict[str, Any]:
    """
    便捷函数：直接提问并获取回答和学习分析

    Args:
        query: 用户问题
        chroma_path: ChromaDB 路径
        learning_collection: 学习分析 collection
        qa_collection: 问答知识 collection
        save_analysis: 是否保存学习分析

    Returns:
        包含回答和学习分析的字典
    """
    qa_system = create_qa_system(
        chroma_path=chroma_path,
        learning_collection=learning_collection,
        qa_collection=qa_collection
    )
    return qa_system.answer(query, save_analysis=save_analysis)


# ============ 交互模式 ============
if __name__ == "__main__":
    print("=== 学习问答系统 (输入 q 或 quit 退出) ===\n")

    # 创建问答系统
    qa = create_qa_system()

    while True:
        try:
            query = input("\n❓ 请输入问题: ").strip()

            if query.lower() in ['q', 'quit', 'exit', '退出']:
                print("再见!")
                break

            if not query:
                continue

            result = qa.answer(query, save_analysis=True)

            print("\n" + "=" * 50)
            print("📝 回答:")
            print(result["answer"])
            print("\n" + "=" * 50)
            print("📊 学习分析:")
            print(result["learning_analysis"])
            print("\n" + "=" * 50)
            print(f"💾 分析ID: {result['analysis_id']}")

        except KeyboardInterrupt:
            print("\n\n再见!")
            break
        except Exception as e:
            print(f"❌ 错误: {e}")

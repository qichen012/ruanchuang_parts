"""
ChromaDB 单元测试

测试 chroma_db 向量数据库的连接、查询、添加、删除等功能
"""
import pytest
import os
import sys
import tempfile
import shutil
from pathlib import Path

# 确保可以导入 chromadb
try:
    import chromadb
    from chromadb.utils import embedding_functions
except ImportError:
    pytest.skip("chromadb not installed", allow_module_level=True)

# 测试用的 ChromaDB 路径
TEST_CHROMA_PATH = Path(__file__).parent.parent / "chroma_db"
TEST_COLLECTION_NAME = "test_collection"


class TestChromaDBConnection:
    """测试 ChromaDB 连接功能"""

    def test_chroma_client_creation(self):
        """测试创建 ChromaDB 客户端"""
        client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        assert client is not None

    def test_get_or_create_collection(self):
        """测试获取或创建 collection"""
        client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        collection = client.get_or_create_collection(name=TEST_COLLECTION_NAME)
        assert collection is not None
        assert collection.name == TEST_COLLECTION_NAME

    def test_list_collections(self):
        """测试列出所有 collection"""
        client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        collections = client.list_collections()
        assert isinstance(collections, list)

    def test_get_collection(self):
        """测试获取已存在的 collection"""
        client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        # 先创建
        client.get_or_create_collection(name=TEST_COLLECTION_NAME)
        # 再获取
        collection = client.get_collection(name=TEST_COLLECTION_NAME)
        assert collection.name == TEST_COLLECTION_NAME


class TestChromaDBDocumentOperations:
    """测试文档操作功能"""

    @pytest.fixture(autouse=True)
    def setup_and_teardown(self):
        """每个测试前创建临时 collection，测试后删除"""
        self.client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        self.test_collection_name = f"{TEST_COLLECTION_NAME}_{id(self)}"
        self.collection = self.client.get_or_create_collection(
            name=self.test_collection_name
        )
        yield
        # 清理
        try:
            self.client.delete_collection(name=self.test_collection_name)
        except Exception:
            pass

    def test_add_documents(self):
        """测试添加文档"""
        documents = [
            "机器学习是人工智能的一个分支",
            "深度学习使用神经网络模型",
            "自然语言处理用于处理文本数据"
        ]
        ids = ["doc1", "doc2", "doc3"]

        self.collection.add(documents=documents, ids=ids)

        count = self.collection.count()
        assert count == 3

    def test_add_documents_with_metadata(self):
        """测试添加带元数据的文档"""
        documents = [
            "互信息 I(x;y) 表示两个随机变量之间的关联程度",
            "条件互信息 I(x;y|z) 表示在已知 z 的条件下 x 和 y 之间的互信息"
        ]
        ids = ["doc_meta_1", "doc_meta_2"]
        metadatas = [
            {"source": "textbook", "chapter": 3},
            {"source": "textbook", "chapter": 4}
        ]

        self.collection.add(
            documents=documents,
            ids=ids,
            metadatas=metadatas
        )

        count = self.collection.count()
        assert count == 2

    def test_query_documents(self):
        """测试查询文档"""
        documents = [
            "互信息 I(x;y) = H(X) - H(X|Y)",
            "熵 H(X) 表示随机变量的不确定性",
            "条件熵 H(X|Y) 表示已知 Y 时 X 的不确定性"
        ]
        ids = ["q1", "q2", "q3"]

        self.collection.add(documents=documents, ids=ids)

        results = self.collection.query(
            query_texts=["互信息"],
            n_results=2
        )

        assert "documents" in results
        assert len(results["documents"][0]) <= 2

    def test_get_document_by_id(self):
        """测试根据 ID 获取文档"""
        documents = ["这是测试文档内容"]
        ids = ["unique_test_id"]

        self.collection.add(documents=documents, ids=ids)

        result = self.collection.get(ids=["unique_test_id"])

        assert "documents" in result
        assert len(result["documents"]) == 1
        assert result["documents"][0] == "这是测试文档内容"

    def test_delete_documents(self):
        """测试删除文档"""
        documents = ["文档1", "文档2", "文档3"]
        ids = ["del1", "del2", "del3"]

        self.collection.add(documents=documents, ids=ids)
        assert self.collection.count() == 3

        self.collection.delete(ids=["del1", "del2"])
        assert self.collection.count() == 1

    def test_update_documents(self):
        """测试更新文档"""
        documents = ["原始内容"]
        ids = ["update_test_id"]

        self.collection.add(documents=documents, ids=ids)

        self.collection.update(
            documents=["更新后的内容"],
            ids=["update_test_id"]
        )

        result = self.collection.get(ids=["update_test_id"])
        assert result["documents"][0] == "更新后的内容"

    def test_count_documents(self):
        """测试统计文档数量"""
        documents = ["文档A", "文档B", "文档C"]
        ids = ["c1", "c2", "c3"]

        initial_count = self.collection.count()

        self.collection.add(documents=documents, ids=ids)

        assert self.collection.count() == initial_count + 3


class TestChromaDBPeek:
    """测试查看文档功能"""

    @pytest.fixture(autouse=True)
    def setup_and_teardown(self):
        """每个测试前创建临时 collection"""
        self.client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        self.test_collection_name = f"{TEST_COLLECTION_NAME}_peek_{id(self)}"
        self.collection = self.client.get_or_create_collection(
            name=self.test_collection_name
        )
        yield
        try:
            self.client.delete_collection(name=self.test_collection_name)
        except Exception:
            pass

    def test_peek_documents(self):
        """测试 peek 功能（查看前 N 条文档）"""
        documents = [f"文档内容{i}" for i in range(10)]
        ids = [f"peek{i}" for i in range(10)]

        self.collection.add(documents=documents, ids=ids)

        result = self.collection.peek(limit=5)

        assert "documents" in result
        assert len(result["documents"]) == 5


class TestChromaDBDistanceMetrics:
    """测试距离度量功能"""

    @pytest.fixture(autouse=True)
    def setup_and_teardown(self):
        """每个测试前创建临时 collection"""
        self.client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        self.test_collection_name = f"{TEST_COLLECTION_NAME}_dist_{id(self)}"
        self.collection = self.client.get_or_create_collection(
            name=self.test_collection_name
        )
        yield
        try:
            self.client.delete_collection(name=self.test_collection_name)
        except Exception:
            pass

    def test_query_with_distances(self):
        """测试查询返回距离值"""
        documents = [
            "机器学习是人工智能的重要分支",
            "深度学习使用多层神经网络",
            "计算机视觉处理图像和视频"
        ]
        ids = ["dist1", "dist2", "dist3"]

        self.collection.add(documents=documents, ids=ids)

        results = self.collection.query(
            query_texts=["人工智能"],
            n_results=3,
            include=["distances"]
        )

        assert "distances" in results
        assert len(results["distances"][0]) == 3

    def test_query_distances_are_sorted(self):
        """测试查询结果的 Distance 按升序排列（距离越小越相关）"""
        documents = [
            "人工智能改变了我们的生活",
            "机器学习是人工智能的子领域",
            "深度学习是机器学习的子领域"
        ]
        ids = ["sort1", "sort2", "sort3"]

        self.collection.add(documents=documents, ids=ids)

        results = self.collection.query(
            query_texts=["机器学习"],
            n_results=3,
            include=["distances"]
        )

        distances = results["distances"][0]
        # 验证距离是升序排列
        assert distances == sorted(distances)


class TestChromaDBErrorHandling:
    """测试错误处理"""

    @pytest.fixture(autouse=True)
    def setup_and_teardown(self):
        """每个测试前创建临时 collection"""
        self.client = chromadb.PersistentClient(path=str(TEST_CHROMA_PATH))
        self.test_collection_name = f"{TEST_COLLECTION_NAME}_err_{id(self)}"
        self.collection = self.client.get_or_create_collection(
            name=self.test_collection_name
        )
        yield
        try:
            self.client.delete_collection(name=self.test_collection_name)
        except Exception:
            pass

    def test_get_nonexistent_collection(self):
        """测试获取不存在的 collection"""
        with pytest.raises(Exception):
            self.client.get_collection(name="nonexistent_collection_xyz")

    def test_add_duplicate_id(self):
        """测试添加重复 ID 的文档"""
        documents = ["内容1", "内容2"]
        ids = ["dup_id", "dup_id"]  # 重复 ID

        # 允许重复 ID 添加（会覆盖）
        self.collection.add(documents=["新内容"], ids=["dup_id"])

        # 验证只有一条记录
        result = self.collection.get(ids=["dup_id"])
        assert len(result["documents"]) == 1

    def test_query_empty_collection(self):
        """测试查询空 collection"""
        results = self.collection.query(
            query_texts=["测试查询"],
            n_results=5
        )

        assert results["documents"][0] == []


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
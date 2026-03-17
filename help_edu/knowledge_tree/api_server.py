"""
ChromaDB 上下文压缩 API 服务
提供 HTTP API 供前端和服务器调用
"""
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any
import uvicorn
import os

# 导入 ChromaDB 搜索模块
from chroma_search import (
    ChromaContextualSearch,
    create_search_instance,
    add_to_chroma,
    search_chroma,
    search_with_compression,
    DEFAULT_COLLECTION,
    DEFAULT_CHROMA_PATH
)

# 尝试导入 LLM 客户端
try:
    from openai import OpenAI

    def get_qwen_client():
        """获取 Qwen LLM 客户端"""
        api_key = os.getenv("DASHSCOPE_API_KEY")
        if not api_key:
            env_path = os.path.join(os.path.dirname(__file__), ".env")
            if os.path.exists(env_path):
                with open(env_path, "r") as f:
                    for line in f:
                        if line.strip().startswith("DASHSCOPE_API_KEY"):
                            api_key = line.split("=")[1].strip()
                            break
        if not api_key:
            return None

        ase_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"
        return OpenAI(api_key=api_key, base_url=ase_url)

    llm_client = get_qwen_client()
except ImportError:
    llm_client = None

# ============ FastAPI 应用 ============
app = FastAPI(
    title="ChromaDB 上下文压缩 API",
    description="提供向量检索和上下文压缩功能",
    version="1.0.0"
)

# 存储搜索实例
_search_instances: Dict[str, ChromaContextualSearch] = {}


def get_search_instance(collection: str = DEFAULT_COLLECTION) -> ChromaContextualSearch:
    """获取或创建搜索实例"""
    if collection not in _search_instances:
        _search_instances[collection] = create_search_instance(collection)

    search = _search_instances[collection]

    # 如果有 LLM 客户端，设置它
    if llm_client:
        search.set_llm_client(llm_client)

    return search


# ============ 请求/响应模型 ============
class AddDocumentsRequest(BaseModel):
    """添加文档请求"""
    documents: List[str] = Field(..., description="文档内容列表")
    metadatas: Optional[List[Dict]] = Field(default=None, description="元数据列表")
    ids: Optional[List[str]] = Field(default=None, description="ID 列表")
    collection: str = Field(default=DEFAULT_COLLECTION, description="集合名称")


class SearchRequest(BaseModel):
    """搜索请求"""
    query: str = Field(..., description="查询文本")
    n_results: int = Field(default=5, description="返回结果数量")
    collection: str = Field(default=DEFAULT_COLLECTION, description="集合名称")
    where: Optional[Dict] = Field(default=None, description="元数据过滤条件")


class CompressionSearchRequest(BaseModel):
    """压缩搜索请求"""
    query: str = Field(..., description="查询文本")
    n_results: int = Field(default=10, description="初始检索数量")
    n_after_compression: int = Field(default=5, description="压缩后保留数量")
    compression_strategy: str = Field(
        default="keyword",
        description="压缩策略: keyword, extractor, filter"
    )
    collection: str = Field(default=DEFAULT_COLLECTION, description="集合名称")


class DeleteDocumentsRequest(BaseModel):
    """删除文档请求"""
    ids: List[str] = Field(..., description="要删除的文档 IDs")
    collection: str = Field(default=DEFAULT_COLLECTION, description="集合名称")


class DocumentResponse(BaseModel):
    """文档操作响应"""
    success: bool
    message: str
    ids: Optional[List[str]] = None
    count: Optional[int] = None


class SearchResponse(BaseModel):
    """搜索响应"""
    query: str
    documents: List[str]
    distances: List[float]
    metadatas: Optional[List[Dict]] = None
    ids: Optional[List[str]] = None


class CompressionSearchResponse(BaseModel):
    """压缩搜索响应"""
    query: str
    documents: List[str]
    scores: List[float]
    original_documents: Optional[List[str]] = None


# ============ API 端点 ============

@app.get("/")
def root():
    """根路径"""
    return {
        "message": "ChromaDB 上下文压缩 API",
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/health")
def health_check():
    """健康检查"""
    return {"status": "healthy"}


# ============ 文档操作端点 ============

@app.post("/documents/add", response_model=DocumentResponse)
def add_documents(request: AddDocumentsRequest):
    """添加文档到向量库"""
    try:
        search = get_search_instance(request.collection)
        ids = search.add_documents(
            documents=request.documents,
            metadatas=request.metadatas,
            ids=request.ids
        )
        return DocumentResponse(
            success=True,
            message=f"成功添加 {len(ids)} 条文档",
            ids=ids,
            count=search.get_document_count()
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/documents/delete", response_model=DocumentResponse)
def delete_documents(request: DeleteDocumentsRequest):
    """删除文档"""
    try:
        search = get_search_instance(request.collection)
        search.delete_documents(request.ids)
        return DocumentResponse(
            success=True,
            message=f"成功删除 {len(request.ids)} 条文档",
            count=search.get_document_count()
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/documents/clear", response_model=DocumentResponse)
def clear_collection(collection: str = Query(DEFAULT_COLLECTION)):
    """清空集合"""
    try:
        search = get_search_instance(collection)
        search.clear_collection()
        return DocumentResponse(
            success=True,
            message=f"集合 {collection} 已清空",
            count=0
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/documents/count")
def get_document_count(collection: str = Query(DEFAULT_COLLECTION)):
    """获取文档数量"""
    try:
        search = get_search_instance(collection)
        return {"count": search.get_document_count()}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ 搜索端点 ============

@app.post("/search", response_model=SearchResponse)
def search(request: SearchRequest):
    """基础向量搜索"""
    try:
        search = get_search_instance(request.collection)
        result = search.search(
            query=request.query,
            n_results=request.n_results,
            where=request.where
        )
        return SearchResponse(
            query=result.query,
            documents=result.documents,
            distances=result.distances,
            metadatas=result.metadatas,
            ids=result.ids
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/search/compression", response_model=CompressionSearchResponse)
def compression_search(request: CompressionSearchRequest):
    """带上下文压缩的搜索"""
    try:
        search = get_search_instance(request.collection)
        result = search.search_with_compression(
            query=request.query,
            n_results=request.n_results,
            n_after_compression=request.n_after_compression,
            compression_strategy=request.compression_strategy
        )
        return CompressionSearchResponse(
            query=request.query,
            documents=result.documents,
            scores=result.scores,
            original_documents=result.original_documents
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ 便捷端点 ============

@app.get("/search/simple")
def simple_search(
    q: str = Query(..., description="查询文本"),
    n: int = Query(5, description="结果数量"),
    collection: str = Query(DEFAULT_COLLECTION, description="集合名称")
):
    """简单的 GET 搜索"""
    try:
        search = get_search_instance(collection)
        result = search.search(q, n_results=n)
        return result.to_dict()
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/collections")
def list_collections():
    """列出所有集合"""
    try:
        client = search.get_chroma_client()
        collections = client.list_collections()
        return {"collections": [c.name for c in collections]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ============ 启动服务 ============
if __name__ == "__main__":
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run(app, host="0.0.0.0", port=port)

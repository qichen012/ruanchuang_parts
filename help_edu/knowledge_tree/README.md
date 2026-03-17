# ChromaDB 上下文压缩模块

为 RAG 系统提供向量检索和上下文压缩功能。

## 功能特性

- **向量检索**: 基于 ChromaDB 的语义搜索
- **上下文压缩**: 支持多种压缩策略
  - `keyword`: 关键词压缩（无需 LLM）
  - `extractor`: LLM 提取相关部分
  - `filter`: LLM 过滤不相关文档
- **多集合支持**: 支持创建多个独立的向量集合
- **元数据过滤**: 支持基于元数据的精确筛选

## 安装

```bash
pip install -r requirements.txt
```

## 快速开始

### 1. 直接使用模块

```python
from chroma_search import create_search_instance, search_with_compression

# 创建搜索实例
search = create_search_instance("my_collection")

# 添加文档
documents = [
    "信息熵 H(X) 表示随机变量 X 的不确定性",
    "互信息 I(x;y) 表示两个随机变量之间的关联程度"
]
search.add_documents(documents)

# 基础搜索
result = search.search("什么是信息熵", n_results=3)

# 带压缩搜索
compressed = search.search_with_compression(
    query="什么是信息熵",
    n_results=10,
    n_after_compression=5,
    compression_strategy="keyword"
)
```

### 2. 使用 API 服务

启动 API 服务：

```bash
python api_server.py
```

API 端点：

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/documents/add` | 添加文档 |
| POST | `/documents/delete` | 删除文档 |
| POST | `/documents/clear` | 清空集合 |
| GET | `/documents/count` | 获取文档数量 |
| POST | `/search` | 基础搜索 |
| POST | `/search/compression` | 带压缩搜索 |
| GET | `/search/simple` | 简单 GET 搜索 |
| GET | `/collections` | 列出所有集合 |

### API 示例

```bash
# 添加文档
curl -X POST http://localhost:8000/documents/add \
  -H "Content-Type: application/json" \
  -d '{
    "documents": ["信息熵 H(X) 表示随机变量 X 的不确定性"],
    "collection": "concepts"
  }'

# 搜索
curl -X POST http://localhost:8000/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是信息熵",
    "n_results": 3,
    "collection": "concepts"
  }'

# 带压缩搜索
curl -X POST http://localhost:8000/search/compression \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是信息熵",
    "n_results": 10,
    "n_after_compression": 5,
    "compression_strategy": "keyword",
    "collection": "concepts"
  }'
```

## 配置

可在 `.env` 文件中配置：

```bash
DASHSCOPE_API_KEY=your_api_key  # 用于 LLM 压缩功能
PORT=8000                       # API 服务端口
CHROMA_PATH=./chroma_db        # ChromaDB 存储路径
```

## 与其他模块集成

可以与 `search_and_generate` 模块集成：

```python
from search_and_generate import api as search_api
from chroma_search import create_search_instance

# 搜索相关内容
search = create_search_instance("concepts")
result = search.search("互信息", n_results=5)

# 结合 search_and_generate 使用
main_result = search_api.main("互信息")
```

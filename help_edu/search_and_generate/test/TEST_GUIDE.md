# 单元测试文档

## 运行测试

```bash
conda activate RC
python -m pytest test/ -v
```

## search 模块

| 输入 | 期望输出 | 实际输出 |
|------|---------|---------|
| `search_news('python')` + mock网络异常 | `[]` | `✓` |
| `search_news('test')` + mock正常响应 | `list`类型 | `✓` |

## generate 模块

| 输入 | 期望输出 | 实际输出 |
|------|---------|---------|
| `generate_expansion('')` | 包含"未提供" | `✓` |
| `fetch_page_content('https://invalid.url')` + mock网络异常 | `None` | `✓` |

## zip 模块

| 输入 | 期望输出 | 实际输出 |
|------|---------|---------|
| `zip_content('')` | 包含"无内容" | `✓` |

## api 模块

| 输入 | 期望输出 | 实际输出 |
|------|---------|---------|
| 导入`api`模块 | `app`属性存在 | `✓` |
| `GitHubSearchRequest(keyword='python')` | `keyword='python'` | `✓` |

## Mock 使用示例

```python
# 模拟函数返回
mocker.patch('search.get_bocha_api_key', return_value='fake_key')

# 模拟网络异常
mock_get.side_effect = Exception('Network error')

# 模拟API响应
mock_response.json.return_value = {'results': []}
```

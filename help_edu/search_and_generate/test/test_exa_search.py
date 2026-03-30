"""
测试 Exa API 搜索功能
"""
import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from exa_search import search_news, get_exa_api_key


def test_get_exa_api_key(monkeypatch):
    """测试获取 Exa API Key"""
    monkeypatch.setenv("EXA_API_KEY", "test_key_123")
    assert get_exa_api_key() == "test_key_123"


def test_get_exa_api_key_missing(monkeypatch):
    """测试未设置 API Key 时返回 None"""
    monkeypatch.delenv("EXA_API_KEY", raising=False)
    assert get_exa_api_key() is None


def test_search_news_returns_empty_on_missing_key(monkeypatch):
    """测试 API Key 缺失时返回空列表"""
    monkeypatch.delenv("EXA_API_KEY", raising=False)

    result = search_news("光合作用", limit=2)
    assert result == []


def test_search_news_returns_list_on_api_error(mocker, monkeypatch):
    """测试 API 错误时返回空列表"""
    monkeypatch.setenv("EXA_API_KEY", "fake_key")
    mocker.patch('exa_search.requests.post')
    mocker.patch('exa_search.requests.post.side_effect', Exception('Network error'))

    result = search_news("python")
    assert result == []


def test_search_news_success(mocker, monkeypatch):
    """测试成功返回搜索结果"""
    monkeypatch.setenv("EXA_API_KEY", "fake_key")

    mock_response = mocker.MagicMock()
    mock_response.json.return_value = {
        "results": [
            {
                "title": "Test Article",
                "url": "https://example.com/article",
                "text": "This is test content about python programming."
            }
        ]
    }
    mock_response.raise_for_status = mocker.MagicMock()
    mocker.patch('exa_search.requests.post', return_value=mock_response)

    result = search_news("python", limit=5, fetch_full=False)

    assert isinstance(result, list)
    assert len(result) == 1
    assert result[0]["title"] == "Test Article"
    assert result[0]["url"] == "https://example.com/article"
    assert "python" in result[0]["content"].lower()


def test_search_news_with_fetch_full(mocker, monkeypatch):
    """测试完整内容抓取"""
    monkeypatch.setenv("EXA_API_KEY", "fake_key")

    mock_response = mocker.MagicMock()
    mock_response.json.return_value = {
        "results": [
            {
                "title": "Python Tutorial",
                "url": "https://example.com/python"
            }
        ]
    }
    mock_response.raise_for_status = mocker.MagicMock()
    mocker.patch('exa_search.requests.post', return_value=mock_response)

    # Mock fetch_page_content
    mocker.patch('exa_search.fetch_page_content', return_value="Full content from the page")

    result = search_news("python", limit=5, fetch_full=True)

    assert isinstance(result, list)
    assert result[0]["content"] == "Full content from the page"


def test_search_news_multiple_keywords(mocker, monkeypatch):
    """测试多个关键词搜索"""
    monkeypatch.setenv("EXA_API_KEY", "fake_key")

    mock_response = mocker.MagicMock()
    mock_response.json.return_value = {
        "results": [
            {
                "title": "Article 1",
                "url": "https://example.com/1",
                "text": "Content about machine learning"
            }
        ]
    }
    mock_response.raise_for_status = mocker.MagicMock()
    mocker.patch('exa_search.requests.post', return_value=mock_response)

    result = search_news(["python", "机器学习"], limit=5, fetch_full=False)

    assert isinstance(result, list)

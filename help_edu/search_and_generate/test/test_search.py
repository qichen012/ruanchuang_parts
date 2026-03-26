import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from search import search_news


def test_search_news_returns_empty_on_error(mocker):
    """测试API错误返回空列表"""
    mocker.patch('search.get_bocha_api_key', return_value='fake_key')
    mock_get = mocker.patch('search.requests.get')
    mock_get.side_effect = Exception('Network error')

    result = search_news('python')
    assert result == []


def test_search_news_returns_list(mocker):
    """测试正常返回列表"""
    mocker.patch('search.get_bocha_api_key', return_value='fake_key')
    mock_response = mocker.MagicMock()
    mock_response.json.return_value = {'results': []}
    mock_get = mocker.patch('search.requests.get')
    mock_get.return_value = mock_response

    result = search_news('test')
    assert isinstance(result, list)

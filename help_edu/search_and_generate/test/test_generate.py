import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from generate import fetch_page_content, generate_expansion


def test_generate_expansion_empty_concept():
    """测试空概念返回错误消息"""
    result = generate_expansion('')
    assert '未提供' in result
    


def test_fetch_page_content_invalid_url(mocker):
    """测试无效URL返回None"""
    mock_get = mocker.patch('generate.requests.get')
    mock_get.side_effect = Exception('Network error')

    result = fetch_page_content('https://invalid.url')
    assert result is None
    

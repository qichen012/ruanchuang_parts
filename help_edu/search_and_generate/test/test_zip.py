import pytest
import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from zip import zip_content


def test_zip_content_empty():
    """测试空文本返回错误消息"""
    result = zip_content('')
    assert '无内容' in result

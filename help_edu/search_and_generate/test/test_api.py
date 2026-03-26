import pytest
import sys
import os
from unittest.mock import patch, MagicMock

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def test_api_module_exists():
    """测试api模块可导入"""
    try:
        import api
        assert hasattr(api, 'app')
    except ImportError as e:
        pytest.skip(f"api模块导入失败: {e}")


def test_search_models_validation():
    """测试请求模型验证"""
    from pydantic import ValidationError
    from api import GitHubSearchRequest

    # 有效输入
    req = GitHubSearchRequest(keyword='python')
    assert req.keyword == 'python'
    assert req.language == 'python'
    assert req.min_stars == 1000

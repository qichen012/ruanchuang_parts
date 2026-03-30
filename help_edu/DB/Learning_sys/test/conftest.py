"""
pytest配置文件
提供数据库测试所需的fixtures和mock配置
"""
import pytest
import sys
import os
from unittest.mock import MagicMock, patch
from datetime import date, datetime

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


@pytest.fixture
def mock_db_session():
    """Mock数据库会话对象"""
    session = MagicMock()
    return session


@pytest.fixture
def sample_user_data():
    """示例用户数据"""
    return {
        "id": 1,
        "email": "test@example.com",
        "password": "hashed_password",
        "name": "Test User",
        "gender": "male",
        "age": 25
    }


@pytest.fixture
def sample_daily_brief_data():
    """示例每日简报数据"""
    return {
        "id": 1,
        "user_id": 1,
        "target_date": date(2026, 3, 25),
        "posterior_insight": "AI技术正在改变教育行业",
        "key_concepts": "机器学习,个性化学习,智能辅导",
        "created_at": datetime(2026, 3, 25, 10, 0, 0),
        "next_review_date": date(2026, 3, 26),
        "review_stage": 1,
        "user_reflect": "今天学习了AI教育应用",
        "source_handouts": [{"title": "AI教育白皮书", "path": "/docs/ai_edu.pdf"}],
        "handout_count": 1,
        "process_time": "120s"
    }


@pytest.fixture
def sample_source_document_data():
    """示例源文档数据"""
    return {
        "id": 1,
        "user_id": 1,
        "file_name": "机器学习入门.pdf",
        "file_path": "/uploads/ml_intro.pdf",
        "upload_date": date(2026, 3, 20),
        "processed_status": "Done"
    }


@pytest.fixture
def sample_elite_idea_card_data():
    """示例精英想法卡片数据"""
    return {
        "id": 1,
        "daily_bried_id": 1,
        "origin_concept": "机器学习",
        "meta_idea_name": "监督学习",
        "meta_explanation": "通过已标记数据训练模型",
        "create_at": datetime(2026, 3, 25, 14, 0, 0)
    }


@pytest.fixture
def mock_engine():
    """Mock数据库引擎"""
    with patch("database.create_engine") as mock:
        yield mock


@pytest.fixture
def mock_session_local():
    """Mock SessionLocal"""
    with patch("database.SessionLocal") as mock:
        yield mock

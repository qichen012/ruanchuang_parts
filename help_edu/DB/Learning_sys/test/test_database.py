"""
数据库模型单元测试
测试 database.py 中定义的所有SQLAlchemy模型
"""
import pytest
import sys
import os
from unittest.mock import MagicMock, patch, Mock
from datetime import date, datetime

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


class TestDatabaseModelsStructure:
    """测试数据库模型结构（通过代码分析，不实际导入）"""

    def test_all_table_classes_are_defined(self):
        """验证所有表类在database.py中定义"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            tree = ast.parse(f.read())

        # 获取所有类定义
        class_names = [node.name for node in ast.walk(tree) if isinstance(node, ast.ClassDef)]

        expected_tables = [
            "UserInformation", "SourceDocument", "DailyBrief", "ReviewLog",
            "EliteIdeaCard", "EliteIdeaCase", "ExternalResource", "UserScreenshot",
            "AssociationBrief", "ScholarNote", "KnowledgeMap", "MapInteractionLog",
            "MapCognitiveSnapshot", "AppUsageLog"
        ]

        for table in expected_tables:
            assert table in class_names, f"Table class {table} not found"

    def test_all_tables_have_tablename_attribute(self):
        """验证所有表都有__tablename__定义"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()
            tree = ast.parse(content)

        expected_tables = [
            "user_information", "source_documents", "daily_briefs", "review_logs",
            "elite_idea_cards", "elite_idea_cases", "external_resources", "user_screenshots",
            "association_briefs", "scholar_notes", "knowledge_maps", "map_interaction_logs",
            "map_cognitive_snapshots", "app_usage_logs"
        ]

        for tablename in expected_tables:
            assert f'__tablename__ = "{tablename}"' in content, f"Table {tablename} __tablename__ not found"

    def test_user_information_has_email_unique_index(self):
        """验证UserInformation的email字段有unique索引"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        # 查找UserInformation类中的email字段定义
        assert "email = Column(String(100), unique=True, index=True)" in content

    def test_daily_brief_has_required_fields(self):
        """验证DailyBrief有所有必需字段"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        required_fields = [
            "id", "user_id", "target_date", "posterior_insight", "key_concepts",
            "created_at", "next_review_date", "review_stage", "user_reflect"
        ]

        # 确认这些字段名在DailyBrief类定义中
        assert "class DailyBrief(Base):" in content
        for field in required_fields:
            assert field in content.split("class DailyBrief(Base):")[1].split("class ")[0]

    def test_all_models_inherit_from_base(self):
        """验证所有模型继承自Base"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        expected_models = [
            "UserInformation", "SourceDocument", "DailyBrief", "ReviewLog",
            "EliteIdeaCard", "EliteIdeaCase", "ExternalResource", "UserScreenshot",
            "AssociationBrief", "ScholarNote", "KnowledgeMap", "MapInteractionLog",
            "MapCognitiveSnapshot", "AppUsageLog"
        ]

        for model in expected_models:
            assert f"class {model}(Base):" in content, f"Model {model} should inherit from Base"

    def test_review_log_has_feynman_score(self):
        """验证ReviewLog有feynman_score字段"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "feynman_score = Column(Integer)" in content

    def test_knowledge_map_has_map_json_json_type(self):
        """验证KnowledgeMap有map_json字段(JSON类型)"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "map_json = Column(JSON)" in content

    def test_app_usage_log_has_duration_seconds(self):
        """验证AppUsageLog有duration_seconds字段"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "duration_seconds = Column(Integer)" in content


class TestDatabaseRelationships:
    """测试数据库关系定义"""

    def test_user_has_relationship_with_source_documents(self):
        """验证UserInformation与SourceDocument有关系"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'source_documents = relationship("SourceDocument"' in content

    def test_user_has_relationship_with_daily_briefs(self):
        """验证UserInformation与DailyBrief有关系"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'daily_briefs = relationship("DailyBrief"' in content

    def test_daily_brief_has_relationship_with_elite_idea_cards(self):
        """验证DailyBrief与EliteIdeaCard有关系"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'elite_idea_cards = relationship("EliteIdeaCard"' in content


class TestDatabaseImports:
    """测试数据库模块导入"""

    def test_database_file_exists(self):
        """验证database.py文件存在"""
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        assert os.path.exists(db_path)

    def test_sqlalchemy_imports_present(self):
        """验证必要的SQLAlchemy导入存在"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        required_imports = [
            "from sqlalchemy import",
            "create_engine", "Column", "Integer", "String", "Date", "DateTime",
            "Enum", "Text", "ForeignKey", "JSON"
        ]

        for imp in required_imports:
            assert imp in content

    def test_sessionmaker_configured(self):
        """验证SessionLocal配置正确"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)" in content

    def test_declarative_base_used(self):
        """验证使用declarative_base"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "Base = declarative_base()" in content


class TestDatabaseConfiguration:
    """测试数据库配置"""

    def test_database_url_from_env(self):
        """验证DATABASE_URL从环境变量读取"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'DATABASE_URL = os.getenv("DATABASE_URL"' in content

    def test_dotenv_load_called(self):
        """验证load_dotenv被调用"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "load_dotenv()" in content

    def test_get_db_function_exists(self):
        """验证get_db函数存在"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "def get_db():" in content

    def test_get_db_uses_session_local(self):
        """验证get_db使用SessionLocal"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        # 提取get_db函数内容
        assert "SessionLocal()" in content

    def test_get_db_uses_try_finally(self):
        """验证get_db使用try-finally确保关闭连接"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "try:" in content and "finally:" in content
        assert "db.close()" in content

    def test_get_db_is_generator(self):
        """验证get_db是生成器函数（使用yield）"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "yield db" in content or "yield" in content.split("def get_db():")[1]


class TestForeignKeyDefinitions:
    """测试外键定义"""

    def test_source_document_has_user_id_fk(self):
        """验证SourceDocument有user_id外键"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'user_id = Column(Integer, ForeignKey("user_information.id"))' in content

    def test_daily_brief_has_user_id_fk(self):
        """验证DailyBrief有user_id外键"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'user_id = Column(Integer, ForeignKey("user_information.id"))' in content

    def test_review_log_has_user_and_brief_fk(self):
        """验证ReviewLog有user_id和brief_id外键"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'user_id = Column(Integer, ForeignKey("user_information.id"))' in content
        assert 'brief_id = Column(Integer, ForeignKey("daily_briefs.id"))' in content

    def test_elite_idea_card_has_daily_brief_fk(self):
        """验证EliteIdeaCard有daily_brief外键"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert 'daily_bried_id = Column(Integer, ForeignKey("daily_briefs.id"))' in content


class TestEnumDefinitions:
    """测试枚举字段定义"""

    def test_user_gender_enum(self):
        """验证UserInformation有gender枚举"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "gender = Column(Enum('male', 'female')" in content

    def test_source_document_status_enum(self):
        """验证SourceDocument有processed_status枚举"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "processed_status = Column(Enum('Pending', 'Done', 'Failed')" in content

    def test_association_brief_type_enum(self):
        """验证AssociationBrief有type枚举"""
        import ast
        db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "database.py")
        with open(db_path, "r") as f:
            content = f.read()

        assert "type = Column(Enum('Auto', 'Manual')" in content

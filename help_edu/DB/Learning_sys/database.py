from sqlalchemy import create_engine, Column, Integer, String, Date, DateTime, Enum, Text, ForeignKey, JSON, Boolean
from sqlalchemy.dialects.mysql import TINYINT
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, relationship
import os
from dotenv import load_dotenv

load_dotenv()  # 加载 .env 文件

DATABASE_URL = os.getenv("DATABASE_URL", "mysql+pymysql://root:password@localhost:3306/Learning_DB")

engine = create_engine(DATABASE_URL)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# 用户信息表
class UserInformation(Base):
    __tablename__ = "user_information"
    
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(100), unique=True, index=True) # 新增：邮箱作为账号
    password = Column(String(255))                       # 新增：加密后的密码
    name = Column(String(45), default="", server_default="") 
    gender = Column(Enum('male', 'female'), default='male', server_default='male')
    age = Column(Integer, default=0, server_default='0')
    
    # 关系
    source_documents = relationship("SourceDocument", back_populates="user")
    daily_briefs = relationship("DailyBrief", back_populates="user")
    user_screenshots = relationship("UserScreenshot", back_populates="user")
    association_briefs = relationship("AssociationBrief", back_populates="user")
    scholar_notes = relationship("ScholarNote", back_populates="user")
    knowledge_maps = relationship("KnowledgeMap", back_populates="user")
    map_interaction_logs = relationship("MapInteractionLog", back_populates="user")
    map_cognitive_snapshots = relationship("MapCognitiveSnapshot", back_populates="user")

# 源文档表
class SourceDocument(Base):
    __tablename__ = "source_documents"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    file_name = Column(String(255))
    file_path = Column(String(500))
    upload_date = Column(Date, nullable=False)
    processed_status = Column(Enum('Pending', 'Done', 'Failed'), nullable=False)
    
    # 关系
    user = relationship("UserInformation", back_populates="source_documents")
    knowledge_maps = relationship("KnowledgeMap", back_populates="source_document")
    map_interaction_logs = relationship("MapInteractionLog", back_populates="source_document")
    map_cognitive_snapshots = relationship("MapCognitiveSnapshot", back_populates="source_document")

# 每日简报表
class DailyBrief(Base):
    __tablename__ = "daily_briefs"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    target_date = Column(Date)
    posterior_insight = Column(Text)              # 修改：放宽长度限制以存储长篇洞见
    key_concepts = Column(Text)                   # 新增：核心概念长文本
    created_at = Column(DateTime)
    next_review_date = Column(Date)               # 修改：统一命名为 next_review_date
    review_stage = Column(Integer)
    user_reflect = Column(Text)                   # 修改：改为全小写并使用 Text 存储长篇反思
    source_handouts = Column(JSON)                # 新增：存储来源材料的列表
    handout_count = Column(Integer)               # 新增：材料数量
    process_time = Column(String(45))             # 新增：处理耗时
    
    # 关系
    user = relationship("UserInformation", back_populates="daily_briefs")
    elite_idea_cards = relationship("EliteIdeaCard", back_populates="daily_brief")
    scholar_notes = relationship("ScholarNote", back_populates="daily_brief")

# 复习日志表
class ReviewLog(Base):
    __tablename__ = "review_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    brief_id = Column(Integer, ForeignKey("daily_briefs.id"))
    review_at = Column(DateTime)
    feynman_score = Column(Integer)
    
    # 关系
    user = relationship("UserInformation")
    daily_brief = relationship("DailyBrief")

# 精英想法卡片表
class EliteIdeaCard(Base):
    __tablename__ = "elite_idea_cards"
    
    id = Column(Integer, primary_key=True, index=True)
    daily_bried_id = Column(Integer, ForeignKey("daily_briefs.id"))
    origin_concept = Column(String(45))
    meta_idea_name = Column(String(45))
    meta_explanation = Column(String(45))
    create_at = Column(DateTime)
    
    # 关系
    daily_brief = relationship("DailyBrief", back_populates="elite_idea_cards")
    elite_idea_cases = relationship("EliteIdeaCase", back_populates="elite_idea_card")
    external_resources = relationship("ExternalResource", back_populates="elite_idea_card")

# 精英想法案例表
class EliteIdeaCase(Base):
    __tablename__ = "elite_idea_cases"
    
    id = Column(Integer, primary_key=True, index=True)
    meta_id = Column(Integer, ForeignKey("elite_idea_cards.id"))
    case_title = Column(String(45))
    case_content = Column(String(45))
    image_path = Column(String(45))
    query_rewrite = Column(Text(100))
    
    # 关系
    elite_idea_card = relationship("EliteIdeaCard", back_populates="elite_idea_cases")

# 外部资源表
class ExternalResource(Base):
    __tablename__ = "external_resources"
    
    id = Column(Integer, primary_key=True, index=True)
    card_id = Column(Integer, ForeignKey("elite_idea_cards.id"))
    title = Column(String(45))
    url = Column(String(45))
    LLM_context = Column(Text(100))
    source = Column(String(45))
    
    # 关系
    elite_idea_card = relationship("EliteIdeaCard", back_populates="external_resources")

# 用户截图表
class UserScreenshot(Base):
    __tablename__ = "user_screenshots"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    image__path = Column(String(45))
    vlm_analysis = Column(Text(100))
    upload_date = Column(Date)
    
    # 关系
    user = relationship("UserInformation", back_populates="user_screenshots")

# 关联简报表
class AssociationBrief(Base):
    __tablename__ = "association_briefs"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    type = Column(Enum('Auto', 'Manual'))
    content = Column(Text(100))
    notes_date = Column(Date)
    screenshot_date = Column(Date)
    created_at = Column(DateTime)
    
    # 关系
    user = relationship("UserInformation", back_populates="association_briefs")

# 学者笔记表
class ScholarNote(Base):
    __tablename__ = "scholar_notes"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    notes_content = Column(Text(100))
    target_date = Column(Date)
    daily_brief_id = Column(Integer, ForeignKey("daily_briefs.id"))
    
    # 关系
    user = relationship("UserInformation", back_populates="scholar_notes")
    daily_brief = relationship("DailyBrief", back_populates="scholar_notes")

# 知识图谱表
class KnowledgeMap(Base):
    __tablename__ = "knowledge_maps"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    source_doc_id = Column(Integer, ForeignKey("source_documents.id"))
    map_json = Column(JSON)
    created_at = Column(DateTime)
    
    # 关系
    user = relationship("UserInformation", back_populates="knowledge_maps")
    source_document = relationship("SourceDocument", back_populates="knowledge_maps")

# 图谱交互日志表
class MapInteractionLog(Base):
    __tablename__ = "map_interaction_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    source_doc_id = Column(Integer, ForeignKey("source_documents.id"))
    node_id = Column(String(45))
    user_query = Column(Text)
    ai_response = Column(Text(100))
    created_at = Column(DateTime)
    is_distilled = Column(Integer)
    
    # 关系
    user = relationship("UserInformation", back_populates="map_interaction_logs")
    source_document = relationship("SourceDocument", back_populates="map_interaction_logs")

# 图谱认知快照表
class MapCognitiveSnapshot(Base):
    __tablename__ = "map_cognitive_snapshots"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    source_doc_id = Column(Integer, ForeignKey("source_documents.id"))
    last_processed_log_id = Column(Integer)
    snapshot_content = Column(Text(100))
    path_nodes = Column(JSON)
    version = Column(Integer)
    last_log_id = Column(Integer)
    
    # 关系
    user = relationship("UserInformation", back_populates="map_cognitive_snapshots")
    source_document = relationship("SourceDocument", back_populates="map_cognitive_snapshots")

# App 使用时段日志表
class AppUsageLog(Base):
    __tablename__ = "app_usage_logs"
    
    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("user_information.id"))
    start_time = Column(DateTime)          # 本次打开 App 的时间
    end_time = Column(DateTime)            # 本次离开 App 的时间
    duration_seconds = Column(Integer)     # 使用时长（秒）
    
    # 关系
    user = relationship("UserInformation")


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
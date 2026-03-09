from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from database import (
    UserInformation, SourceDocument, DailyBrief, EliteIdeaCard,
    EliteIdeaCase, ExternalResource, UserScreenshot, AssociationBrief,
    ScholarNote, KnowledgeMap, MapInteractionLog, MapCognitiveSnapshot, ReviewLog
)
from pydantic import BaseModel, Field, EmailStr
from typing import List, Optional
from datetime import date, datetime
import utils

# 创建路由
api_router = APIRouter()

# Pydantic模型定义
# 用户相关模型
class UserBase(BaseModel):
    name: Optional[str] = Field(None, max_length=45)
    gender: Optional[str] = Field(None, pattern="^(male|female)$")
    age: Optional[int] = Field(None, ge=0, le=120)

class UserCreate(UserBase):
    id: int

class UserUpdate(UserBase):
    pass

class UserResponse(UserBase):
    id: int
    
    class Config:
        from_attributes = True

# 源文档相关模型
class SourceDocumentBase(BaseModel):
    user_id: Optional[int] = None
    file_name: Optional[str] = Field(None, max_length=255)
    file_path: Optional[str] = Field(None, max_length=500)
    upload_date: date
    processed_status: str = Field(..., pattern="^(Pending|Done|Failed)$")

class SourceDocumentCreate(SourceDocumentBase):
    pass

class SourceDocumentUpdate(BaseModel):
    user_id: Optional[int] = None
    file_name: Optional[str] = Field(None, max_length=255)
    file_path: Optional[str] = Field(None, max_length=500)
    upload_date: Optional[date] = None
    processed_status: Optional[str] = Field(None, pattern="^(Pending|Done|Failed)$")

class SourceDocumentResponse(SourceDocumentBase):
    id: int
    
    class Config:
        from_attributes = True

# 每日简报相关模型
class DailyBriefBase(BaseModel):
    user_id: Optional[int] = None
    target_date: Optional[date] = None
    posterior_insight: Optional[str] = Field(None, max_length=45)
    created_at: Optional[datetime] = None
    new_review_date: Optional[date] = None
    reciew_stage: Optional[int] = None
    User_reflect: Optional[str] = Field(None, max_length=45)

class DailyBriefCreate(DailyBriefBase):
    pass

class DailyBriefUpdate(BaseModel):
    user_id: Optional[int] = None
    target_date: Optional[date] = None
    posterior_insight: Optional[str] = Field(None, max_length=45)
    new_review_date: Optional[date] = None
    reciew_stage: Optional[int] = None
    User_reflect: Optional[str] = Field(None, max_length=45)

class DailyBriefResponse(DailyBriefBase):
    id: int
    
    class Config:
        from_attributes = True

# 复习日志相关模型
class ReviewLogBase(BaseModel):
    user_id: Optional[int] = None
    brief_id: Optional[int] = None
    review_at: Optional[datetime] = None
    feynman_score: Optional[int] = None

class ReviewLogCreate(ReviewLogBase):
    pass

class ReviewLogUpdate(BaseModel):
    user_id: Optional[int] = None
    brief_id: Optional[int] = None
    review_at: Optional[datetime] = None
    feynman_score: Optional[int] = None

class ReviewLogResponse(ReviewLogBase):
    id: int
    
    class Config:
        from_attributes = True

# 精英想法卡片相关模型
class EliteIdeaCardBase(BaseModel):
    daily_bried_id: Optional[int] = None
    origin_concept: Optional[str] = Field(None, max_length=45)
    meta_idea_name: Optional[str] = Field(None, max_length=45)
    meta_explanation: Optional[str] = Field(None, max_length=45)
    create_at: Optional[datetime] = None

class EliteIdeaCardCreate(EliteIdeaCardBase):
    pass

class EliteIdeaCardUpdate(BaseModel):
    daily_bried_id: Optional[int] = None
    origin_concept: Optional[str] = Field(None, max_length=45)
    meta_idea_name: Optional[str] = Field(None, max_length=45)
    meta_explanation: Optional[str] = Field(None, max_length=45)

class EliteIdeaCardResponse(EliteIdeaCardBase):
    id: int
    
    class Config:
        from_attributes = True

class UserAuthRequest(BaseModel):
    email: str = Field(..., description="用户账号")
    password: str = Field(..., max_length=50)

class TokenResponse(BaseModel):
    token: Optional[str] = None
    message: str
    code: int

class AppUsageCreate(BaseModel):
    user_id: int
    start_time: datetime
    end_time: datetime
    duration_seconds: int

class AppUsageResponse(AppUsageCreate):
    id: int
    class Config:
        from_attributes = True

# --- API 端点 ---
@api_router.post("/app-usage", response_model=AppUsageResponse)
def record_app_usage(usage: AppUsageCreate, db: Session = Depends(get_db)):
    db_usage = AppUsageLog(**usage.dict())
    db.add(db_usage)
    db.commit()
    db.refresh(db_usage)
    return db_usage

@api_router.post("/register", response_model=TokenResponse)
def register(user_data: UserAuthRequest, db: Session = Depends(get_db)):
    # 检查数据库中是否已经存在该邮箱
    db_user = db.query(UserInformation).filter(UserInformation.email == user_data.email).first()
    if db_user:
        return TokenResponse(token=None, message="邮箱已被注册，请直接登录", code=400)
    
    try:
        # 密码加密
        hashed_pwd = utils.get_password_hash(user_data.password)
        
        # 创建新用户 (由于 id 是自增的，不需要手动传 id)
        new_user = UserInformation(
            email=user_data.email, 
            password=hashed_pwd,
            # name, gender, age 可以留空，后续通过 PUT /users/{id} 补充
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
        
        # 签发 Token
        access_token = utils.create_access_token(data={"sub": new_user.email, "user_id": new_user.id})
        
        return TokenResponse(token=access_token, message="注册成功", code=200)
    except Exception as e:
        db.rollback()
        return TokenResponse(token=None, message=f"服务器错误: {str(e)}", code=500)

@api_router.post("/login", response_model=TokenResponse)
def login(user_data: UserAuthRequest, db: Session = Depends(get_db)):
    user = db.query(UserInformation).filter(UserInformation.email == user_data.email).first()
    
    if not user or not utils.verify_password(user_data.password, user.password):
        return TokenResponse(token=None, message="账号或密码错误", code=401)
    
    access_token = utils.create_access_token(data={"sub": user.email, "user_id": user.id})
    return TokenResponse(token=access_token, message="登录成功", code=200)

# API端点
# 用户相关API
@api_router.post("/users", response_model=UserResponse)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = UserInformation(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

@api_router.get("/users/{user_id}", response_model=UserResponse)
def get_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user

@api_router.get("/users", response_model=List[UserResponse])
def get_users(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    users = db.query(UserInformation).offset(skip).limit(limit).all()
    return users

@api_router.put("/users/{user_id}", response_model=UserResponse)
def update_user(user_id: int, user: UserUpdate, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    for key, value in user.dict(exclude_unset=True).items():
        setattr(db_user, key, value)
    
    db.commit()
    db.refresh(db_user)
    return db_user

@api_router.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    db.delete(db_user)
    db.commit()
    return {"message": "User deleted successfully"}

# 源文档相关API
@api_router.post("/source-documents", response_model=SourceDocumentResponse)
def create_source_document(doc: SourceDocumentCreate, db: Session = Depends(get_db)):
    db_doc = SourceDocument(**doc.dict())
    db.add(db_doc)
    db.commit()
    db.refresh(db_doc)
    return db_doc

@api_router.get("/source-documents/{doc_id}", response_model=SourceDocumentResponse)
def get_source_document(doc_id: int, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    return db_doc

@api_router.get("/source-documents", response_model=List[SourceDocumentResponse])
def get_source_documents(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    docs = db.query(SourceDocument).offset(skip).limit(limit).all()
    return docs

@api_router.put("/source-documents/{doc_id}", response_model=SourceDocumentResponse)
def update_source_document(doc_id: int, doc: SourceDocumentUpdate, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    
    for key, value in doc.dict(exclude_unset=True).items():
        setattr(db_doc, key, value)
    
    db.commit()
    db.refresh(db_doc)
    return db_doc

@api_router.delete("/source-documents/{doc_id}")
def delete_source_document(doc_id: int, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    
    db.delete(db_doc)
    db.commit()
    return {"message": "Source document deleted successfully"}

# 每日简报相关API
@api_router.post("/daily-briefs", response_model=DailyBriefResponse)
def create_daily_brief(brief: DailyBriefCreate, db: Session = Depends(get_db)):
    db_brief = DailyBrief(**brief.dict())
    db.add(db_brief)
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.get("/daily-briefs/{brief_id}", response_model=DailyBriefResponse)
def get_daily_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    return db_brief

@api_router.get("/daily-briefs", response_model=List[DailyBriefResponse])
def get_daily_briefs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    briefs = db.query(DailyBrief).offset(skip).limit(limit).all()
    return briefs

@api_router.put("/daily-briefs/{brief_id}", response_model=DailyBriefResponse)
def update_daily_brief(brief_id: int, brief: DailyBriefUpdate, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    
    for key, value in brief.dict(exclude_unset=True).items():
        setattr(db_brief, key, value)
    
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.delete("/daily-briefs/{brief_id}")
def delete_daily_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    
    db.delete(db_brief)
    db.commit()
    return {"message": "Daily brief deleted successfully"}

# 复习日志相关API
@api_router.post("/review-logs", response_model=ReviewLogResponse)
def create_review_log(log: ReviewLogCreate, db: Session = Depends(get_db)):
    db_log = ReviewLog(**log.dict())
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.get("/review-logs/{log_id}", response_model=ReviewLogResponse)
def get_review_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    return db_log

@api_router.get("/review-logs", response_model=List[ReviewLogResponse])
def get_review_logs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    logs = db.query(ReviewLog).offset(skip).limit(limit).all()
    return logs

@api_router.put("/review-logs/{log_id}", response_model=ReviewLogResponse)
def update_review_log(log_id: int, log: ReviewLogUpdate, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    
    for key, value in log.dict(exclude_unset=True).items():
        setattr(db_log, key, value)
    
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.delete("/review-logs/{log_id}")
def delete_review_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    
    db.delete(db_log)
    db.commit()
    return {"message": "Review log deleted successfully"}

# 精英想法卡片相关模型
class EliteIdeaCardBase(BaseModel):
    daily_bried_id: Optional[int] = None
    origin_concept: Optional[str] = Field(None, max_length=45)
    meta_idea_name: Optional[str] = Field(None, max_length=45)
    meta_explanation: Optional[str] = Field(None, max_length=45)
    create_at: Optional[datetime] = None

class EliteIdeaCardCreate(EliteIdeaCardBase):
    pass

class EliteIdeaCardUpdate(BaseModel):
    daily_bried_id: Optional[int] = None
    origin_concept: Optional[str] = Field(None, max_length=45)
    meta_idea_name: Optional[str] = Field(None, max_length=45)
    meta_explanation: Optional[str] = Field(None, max_length=45)

class EliteIdeaCardResponse(EliteIdeaCardBase):
    id: int
    
    class Config:
        from_attributes = True

# 精英想法案例相关模型
class EliteIdeaCaseBase(BaseModel):
    meta_id: Optional[int] = None
    case_title: Optional[str] = Field(None, max_length=45)
    case_content: Optional[str] = Field(None, max_length=45)
    image_path: Optional[str] = Field(None, max_length=45)
    query_rewrite: Optional[str] = None

class EliteIdeaCaseCreate(EliteIdeaCaseBase):
    pass

class EliteIdeaCaseUpdate(BaseModel):
    meta_id: Optional[int] = None
    case_title: Optional[str] = Field(None, max_length=45)
    case_content: Optional[str] = Field(None, max_length=45)
    image_path: Optional[str] = Field(None, max_length=45)
    query_rewrite: Optional[str] = None

class EliteIdeaCaseResponse(EliteIdeaCaseBase):
    id: int
    
    class Config:
        from_attributes = True

# 外部资源相关模型
class ExternalResourceBase(BaseModel):
    card_id: Optional[int] = None
    title: Optional[str] = Field(None, max_length=45)
    url: Optional[str] = Field(None, max_length=45)
    LLM_context: Optional[str] = None
    source: Optional[str] = Field(None, max_length=45)

class ExternalResourceCreate(ExternalResourceBase):
    pass

class ExternalResourceUpdate(BaseModel):
    card_id: Optional[int] = None
    title: Optional[str] = Field(None, max_length=45)
    url: Optional[str] = Field(None, max_length=45)
    LLM_context: Optional[str] = None
    source: Optional[str] = Field(None, max_length=45)

class ExternalResourceResponse(ExternalResourceBase):
    id: int
    
    class Config:
        from_attributes = True

# 用户截图相关模型
class UserScreenshotBase(BaseModel):
    user_id: Optional[int] = None
    image__path: Optional[str] = Field(None, max_length=45)
    vlm_analysis: Optional[str] = None
    upload_date: Optional[date] = None

class UserScreenshotCreate(UserScreenshotBase):
    pass

class UserScreenshotUpdate(BaseModel):
    user_id: Optional[int] = None
    image__path: Optional[str] = Field(None, max_length=45)
    vlm_analysis: Optional[str] = None
    upload_date: Optional[date] = None

class UserScreenshotResponse(UserScreenshotBase):
    id: int
    
    class Config:
        from_attributes = True

# 关联简报相关模型
class AssociationBriefBase(BaseModel):
    user_id: Optional[int] = None
    type: Optional[str] = Field(None, pattern="^(Auto|Manual)$")
    content: Optional[str] = None
    notes_date: Optional[date] = None
    screenshot_date: Optional[date] = None
    created_at: Optional[datetime] = None

class AssociationBriefCreate(AssociationBriefBase):
    pass

class AssociationBriefUpdate(BaseModel):
    user_id: Optional[int] = None
    type: Optional[str] = Field(None, pattern="^(Auto|Manual)$")
    content: Optional[str] = None
    notes_date: Optional[date] = None
    screenshot_date: Optional[date] = None

class AssociationBriefResponse(AssociationBriefBase):
    id: int
    
    class Config:
        from_attributes = True

# 学者笔记相关模型
class ScholarNoteBase(BaseModel):
    user_id: Optional[int] = None
    notes_content: Optional[str] = None
    target_date: Optional[date] = None
    daily_brief_id: Optional[int] = None

class ScholarNoteCreate(ScholarNoteBase):
    pass

class ScholarNoteUpdate(BaseModel):
    user_id: Optional[int] = None
    notes_content: Optional[str] = None
    target_date: Optional[date] = None
    daily_brief_id: Optional[int] = None

class ScholarNoteResponse(ScholarNoteBase):
    id: int
    
    class Config:
        from_attributes = True

# 知识图谱相关模型
class KnowledgeMapBase(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    map_json: Optional[dict] = None
    created_at: Optional[datetime] = None

class KnowledgeMapCreate(KnowledgeMapBase):
    pass

class KnowledgeMapUpdate(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    map_json: Optional[dict] = None

class KnowledgeMapResponse(KnowledgeMapBase):
    id: int
    
    class Config:
        from_attributes = True

# 图谱交互日志相关模型
class MapInteractionLogBase(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    node_id: Optional[str] = Field(None, max_length=45)
    user_query: Optional[str] = None
    ai_response: Optional[str] = None
    created_at: Optional[datetime] = None
    is_distilled: Optional[int] = None

class MapInteractionLogCreate(MapInteractionLogBase):
    pass

class MapInteractionLogUpdate(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    node_id: Optional[str] = Field(None, max_length=45)
    user_query: Optional[str] = None
    ai_response: Optional[str] = None
    is_distilled: Optional[int] = None

class MapInteractionLogResponse(MapInteractionLogBase):
    id: int
    
    class Config:
        from_attributes = True

# 图谱认知快照相关模型
class MapCognitiveSnapshotBase(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    last_processed_log_id: Optional[int] = None
    snapshot_content: Optional[str] = None
    path_nodes: Optional[dict] = None
    version: Optional[int] = None
    last_log_id: Optional[int] = None

class MapCognitiveSnapshotCreate(MapCognitiveSnapshotBase):
    pass

class MapCognitiveSnapshotUpdate(BaseModel):
    user_id: Optional[int] = None
    source_doc_id: Optional[int] = None
    last_processed_log_id: Optional[int] = None
    snapshot_content: Optional[str] = None
    path_nodes: Optional[dict] = None
    version: Optional[int] = None
    last_log_id: Optional[int] = None

class MapCognitiveSnapshotResponse(MapCognitiveSnapshotBase):
    id: int
    
    class Config:
        from_attributes = True

# API端点
# 用户相关API
@api_router.post("/users", response_model=UserResponse)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = UserInformation(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user

@api_router.get("/users/{user_id}", response_model=UserResponse)
def get_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user

@api_router.get("/users", response_model=List[UserResponse])
def get_users(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    users = db.query(UserInformation).offset(skip).limit(limit).all()
    return users

@api_router.put("/users/{user_id}", response_model=UserResponse)
def update_user(user_id: int, user: UserUpdate, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    for key, value in user.dict(exclude_unset=True).items():
        setattr(db_user, key, value)
    
    db.commit()
    db.refresh(db_user)
    return db_user

@api_router.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    db_user = db.query(UserInformation).filter(UserInformation.id == user_id).first()
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    
    db.delete(db_user)
    db.commit()
    return {"message": "User deleted successfully"}

# 源文档相关API
@api_router.post("/source-documents", response_model=SourceDocumentResponse)
def create_source_document(doc: SourceDocumentCreate, db: Session = Depends(get_db)):
    db_doc = SourceDocument(**doc.dict())
    db.add(db_doc)
    db.commit()
    db.refresh(db_doc)
    return db_doc

@api_router.get("/source-documents/{doc_id}", response_model=SourceDocumentResponse)
def get_source_document(doc_id: int, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    return db_doc

@api_router.get("/source-documents", response_model=List[SourceDocumentResponse])
def get_source_documents(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    docs = db.query(SourceDocument).offset(skip).limit(limit).all()
    return docs

@api_router.put("/source-documents/{doc_id}", response_model=SourceDocumentResponse)
def update_source_document(doc_id: int, doc: SourceDocumentUpdate, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    
    for key, value in doc.dict(exclude_unset=True).items():
        setattr(db_doc, key, value)
    
    db.commit()
    db.refresh(db_doc)
    return db_doc

@api_router.delete("/source-documents/{doc_id}")
def delete_source_document(doc_id: int, db: Session = Depends(get_db)):
    db_doc = db.query(SourceDocument).filter(SourceDocument.id == doc_id).first()
    if db_doc is None:
        raise HTTPException(status_code=404, detail="Source document not found")
    
    db.delete(db_doc)
    db.commit()
    return {"message": "Source document deleted successfully"}

# 每日简报相关API
@api_router.post("/daily-briefs", response_model=DailyBriefResponse)
def create_daily_brief(brief: DailyBriefCreate, db: Session = Depends(get_db)):
    db_brief = DailyBrief(**brief.dict())
    db.add(db_brief)
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.get("/daily-briefs/{brief_id}", response_model=DailyBriefResponse)
def get_daily_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    return db_brief

@api_router.get("/daily-briefs", response_model=List[DailyBriefResponse])
def get_daily_briefs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    briefs = db.query(DailyBrief).offset(skip).limit(limit).all()
    return briefs

@api_router.put("/daily-briefs/{brief_id}", response_model=DailyBriefResponse)
def update_daily_brief(brief_id: int, brief: DailyBriefUpdate, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    
    for key, value in brief.dict(exclude_unset=True).items():
        setattr(db_brief, key, value)
    
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.delete("/daily-briefs/{brief_id}")
def delete_daily_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(DailyBrief).filter(DailyBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Daily brief not found")
    
    db.delete(db_brief)
    db.commit()
    return {"message": "Daily brief deleted successfully"}

# 复习日志相关API
@api_router.post("/review-logs", response_model=ReviewLogResponse)
def create_review_log(log: ReviewLogCreate, db: Session = Depends(get_db)):
    db_log = ReviewLog(**log.dict())
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.get("/review-logs/{log_id}", response_model=ReviewLogResponse)
def get_review_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    return db_log

@api_router.get("/review-logs", response_model=List[ReviewLogResponse])
def get_review_logs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    logs = db.query(ReviewLog).offset(skip).limit(limit).all()
    return logs

@api_router.put("/review-logs/{log_id}", response_model=ReviewLogResponse)
def update_review_log(log_id: int, log: ReviewLogUpdate, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    
    for key, value in log.dict(exclude_unset=True).items():
        setattr(db_log, key, value)
    
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.delete("/review-logs/{log_id}")
def delete_review_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(ReviewLog).filter(ReviewLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Review log not found")
    
    db.delete(db_log)
    db.commit()
    return {"message": "Review log deleted successfully"}

# 精英想法卡片相关API
@api_router.post("/elite-idea-cards", response_model=EliteIdeaCardResponse)
def create_elite_idea_card(card: EliteIdeaCardCreate, db: Session = Depends(get_db)):
    db_card = EliteIdeaCard(**card.dict())
    db.add(db_card)
    db.commit()
    db.refresh(db_card)
    return db_card

@api_router.get("/elite-idea-cards/{card_id}", response_model=EliteIdeaCardResponse)
def get_elite_idea_card(card_id: int, db: Session = Depends(get_db)):
    db_card = db.query(EliteIdeaCard).filter(EliteIdeaCard.id == card_id).first()
    if db_card is None:
        raise HTTPException(status_code=404, detail="Elite idea card not found")
    return db_card

@api_router.get("/elite-idea-cards", response_model=List[EliteIdeaCardResponse])
def get_elite_idea_cards(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    cards = db.query(EliteIdeaCard).offset(skip).limit(limit).all()
    return cards

@api_router.put("/elite-idea-cards/{card_id}", response_model=EliteIdeaCardResponse)
def update_elite_idea_card(card_id: int, card: EliteIdeaCardUpdate, db: Session = Depends(get_db)):
    db_card = db.query(EliteIdeaCard).filter(EliteIdeaCard.id == card_id).first()
    if db_card is None:
        raise HTTPException(status_code=404, detail="Elite idea card not found")
    
    for key, value in card.dict(exclude_unset=True).items():
        setattr(db_card, key, value)
    
    db.commit()
    db.refresh(db_card)
    return db_card

@api_router.delete("/elite-idea-cards/{card_id}")
def delete_elite_idea_card(card_id: int, db: Session = Depends(get_db)):
    db_card = db.query(EliteIdeaCard).filter(EliteIdeaCard.id == card_id).first()
    if db_card is None:
        raise HTTPException(status_code=404, detail="Elite idea card not found")
    
    db.delete(db_card)
    db.commit()
    return {"message": "Elite idea card deleted successfully"}

# 精英想法案例相关API
@api_router.post("/elite-idea-cases", response_model=EliteIdeaCaseResponse)
def create_elite_idea_case(case: EliteIdeaCaseCreate, db: Session = Depends(get_db)):
    db_case = EliteIdeaCase(**case.dict())
    db.add(db_case)
    db.commit()
    db.refresh(db_case)
    return db_case

@api_router.get("/elite-idea-cases/{case_id}", response_model=EliteIdeaCaseResponse)
def get_elite_idea_case(case_id: int, db: Session = Depends(get_db)):
    db_case = db.query(EliteIdeaCase).filter(EliteIdeaCase.id == case_id).first()
    if db_case is None:
        raise HTTPException(status_code=404, detail="Elite idea case not found")
    return db_case

@api_router.get("/elite-idea-cases", response_model=List[EliteIdeaCaseResponse])
def get_elite_idea_cases(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    cases = db.query(EliteIdeaCase).offset(skip).limit(limit).all()
    return cases

@api_router.put("/elite-idea-cases/{case_id}", response_model=EliteIdeaCaseResponse)
def update_elite_idea_case(case_id: int, case: EliteIdeaCaseUpdate, db: Session = Depends(get_db)):
    db_case = db.query(EliteIdeaCase).filter(EliteIdeaCase.id == case_id).first()
    if db_case is None:
        raise HTTPException(status_code=404, detail="Elite idea case not found")
    
    for key, value in case.dict(exclude_unset=True).items():
        setattr(db_case, key, value)
    
    db.commit()
    db.refresh(db_case)
    return db_case

@api_router.delete("/elite-idea-cases/{case_id}")
def delete_elite_idea_case(case_id: int, db: Session = Depends(get_db)):
    db_case = db.query(EliteIdeaCase).filter(EliteIdeaCase.id == case_id).first()
    if db_case is None:
        raise HTTPException(status_code=404, detail="Elite idea case not found")
    
    db.delete(db_case)
    db.commit()
    return {"message": "Elite idea case deleted successfully"}

# 外部资源相关API
@api_router.post("/external-resources", response_model=ExternalResourceResponse)
def create_external_resource(resource: ExternalResourceCreate, db: Session = Depends(get_db)):
    db_resource = ExternalResource(**resource.dict())
    db.add(db_resource)
    db.commit()
    db.refresh(db_resource)
    return db_resource

@api_router.get("/external-resources/{resource_id}", response_model=ExternalResourceResponse)
def get_external_resource(resource_id: int, db: Session = Depends(get_db)):
    db_resource = db.query(ExternalResource).filter(ExternalResource.id == resource_id).first()
    if db_resource is None:
        raise HTTPException(status_code=404, detail="External resource not found")
    return db_resource

@api_router.get("/external-resources", response_model=List[ExternalResourceResponse])
def get_external_resources(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    resources = db.query(ExternalResource).offset(skip).limit(limit).all()
    return resources

@api_router.put("/external-resources/{resource_id}", response_model=ExternalResourceResponse)
def update_external_resource(resource_id: int, resource: ExternalResourceUpdate, db: Session = Depends(get_db)):
    db_resource = db.query(ExternalResource).filter(ExternalResource.id == resource_id).first()
    if db_resource is None:
        raise HTTPException(status_code=404, detail="External resource not found")
    
    for key, value in resource.dict(exclude_unset=True).items():
        setattr(db_resource, key, value)
    
    db.commit()
    db.refresh(db_resource)
    return db_resource

@api_router.delete("/external-resources/{resource_id}")
def delete_external_resource(resource_id: int, db: Session = Depends(get_db)):
    db_resource = db.query(ExternalResource).filter(ExternalResource.id == resource_id).first()
    if db_resource is None:
        raise HTTPException(status_code=404, detail="External resource not found")
    
    db.delete(db_resource)
    db.commit()
    return {"message": "External resource deleted successfully"}

# 用户截图相关API
@api_router.post("/user-screenshots", response_model=UserScreenshotResponse)
def create_user_screenshot(screenshot: UserScreenshotCreate, db: Session = Depends(get_db)):
    db_screenshot = UserScreenshot(**screenshot.dict())
    db.add(db_screenshot)
    db.commit()
    db.refresh(db_screenshot)
    return db_screenshot

@api_router.get("/user-screenshots/{screenshot_id}", response_model=UserScreenshotResponse)
def get_user_screenshot(screenshot_id: int, db: Session = Depends(get_db)):
    db_screenshot = db.query(UserScreenshot).filter(UserScreenshot.id == screenshot_id).first()
    if db_screenshot is None:
        raise HTTPException(status_code=404, detail="User screenshot not found")
    return db_screenshot

@api_router.get("/user-screenshots", response_model=List[UserScreenshotResponse])
def get_user_screenshots(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    screenshots = db.query(UserScreenshot).offset(skip).limit(limit).all()
    return screenshots

@api_router.put("/user-screenshots/{screenshot_id}", response_model=UserScreenshotResponse)
def update_user_screenshot(screenshot_id: int, screenshot: UserScreenshotUpdate, db: Session = Depends(get_db)):
    db_screenshot = db.query(UserScreenshot).filter(UserScreenshot.id == screenshot_id).first()
    if db_screenshot is None:
        raise HTTPException(status_code=404, detail="User screenshot not found")
    
    for key, value in screenshot.dict(exclude_unset=True).items():
        setattr(db_screenshot, key, value)
    
    db.commit()
    db.refresh(db_screenshot)
    return db_screenshot

@api_router.delete("/user-screenshots/{screenshot_id}")
def delete_user_screenshot(screenshot_id: int, db: Session = Depends(get_db)):
    db_screenshot = db.query(UserScreenshot).filter(UserScreenshot.id == screenshot_id).first()
    if db_screenshot is None:
        raise HTTPException(status_code=404, detail="User screenshot not found")
    
    db.delete(db_screenshot)
    db.commit()
    return {"message": "User screenshot deleted successfully"}

# 关联简报相关API
@api_router.post("/association-briefs", response_model=AssociationBriefResponse)
def create_association_brief(brief: AssociationBriefCreate, db: Session = Depends(get_db)):
    db_brief = AssociationBrief(**brief.dict())
    db.add(db_brief)
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.get("/association-briefs/{brief_id}", response_model=AssociationBriefResponse)
def get_association_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(AssociationBrief).filter(AssociationBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Association brief not found")
    return db_brief

@api_router.get("/association-briefs", response_model=List[AssociationBriefResponse])
def get_association_briefs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    briefs = db.query(AssociationBrief).offset(skip).limit(limit).all()
    return briefs

@api_router.put("/association-briefs/{brief_id}", response_model=AssociationBriefResponse)
def update_association_brief(brief_id: int, brief: AssociationBriefUpdate, db: Session = Depends(get_db)):
    db_brief = db.query(AssociationBrief).filter(AssociationBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Association brief not found")
    
    for key, value in brief.dict(exclude_unset=True).items():
        setattr(db_brief, key, value)
    
    db.commit()
    db.refresh(db_brief)
    return db_brief

@api_router.delete("/association-briefs/{brief_id}")
def delete_association_brief(brief_id: int, db: Session = Depends(get_db)):
    db_brief = db.query(AssociationBrief).filter(AssociationBrief.id == brief_id).first()
    if db_brief is None:
        raise HTTPException(status_code=404, detail="Association brief not found")
    
    db.delete(db_brief)
    db.commit()
    return {"message": "Association brief deleted successfully"}

# 学者笔记相关API
@api_router.post("/scholar-notes", response_model=ScholarNoteResponse)
def create_scholar_note(note: ScholarNoteCreate, db: Session = Depends(get_db)):
    db_note = ScholarNote(**note.dict())
    db.add(db_note)
    db.commit()
    db.refresh(db_note)
    return db_note

@api_router.get("/scholar-notes/{note_id}", response_model=ScholarNoteResponse)
def get_scholar_note(note_id: int, db: Session = Depends(get_db)):
    db_note = db.query(ScholarNote).filter(ScholarNote.id == note_id).first()
    if db_note is None:
        raise HTTPException(status_code=404, detail="Scholar note not found")
    return db_note

@api_router.get("/scholar-notes", response_model=List[ScholarNoteResponse])
def get_scholar_notes(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    notes = db.query(ScholarNote).offset(skip).limit(limit).all()
    return notes

@api_router.put("/scholar-notes/{note_id}", response_model=ScholarNoteResponse)
def update_scholar_note(note_id: int, note: ScholarNoteUpdate, db: Session = Depends(get_db)):
    db_note = db.query(ScholarNote).filter(ScholarNote.id == note_id).first()
    if db_note is None:
        raise HTTPException(status_code=404, detail="Scholar note not found")
    
    for key, value in note.dict(exclude_unset=True).items():
        setattr(db_note, key, value)
    
    db.commit()
    db.refresh(db_note)
    return db_note

@api_router.delete("/scholar-notes/{note_id}")
def delete_scholar_note(note_id: int, db: Session = Depends(get_db)):
    db_note = db.query(ScholarNote).filter(ScholarNote.id == note_id).first()
    if db_note is None:
        raise HTTPException(status_code=404, detail="Scholar note not found")
    
    db.delete(db_note)
    db.commit()
    return {"message": "Scholar note deleted successfully"}

# 知识图谱相关API
@api_router.post("/knowledge-maps", response_model=KnowledgeMapResponse)
def create_knowledge_map(km: KnowledgeMapCreate, db: Session = Depends(get_db)):
    db_km = KnowledgeMap(**km.dict())
    db.add(db_km)
    db.commit()
    db.refresh(db_km)
    return db_km

@api_router.get("/knowledge-maps/{km_id}", response_model=KnowledgeMapResponse)
def get_knowledge_map(km_id: int, db: Session = Depends(get_db)):
    db_km = db.query(KnowledgeMap).filter(KnowledgeMap.id == km_id).first()
    if db_km is None:
        raise HTTPException(status_code=404, detail="Knowledge map not found")
    return db_km

@api_router.get("/knowledge-maps", response_model=List[KnowledgeMapResponse])
def get_knowledge_maps(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    kmaps = db.query(KnowledgeMap).offset(skip).limit(limit).all()
    return kmaps

@api_router.put("/knowledge-maps/{km_id}", response_model=KnowledgeMapResponse)
def update_knowledge_map(km_id: int, km: KnowledgeMapUpdate, db: Session = Depends(get_db)):
    db_km = db.query(KnowledgeMap).filter(KnowledgeMap.id == km_id).first()
    if db_km is None:
        raise HTTPException(status_code=404, detail="Knowledge map not found")
    
    for key, value in km.dict(exclude_unset=True).items():
        setattr(db_km, key, value)
    
    db.commit()
    db.refresh(db_km)
    return db_km

@api_router.delete("/knowledge-maps/{km_id}")
def delete_knowledge_map(km_id: int, db: Session = Depends(get_db)):
    db_km = db.query(KnowledgeMap).filter(KnowledgeMap.id == km_id).first()
    if db_km is None:
        raise HTTPException(status_code=404, detail="Knowledge map not found")
    
    db.delete(db_km)
    db.commit()
    return {"message": "Knowledge map deleted successfully"}

# 图谱交互日志相关API
@api_router.post("/map-interaction-logs", response_model=MapInteractionLogResponse)
def create_map_interaction_log(log: MapInteractionLogCreate, db: Session = Depends(get_db)):
    db_log = MapInteractionLog(**log.dict())
    db.add(db_log)
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.get("/map-interaction-logs/{log_id}", response_model=MapInteractionLogResponse)
def get_map_interaction_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(MapInteractionLog).filter(MapInteractionLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Map interaction log not found")
    return db_log

@api_router.get("/map-interaction-logs", response_model=List[MapInteractionLogResponse])
def get_map_interaction_logs(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    logs = db.query(MapInteractionLog).offset(skip).limit(limit).all()
    return logs

@api_router.put("/map-interaction-logs/{log_id}", response_model=MapInteractionLogResponse)
def update_map_interaction_log(log_id: int, log: MapInteractionLogUpdate, db: Session = Depends(get_db)):
    db_log = db.query(MapInteractionLog).filter(MapInteractionLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Map interaction log not found")
    
    for key, value in log.dict(exclude_unset=True).items():
        setattr(db_log, key, value)
    
    db.commit()
    db.refresh(db_log)
    return db_log

@api_router.delete("/map-interaction-logs/{log_id}")
def delete_map_interaction_log(log_id: int, db: Session = Depends(get_db)):
    db_log = db.query(MapInteractionLog).filter(MapInteractionLog.id == log_id).first()
    if db_log is None:
        raise HTTPException(status_code=404, detail="Map interaction log not found")
    
    db.delete(db_log)
    db.commit()
    return {"message": "Map interaction log deleted successfully"}

# 图谱认知快照相关API
@api_router.post("/map-cognitive-snapshots", response_model=MapCognitiveSnapshotResponse)
def create_map_cognitive_snapshot(snapshot: MapCognitiveSnapshotCreate, db: Session = Depends(get_db)):
    db_snapshot = MapCognitiveSnapshot(**snapshot.dict())
    db.add(db_snapshot)
    db.commit()
    db.refresh(db_snapshot)
    return db_snapshot

@api_router.get("/map-cognitive-snapshots/{snapshot_id}", response_model=MapCognitiveSnapshotResponse)
def get_map_cognitive_snapshot(snapshot_id: int, db: Session = Depends(get_db)):
    db_snapshot = db.query(MapCognitiveSnapshot).filter(MapCognitiveSnapshot.id == snapshot_id).first()
    if db_snapshot is None:
        raise HTTPException(status_code=404, detail="Map cognitive snapshot not found")
    return db_snapshot

@api_router.get("/map-cognitive-snapshots", response_model=List[MapCognitiveSnapshotResponse])
def get_map_cognitive_snapshots(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    snapshots = db.query(MapCognitiveSnapshot).offset(skip).limit(limit).all()
    return snapshots

@api_router.put("/map-cognitive-snapshots/{snapshot_id}", response_model=MapCognitiveSnapshotResponse)
def update_map_cognitive_snapshot(snapshot_id: int, snapshot: MapCognitiveSnapshotUpdate, db: Session = Depends(get_db)):
    db_snapshot = db.query(MapCognitiveSnapshot).filter(MapCognitiveSnapshot.id == snapshot_id).first()
    if db_snapshot is None:
        raise HTTPException(status_code=404, detail="Map cognitive snapshot not found")
    
    for key, value in snapshot.dict(exclude_unset=True).items():
        setattr(db_snapshot, key, value)
    
    db.commit()
    db.refresh(db_snapshot)
    return db_snapshot

@api_router.delete("/map-cognitive-snapshots/{snapshot_id}")
def delete_map_cognitive_snapshot(snapshot_id: int, db: Session = Depends(get_db)):
    db_snapshot = db.query(MapCognitiveSnapshot).filter(MapCognitiveSnapshot.id == snapshot_id).first()
    if db_snapshot is None:
        raise HTTPException(status_code=404, detail="Map cognitive snapshot not found")
    
    db.delete(db_snapshot)
    db.commit()
    return {"message": "Map cognitive snapshot deleted successfully"}


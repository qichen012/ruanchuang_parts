"""
GitHub 仓库搜索 API 服务
提供关键词搜索和每日简报搜索功能
"""
from fastapi import FastAPI, HTTPException, Query, Depends
from pydantic import BaseModel, Field
from typing import List, Dict, Optional, Any
from datetime import date
import uvicorn
import os
import sys

# 添加当前目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from github_search import search_repos_by_keyword, search_repos_by_report


# ============ FastAPI 应用 ============
app = FastAPI(
    title="GitHub 仓库搜索 API",
    description="提供关键词搜索和每日简报搜索 GitHub 仓库功能",
    version="1.0.0"
)


# ============ 数据库连接 ============
def get_db_connection():
    """获取数据库连接"""
    try:
        import pymysql
        db_url = os.getenv("DATABASE_URL", "mysql+pymysql://root:password@localhost:3306/Learning_DB")
        # 解析数据库URL
        db_config = {
            "host": db_url.split("@")[1].split(":")[0],
            "port": int(db_url.split(":")[2].split("/")[0]),
            "user": db_url.split("//")[1].split(":")[0],
            "password": db_url.split(":")[2].split("@")[0],
            "database": db_url.split("/")[-1],
            "charset": "utf8mb4"
        }
        return pymysql.connect(**db_config)
    except Exception as e:
        print(f"数据库连接失败: {e}")
        return None


def get_daily_brief_from_db(brief_id: int = None, user_id: int = None, target_date: date = None):
    """从数据库获取每日简报"""
    db = get_db_connection()
    if not db:
        return None

    try:
        cursor = db.cursor(pymysql.cursors.DictCursor)

        if brief_id:
            cursor.execute("SELECT * FROM daily_briefs WHERE id = %s", (brief_id,))
        elif user_id and target_date:
            cursor.execute(
                "SELECT * FROM daily_briefs WHERE user_id = %s AND target_date = %s",
                (user_id, target_date)
            )
        elif user_id:
            cursor.execute("SELECT * FROM daily_briefs WHERE user_id = %s ORDER BY target_date DESC LIMIT 1", (user_id,))
        else:
            cursor.execute("SELECT * FROM daily_briefs ORDER BY id DESC LIMIT 1")

        result = cursor.fetchone()
        db.close()
        return result
    except Exception as e:
        print(f"查询每日简报失败: {e}")
        if db:
            db.close()
        return None


# ============ 请求/响应模型 ============
class GitHubSearchRequest(BaseModel):
    """GitHub仓库关键词搜索请求"""
    keyword: str = Field(..., description="搜索关键词")
    language: str = Field(default="python", description="编程语言筛选")
    min_stars: int = Field(default=1000, description="最少星标数")
    limit: int = Field(default=10, description="返回结果数量")


class GitHubSearchByReportRequest(BaseModel):
    """每日简报搜索请求"""
    report: str = Field(..., description="每日简报文本")
    language: str = Field(default="", description="编程语言筛选")
    min_stars: int = Field(default=1000, description="最少星标数")
    limit: int = Field(default=10, description="返回结果数量")


class GitHubSearchByBriefIdRequest(BaseModel):
    """根据数据库每日简报ID搜索请求"""
    brief_id: Optional[int] = Field(default=None, description="每日简报ID")
    user_id: Optional[int] = Field(default=None, description="用户ID")
    target_date: Optional[str] = Field(default=None, description="日期 YYYY-MM-DD")
    language: str = Field(default="", description="编程语言筛选")
    min_stars: int = Field(default=1000, description="最少星标数")
    limit: int = Field(default=10, description="返回结果数量")


class GitHubSearchResponse(BaseModel):
    """GitHub仓库搜索响应"""
    success: bool
    keyword: Optional[str] = None
    brief_id: Optional[int] = None
    language: str
    count: int
    repos: List[Dict[str, Any]]


# ============ API 端点 ============
@app.post("/github/search", response_model=GitHubSearchResponse)
def github_search(request: GitHubSearchRequest):
    """根据关键词搜索GitHub仓库"""
    repos = search_repos_by_keyword(
        keyword=request.keyword,
        language=request.language,
        min_stars=request.min_stars,
        limit=request.limit
    )
    return GitHubSearchResponse(
        success=True,
        keyword=request.keyword,
        language=request.language,
        count=len(repos),
        repos=repos
    )


@app.post("/github/search-by-report", response_model=GitHubSearchResponse)
def github_search_by_report(request: GitHubSearchByReportRequest):
    """根据每日简报文本搜索GitHub仓库"""
    repos = search_repos_by_report(
        report_text=request.report,
        language=request.language,
        min_stars=request.min_stars,
        limit=request.limit
    )
    return GitHubSearchResponse(
        success=True,
        language=request.language,
        count=len(repos),
        repos=repos
    )


@app.post("/github/search-by-brief", response_model=GitHubSearchResponse)
def github_search_by_brief(request: GitHubSearchByBriefIdRequest):
    """根据数据库中的每日简报搜索GitHub仓库"""
    # 从数据库获取每日简报
    brief = get_daily_brief_from_db(
        brief_id=request.brief_id,
        user_id=request.user_id,
        target_date=request.target_date
    )

    if not brief:
        raise HTTPException(status_code=404, detail="未找到每日简报")

    # 组合简报内容用于搜索
    report_text = ""
    if brief.get("posterior_insight"):
        report_text += brief["posterior_insight"] + " "
    if brief.get("key_concepts"):
        report_text += brief["key_concepts"]

    if not report_text.strip():
        raise HTTPException(status_code=400, detail="每日简报内容为空")

    repos = search_repos_by_report(
        report_text=report_text,
        language=request.language,
        min_stars=request.min_stars,
        limit=request.limit
    )

    return GitHubSearchResponse(
        success=True,
        brief_id=brief.get("id"),
        language=request.language,
        count=len(repos),
        repos=repos
    )


# ============ 启动服务 ============
if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    uvicorn.run(app, host="0.0.0.0", port=port)

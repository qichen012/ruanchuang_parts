# Learning Management System API 文档

本 API 文档描述了学习管理系统的所有可用端点、数据模型和认证机制。该后端基于 FastAPI 框架开发。

## 基础信息
- **API 版本**: v1
- **基础 URL**: http://127.0.0.1:8000/api/v1
- **协议**: HTTPS/HTTP
- **内容类型**: application/json

## 认证机制
本系统使用 **JWT (JSON Web Token)** 进行身份验证。
- 注册 (`/register`) 和登录 (`/login`) 端点不需要身份验证。
- 对于受保护的操作，应在 HTTP 请求头中附带 Token：
  `Authorization: Bearer <your_access_token>`
- Token 默认有效时长为 7 天。

---

## 数据模型

以下模型字段精确映射了 API 请求和响应中的 JSON 结构及数据库表结构。

### UserInformation (用户信息)
- **id**: Integer (主键)
- **email**: String (邮箱账号，唯一)
- **password**: String (仅注册/登录时使用，加密存储，API 响应不返回)
- **name**: String (最大长度 45)
- **gender**: Enum ('male', 'female')
- **age**: Integer

### SourceDocument (源文档)
- **id**: Integer (主键)
- **user_id**: Integer (外键，关联 UserInformation)
- **file_name**: String (最大长度 255)
- **file_path**: String (最大长度 500)
- **upload_date**: Date (YYYY-MM-DD)
- **processed_status**: Enum ('Pending', 'Done', 'Failed')

### DailyBrief (每日简报)
- **id**: Integer (主键)
- **user_id**: Integer (外键，关联 UserInformation)
- **target_date**: Date
- **posterior_insight**: Text (今日洞见)
- **key_concepts**: Text (核心概念总结)
- **created_at**: DateTime
- **next_review_date**: Date (下次复习日期)
- **review_stage**: Integer (复习阶段)
- **user_reflect**: Text (用户反思)
- **source_handouts**: JSON (来源材料名称列表)
- **handout_count**: Integer (材料数量)
- **process_time**: String (处理耗时，如 "9.90s")

### ReviewLog (复习日志)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **brief_id**: Integer (外键，关联 DailyBrief)
- **review_at**: DateTime
- **feynman_score**: Integer

### EliteIdeaCard (精英想法卡片)
- **id**: Integer (主键)
- **daily_brief_id**: Integer (外键，关联 DailyBrief)
- **origin_concept**: String (最大长度 45)
- **meta_idea_name**: String (最大长度 45)
- **meta_explanation**: String (最大长度 45)
- **create_at**: DateTime

### EliteIdeaCase (精英想法案例)
- **id**: Integer (主键)
- **meta_id**: Integer (外键，关联 EliteIdeaCard)
- **case_title**: String (最大长度 45)
- **case_content**: String (最大长度 45)
- **image_path**: String (最大长度 45)
- **query_rewrite**: Text (最大长度 100)

### ExternalResource (外部资源)
- **id**: Integer (主键)
- **card_id**: Integer (外键，关联 EliteIdeaCard)
- **title**: String (最大长度 45)
- **url**: String (最大长度 45)
- **LLM_context**: Text (最大长度 100)
- **source**: String (最大长度 45)

### UserScreenshot (用户截图)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **image_path**: String (最大长度 45)
- **vlm_analysis**: Text (最大长度 100)
- **upload_date**: Date

### AssociationBrief (关联简报)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **type**: Enum ('Auto', 'Manual')
- **content**: Text (最大长度 100)
- **notes_date**: Date
- **screenshot_date**: Date
- **created_at**: DateTime

### ScholarNote (学者笔记)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **notes_content**: Text (最大长度 100)
- **target_date**: Date
- **daily_brief_id**: Integer (外键)

### KnowledgeMap (知识图谱)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **source_doc_id**: Integer (外键，关联 SourceDocument)
- **map_json**: JSON
- **created_at**: DateTime

### MapInteractionLog (图谱交互日志)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **source_doc_id**: Integer (外键)
- **node_id**: String (最大长度 45)
- **user_query**: Text
- **ai_response**: Text (最大长度 100)
- **created_at**: DateTime
- **is_distilled**: Integer

### MapCognitiveSnapshot (图谱认知快照)
- **id**: Integer (主键)
- **user_id**: Integer (外键)
- **source_doc_id**: Integer (外键)
- **last_processed_log_id**: Integer
- **snapshot_content**: Text (最大长度 100)
- **path_nodes**: JSON
- **version**: Integer
- **last_log_id**: Integer

### AppUsageLog (App使用时段日志)
- **id**: Integer (主键，自增)
- **user_id**: Integer (外键，关联 UserInformation)
- **start_time**: DateTime (本次进入 App 的时间)
- **end_time**: DateTime (本次离开 App 的时间)
- **duration_seconds**: Integer (使用时长，单位：秒)

---

## API 端点

### 认证管理 

#### 用户注册
- **POST** `/register`
- **请求体**: 
  ```json
  {
    "email": "user@example.com",
    "password": "secure_password"
  }
- **响应**: 200 OK
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
      "message": "注册成功",
      "code": 200
    }
    ```

#### 用户登录
- **POST** `/login`
- **请求体**:
    ```JSON
    {
      "email": "user@example.com",
      "password": "secure_password"
    }```
- **响应**: 200 OK
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
      "message": "登录成功",
      "code": 200
    }
    ```

### 用户信息管理

#### 获取所有用户
- **GET** `/users`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 用户对象列表

#### 获取特定用户
- **GET** `/users/{user_id}`
- **参数**: `user_id` (路径参数)
- **响应**: 用户对象

#### 创建用户
- **POST** `/users`
- **请求体**: 
  ```json
  {
    "id": 1,
    "name": "张三",
    "gender": "male",
    "age": 25
  }
  ```
- **响应**: 创建的用户对象

#### 更新用户
- **PUT** `/users/{user_id}`
- **参数**: `user_id` (路径参数)
- **请求体**: 
  ```json
  {
    "name": "李四",
    "gender": "female",
    "age": 30
  }
  ```
- **响应**: 更新的用户对象

#### 删除用户
- **DELETE** `/users/{user_id}`
- **参数**: `user_id` (路径参数)
- **响应**: 成功消息

### 源文档管理

#### 获取所有源文档
- **GET** `/source-documents`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 源文档对象列表

#### 获取特定源文档
- **GET** `/source-documents/{doc_id}`
- **参数**: `doc_id` (路径参数)
- **响应**: 源文档对象

#### 创建源文档
- **POST** `/source-documents`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "file_name": "example.pdf",
    "file_path": "/path/to/file",
    "upload_date": "2023-10-01",
    "processed_status": "Pending"
  }
  ```
- **响应**: 创建的源文档对象

#### 更新源文档
- **PUT** `/source-documents/{doc_id}`
- **参数**: `doc_id` (路径参数)
- **请求体**: 
  ```json
  {
    "processed_status": "Done"
  }
  ```
- **响应**: 更新的源文档对象

#### 删除源文档
- **DELETE** `/source-documents/{doc_id}`
- **参数**: `doc_id` (路径参数)
- **响应**: 成功消息

### 每日简报管理

#### 获取所有每日简报
- **GET** `/daily-briefs`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 每日简报对象列表

#### 获取特定每日简报
- **GET** `/daily-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **响应**: 每日简报对象

#### 创建每日简报
- **POST** `/daily-briefs`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "target_date": "2026-03-09",
    "posterior_insight": "今日学习内容聚焦于...",
    "key_concepts": "1. 今日学习总览\n...",
    "created_at": "2026-03-09T02:47:17.404697+00:00",
    "next_review_date": "2026-03-10",
    "review_stage": 0,
    "user_reflect": "",
    "source_handouts": ["数据科学理论基础：机器学习"],
    "handout_count": 1,
    "process_time": "9.90s"
  }
  ```
- **响应**: 创建的每日简报对象

#### 更新每日简报
- **PUT** `/daily-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **请求体**: 
  ```json
  {
    "title": "更新后的标题",
    "content": "更新后的内容"
  }
  ```
- **响应**: 更新的每日简报对象

#### 删除每日简报
- **DELETE** `/daily-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **响应**: 成功消息

### 复习日志管理

#### 获取所有复习日志
- **GET** `/review-logs`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 复习日志对象列表

#### 获取特定复习日志
- **GET** `/review-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **响应**: 复习日志对象

#### 创建复习日志
- **POST** `/review-logs`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "document_id": 2,
    "review_date": "2023-10-01",
    "review_stage": 1,
    "User_reflect": "复习心得..."
  }
  ```
- **响应**: 创建的复习日志对象

#### 更新复习日志
- **PUT** `/review-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **请求体**: 
  ```json
  {
    "review_stage": 2,
    "User_reflect": "更新的复习心得"
  }
  ```
- **响应**: 更新的复习日志对象

#### 删除复习日志
- **DELETE** `/review-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **响应**: 成功消息

### 精英想法卡片管理

#### 获取所有精英想法卡片
- **GET** `/elite-idea-cards`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 精英想法卡片对象列表

#### 获取特定精英想法卡片
- **GET** `/elite-idea-cards/{card_id}`
- **参数**: `card_id` (路径参数)
- **响应**: 精英想法卡片对象

#### 创建精英想法卡片
- **POST** `/elite-idea-cards`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "card_title": "创新想法",
    "card_content": "这是一个创新的想法...",
    "category": "技术",
    "created_at": "2023-10-01T10:00:00"
  }
  ```
- **响应**: 创建的精英想法卡片对象

#### 更新精英想法卡片
- **PUT** `/elite-idea-cards/{card_id}`
- **参数**: `card_id` (路径参数)
- **请求体**: 
  ```json
  {
    "card_title": "更新的想法标题",
    "card_content": "更新的想法内容"
  }
  ```
- **响应**: 更新的精英想法卡片对象

#### 删除精英想法卡片
- **DELETE** `/elite-idea-cards/{card_id}`
- **参数**: `card_id` (路径参数)
- **响应**: 成功消息

### 精英想法案例管理

#### 获取所有精英想法案例
- **GET** `/elite-idea-cases`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 精英想法案例对象列表

#### 获取特定精英想法案例
- **GET** `/elite-idea-cases/{case_id}`
- **参数**: `case_id` (路径参数)
- **响应**: 精英想法案例对象

#### 创建精英想法案例
- **POST** `/elite-idea-cases`
- **请求体**: 
  ```json
  {
    "case_title": "优秀案例",
    "case_content": "这是一个优秀的案例...",
    "source": "内部",
    "applied_field": "教育"
  }
  ```
- **响应**: 创建的精英想法案例对象

#### 更新精英想法案例
- **PUT** `/elite-idea-cases/{case_id}`
- **参数**: `case_id` (路径参数)
- **请求体**: 
  ```json
  {
    "case_title": "更新的案例标题",
    "case_content": "更新的案例内容"
  }
  ```
- **响应**: 更新的精英想法案例对象

#### 删除精英想法案例
- **DELETE** `/elite-idea-cases/{case_id}`
- **参数**: `case_id` (路径参数)
- **响应**: 成功消息

### 外部资源管理

#### 获取所有外部资源
- **GET** `/external-resources`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 外部资源对象列表

#### 获取特定外部资源
- **GET** `/external-resources/{resource_id}`
- **参数**: `resource_id` (路径参数)
- **响应**: 外部资源对象

#### 创建外部资源
- **POST** `/external-resources`
- **请求体**: 
  ```json
  {
    "resource_title": "优质资源",
    "resource_url": "https://example.com",
    "description": "这是一个优质的外部资源",
    "category": "学习资料"
  }
  ```
- **响应**: 创建的外部资源对象

#### 更新外部资源
- **PUT** `/external-resources/{resource_id}`
- **参数**: `resource_id` (路径参数)
- **请求体**: 
  ```json
  {
    "resource_title": "更新的资源标题",
    "description": "更新的描述"
  }
  ```
- **响应**: 更新的外部资源对象

#### 删除外部资源
- **DELETE** `/external-resources/{resource_id}`
- **参数**: `resource_id` (路径参数)
- **响应**: 成功消息

### 用户截图管理

#### 获取所有用户截图
- **GET** `/user-screenshots`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 用户截图对象列表

#### 获取特定用户截图
- **GET** `/user-screenshots/{screenshot_id}`
- **参数**: `screenshot_id` (路径参数)
- **响应**: 用户截图对象

#### 创建用户截图
- **POST** `/user-screenshots`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "screenshot_path": "/path/to/screenshot.png",
    "capture_date": "2023-10-01",
    "related_document_id": 2
  }
  ```
- **响应**: 创建的用户截图对象

#### 更新用户截图
- **PUT** `/user-screenshots/{screenshot_id}`
- **参数**: `screenshot_id` (路径参数)
- **请求体**: 
  ```json
  {
    "screenshot_path": "/path/to/new_screenshot.png"
  }
  ```
- **响应**: 更新的用户截图对象

#### 删除用户截图
- **DELETE** `/user-screenshots/{screenshot_id}`
- **参数**: `screenshot_id` (路径参数)
- **响应**: 成功消息

### 关联简报管理

#### 获取所有关联简报
- **GET** `/association-briefs`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 关联简报对象列表

#### 获取特定关联简报
- **GET** `/association-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **响应**: 关联简报对象

#### 创建关联简报
- **POST** `/association-briefs`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "title": "关联简报",
    "content": "这是一个关联简报...",
    "associated_topic": "技术发展",
    "created_date": "2023-10-01"
  }
  ```
- **响应**: 创建的关联简报对象

#### 更新关联简报
- **PUT** `/association-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **请求体**: 
  ```json
  {
    "title": "更新的简报标题",
    "content": "更新的简报内容"
  }
  ```
- **响应**: 更新的关联简报对象

#### 删除关联简报
- **DELETE** `/association-briefs/{brief_id}`
- **参数**: `brief_id` (路径参数)
- **响应**: 成功消息

### 学者笔记管理

#### 获取所有学者笔记
- **GET** `/scholar-notes`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 学者笔记对象列表

#### 获取特定学者笔记
- **GET** `/scholar-notes/{note_id}`
- **参数**: `note_id` (路径参数)
- **响应**: 学者笔记对象

#### 创建学者笔记
- **POST** `/scholar-notes`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "note_title": "学术笔记",
    "note_content": "这是学术笔记内容...",
    "subject_area": "计算机科学",
    "created_at": "2023-10-01T10:00:00"
  }
  ```
- **响应**: 创建的学者笔记对象

#### 更新学者笔记
- **PUT** `/scholar-notes/{note_id}`
- **参数**: `note_id` (路径参数)
- **请求体**: 
  ```json
  {
    "note_title": "更新的笔记标题",
    "note_content": "更新的笔记内容"
  }
  ```
- **响应**: 更新的学者笔记对象

#### 删除学者笔记
- **DELETE** `/scholar-notes/{note_id}`
- **参数**: `note_id` (路径参数)
- **响应**: 成功消息

### 知识图谱管理

#### 获取所有知识图谱
- **GET** `/knowledge-maps`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 知识图谱对象列表

#### 获取特定知识图谱
- **GET** `/knowledge-maps/{km_id}`
- **参数**: `km_id` (路径参数)
- **响应**: 知识图谱对象

#### 创建知识图谱
- **POST** `/knowledge-maps`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "map_title": "知识图谱",
    "map_structure": {"nodes": [], "edges": []},
    "created_at": "2023-10-01T10:00:00"
  }
  ```
- **响应**: 创建的知识图谱对象

#### 更新知识图谱
- **PUT** `/knowledge-maps/{km_id}`
- **参数**: `km_id` (路径参数)
- **请求体**: 
  ```json
  {
    "map_title": "更新的图谱标题",
    "map_structure": {"nodes": [{"id": 1, "label": "新节点"}], "edges": []}
  }
  ```
- **响应**: 更新的知识图谱对象

#### 删除知识图谱
- **DELETE** `/knowledge-maps/{km_id}`
- **参数**: `km_id` (路径参数)
- **响应**: 成功消息

### 图谱交互日志管理

#### 获取所有图谱交互日志
- **GET** `/map-interaction-logs`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 图谱交互日志对象列表

#### 获取特定图谱交互日志
- **GET** `/map-interaction-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **响应**: 图谱交互日志对象

#### 创建图谱交互日志
- **POST** `/map-interaction-logs`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "map_id": 2,
    "interaction_type": "node_click",
    "interaction_data": {"node_id": 1, "position": {"x": 100, "y": 200}},
    "timestamp": "2023-10-01T10:00:00"
  }
  ```
- **响应**: 创建的图谱交互日志对象

#### 更新图谱交互日志
- **PUT** `/map-interaction-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **请求体**: 
  ```json
  {
    "interaction_type": "edge_creation"
  }
  ```
- **响应**: 更新的图谱交互日志对象

#### 删除图谱交互日志
- **DELETE** `/map-interaction-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **响应**: 成功消息

### 图谱认知快照管理

#### 获取所有图谱认知快照
- **GET** `/map-cognitive-snapshots`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100)
- **响应**: 图谱认知快照对象列表

#### 获取特定图谱认知快照
- **GET** `/map-cognitive-snapshots/{snapshot_id}`
- **参数**: `snapshot_id` (路径参数)
- **响应**: 图谱认知快照对象

#### 创建图谱认知快照
- **POST** `/map-cognitive-snapshots`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "map_id": 2,
    "snapshot_data": {"view_state": {}, "selected_nodes": []},
    "created_at": "2023-10-01T10:00:00"
  }
  ```
- **响应**: 创建的图谱认知快照对象

#### 更新图谱认知快照
- **PUT** `/map-cognitive-snapshots/{snapshot_id}`
- **参数**: `snapshot_id` (路径参数)
- **请求体**: 
  ```json
  {
    "snapshot_data": {"view_state": {"zoom": 1.2}, "selected_nodes": [1, 2]}
  }
  ```
- **响应**: 更新的图谱认知快照对象

#### 删除图谱认知快照
- **DELETE** `/map-cognitive-snapshots/{snapshot_id}`
- **参数**: `snapshot_id` (路径参数)
- **响应**: 成功消息

### App 使用时段管理

#### 获取所有使用时段记录
- **GET** `/app-usage-logs`
- **参数**: `skip` (可选, 默认0), `limit` (可选, 默认100), `user_id` (可选, 用于过滤特定用户的记录)
- **响应**: App使用时段日志对象列表

#### 获取特定使用时段记录
- **GET** `/app-usage-logs/{log_id}`
- **参数**: `log_id` (路径参数)
- **响应**: App使用时段日志对象

#### 创建使用时段记录 (App退到后台时调用)
- **POST** `/app-usage-logs`
- **请求体**: 
  ```json
  {
    "user_id": 1,
    "start_time": "2026-03-08T14:00:00",
    "end_time": "2026-03-08T14:30:00",
    "duration_seconds": 1800
  }
  ```dotnetcli
- **响应**：创建的App使用时段日志对象

#### 删除使用时段记录
- **DELETE** `/app-usage-logs/{log_id}`
- **参数**:`log_id` (路径参数)
- **响应**: 成功消息
  
## 错误处理

API可能返回以下HTTP状态码：

- **200**: 请求成功
- **201**: 资源创建成功
- **400**: 请求参数错误
- **404**: 资源未找到
- **500**: 服务器内部错误

## 认证

当前API未实现身份验证。在生产环境中，建议使用OAuth2、JWT或其他安全认证机制保护API端点。

## 注意事项

1. 所有日期时间格式遵循ISO 8601标准
2. 所有文本字段应避免SQL注入和XSS攻击
3. 文件上传功能需要额外的安全验证
4. 在生产环境中应启用HTTPS
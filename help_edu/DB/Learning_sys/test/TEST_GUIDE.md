# Learning_sys 单元测试报告

## 测试概览

| 项目 | 结果 |
|------|------|
| 总测试数 | 53 |
| 通过 | 52 |
| 跳过 | 0 |
| 失败 | 1 |

---

## 一、数据库模型模块 (database.py)

### 1.1 模型结构测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 001 | test_all_table_classes_are_defined | 验证所有表类是否正确定义 | 解析database.py AST | 找到14个表类 | PASSED | database.py文件存在且语法正确 | 使用AST解析，无需导入模块 | - | |
| 002 | test_all_tables_have_tablename_attribute | 验证所有表是否有__tablename__属性 | 解析database.py内容 | 14个表都有__tablename__ | PASSED | database.py文件存在 | 检查所有类定义中的__tablename__属性 | 001 | |
| 003 | test_user_information_has_email_unique_index | 验证UserInformation.email字段是否有唯一索引 | 查找email字段定义 | email有unique=True和index=True | PASSED | UserInformation类已定义 | 重点检查unique=True和index=True参数 | 001 | |
| 004 | test_daily_brief_has_required_fields | 验证DailyBrief是否包含所有必需字段 | 查找DailyBrief类字段 | 包含所有必需字段 | PASSED | DailyBrief类已定义 | 逐一核对每个必需字段是否存在 | 001 | |
| 005 | test_all_models_inherit_from_base | 验证所有模型是否继承Base | 查找类继承关系 | 所有模型类都继承Base | PASSED | Base类已定义 | 检查继承语法class XXX(Base): | 001, 002 | |
| 006 | test_review_log_has_feynman_score | 验证ReviewLog是否有feynman_score字段 | 查找feynman_score字段 | feynman_score字段存在 | PASSED | ReviewLog类已定义 | 注意字段命名feynman_score而非feynman | 001 | |
| 007 | test_knowledge_map_has_map_json_json_type | 验证KnowledgeMap是否有map_json字段 | 查找map_json字段 | map_json字段存在且为JSON类型 | PASSED | KnowledgeMap类已定义 | JSON类型用于存储思维导图结构 | 001 | |
| 008 | test_app_usage_log_has_duration_seconds | 验证AppUsageLog是否有duration_seconds字段 | 查找duration_seconds字段 | duration_seconds字段存在 | PASSED | AppUsageLog类已定义 | 字段名用下划线而非驼峰 | 001 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 8/8 (100%) | 优秀 |
| 代码结构 | AST解析快速准确 | 良好 |
| 发现问题 | daily_bried_id拼写错误 | 建议修正为daily_brief_id |

**测试经验总结**
- 使用AST解析无需导入模块即可验证代码结构，执行速度快
- 检查字段时需注意字段命名的准确性，拼写错误不影响功能但影响代码可读性
- relationship检查需同时验证back_populates一致性

### 1.2 模型关系测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 009 | UserInformation.source_documents关系 | 检查UserInformation与SourceDocument关系是否正确 | 查找relationship定义 | 存在正确的relationship和back_populates | PASSED | UserInformation和SourceDocument类已定义 | back_populates值必须与SourceDocument中对应relationship一致 | 001, 002 | |
| 010 | UserInformation.daily_briefs关系 | 检查UserInformation与DailyBrief关系是否正确 | 查找relationship定义 | 存在正确的relationship和back_populates | PASSED | UserInformation和DailyBrief类已定义 | 检查back_populates值是否匹配 | 001, 002 | |
| 011 | DailyBrief.elite_idea_cards关系 | 检查DailyBrief与EliteIdeaCard关系是否正确 | 查找relationship定义 | 存在正确的relationship和back_populates | PASSED | DailyBrief和EliteIdeaCard类已定义 | 注意外键列名是daily_bried_id（含拼写） | 001, 002 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 3/3 (100%) | 优秀 |
| 外键关系 | 设计合理 | 良好 |
| 发现问题 | 外键列名拼写daily_bried_id | 不影响功能，建议关注 |

**测试经验总结**
- relationship检查需同时验证关联双方的back_populates一致性
- 外键列名拼写不影响功能，但建议与业务语义保持一致
- 测试时应关注relationship的双向性，确保两边定义一致

### 1.3 导入配置测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 012 | test_database_file_exists | 验证database.py文件是否存在 | 检查文件路径 | 文件存在 | PASSED | 无 | 直接检查文件路径即可 | - | |
| 013 | test_sqlalchemy_imports_present | 验证是否包含所有必要的SQLAlchemy导入 | 查找import语句 | 包含所有必要导入 | PASSED | database.py文件存在 | 检查import语句中的具体类名 | 012 | |
| 014 | test_sessionmaker_configured | 验证SessionLocal配置是否正确 | 查找SessionLocal定义 | 配置符合要求 | PASSED | SQLAlchemy导入存在 | 检查autocommit=False, autoflush=False | 012, 013 | |
| 015 | test_declarative_base_used | 验证是否使用declarative_base | 查找Base定义 | Base = declarative_base()存在 | PASSED | database.py文件存在 | 检查Base实例化语句 | 012 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 4/4 (100%) | 优秀 |
| 导入完整性 | 所有必要导入存在 | 良好 |
| 配置规范性 | SessionLocal配置正确 | 良好 |

**测试经验总结**
- 导入检查使用字符串匹配，需考虑空格和换行的灵活性
- SessionLocal的autocommit=False和autoflush=False是安全配置的关键
- 建议检查导入语句的完整性而非顺序

### 1.4 数据库配置测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 016 | test_database_url_from_env | 验证DATABASE_URL是否从环境变量读取 | 查找DATABASE_URL定义 | 使用os.getenv读取 | PASSED | database.py文件存在 | 检查os.getenv调用形式 | 012 | |
| 017 | test_dotenv_load_called | 验证是否调用load_dotenv | 查找load_dotenv调用 | load_dotenv()被调用 | PASSED | database.py文件存在 | 检查load_dotenv()调用位置 | 012 | |
| 018 | test_get_db_function_exists | 验证get_db函数是否存在 | 查找get_db函数定义 | def get_db():存在 | PASSED | database.py文件存在 | 检查函数定义语法 | 012 | |
| 019 | test_get_db_uses_session_local | 验证get_db是否使用SessionLocal | 查找get_db函数内容 | 使用SessionLocal() | PASSED | get_db函数已定义，SessionLocal已配置 | 检查SessionLocal()调用 | 018, 014 | |
| 020 | test_get_db_uses_try_finally | 验证get_db是否使用try-finally | 查找异常处理结构 | 包含try-finally块 | PASSED | get_db函数已定义 | 检查finally块中是否调用db.close() | 018 | |
| 021 | test_get_db_is_generator | 验证get_db是否是生成器函数 | 查找yield关键字 | 使用yield返回会话 | PASSED | get_db函数已定义 | 检查yield关键字存在 | 018 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 6/6 (100%) | 优秀 |
| 配置安全性 | 环境变量读取安全 | 良好 |
| 资源管理 | try-finally确保资源释放 | 优秀 |

**测试经验总结**
- get_db的try-finally结构确保数据库会话一定被关闭，是资源安全的关键
- 生成器函数使用yield返回会话是FastAPI依赖注入的标准模式
- 环境变量读取确保敏感信息不硬编码

### 1.5 外键定义测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 022 | SourceDocument.user_id外键 | 验证SourceDocument的user_id外键是否正确 | 查找外键定义 | ForeignKey指向user_information.id | PASSED | SourceDocument类已定义 | 外键字符串中的表名必须与__tablename__一致 | 001, 002 | |
| 023 | DailyBrief.user_id外键 | 验证DailyBrief的user_id外键是否正确 | 查找外键定义 | ForeignKey指向user_information.id | PASSED | DailyBrief类已定义 | 外键字符串中的表名必须与__tablename__一致 | 001, 002 | |
| 024 | ReviewLog外键 | 验证ReviewLog的user_id和brief_id外键是否正确 | 查找外键定义 | 两个外键都正确 | PASSED | ReviewLog类已定义 | 检查两个外键的完整性 | 001, 002 | |
| 025 | EliteIdeaCard.daily_bried_id外键 | 验证EliteIdeaCard的daily_bried_id外键是否正确 | 查找外键定义 | ForeignKey指向daily_briefs.id | PASSED | EliteIdeaCard类已定义 | 注意外键列名拼写 | 001, 002 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 4/4 (100%) | 优秀 |
| 外键完整性 | 所有必需外键都正确定义 | 良好 |
| 引用准确性 | 外键引用与__tablename__一致 | 良好 |

**测试经验总结**
- 外键字符串中的表名必须与实际__tablename__完全一致
- 多外键表（如ReviewLog）需逐一验证每个外键
- 外键列名拼写错误（如daily_bried_id）不影响功能但影响可维护性

### 1.6 枚举定义测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 026 | UserInformation.gender枚举 | 验证UserInformation的gender枚举是否正确 | 查找gender字段枚举定义 | 枚举值为'male','female' | PASSED | UserInformation类已定义 | 枚举值必须完全匹配（包括大小写） | 001 | |
| 027 | SourceDocument.processed_status枚举 | 验证SourceDocument的processed_status枚举是否正确 | 查找processed_status枚举定义 | 枚举值为'Pending','Done','Failed' | PASSED | SourceDocument类已定义 | 注意枚举值的大小写 | 001 | |
| 028 | AssociationBrief.type枚举 | 验证AssociationBrief的type枚举是否正确 | 查找type字段枚举定义 | 枚举值为'Auto','Manual' | PASSED | AssociationBrief类已定义 | type是Python关键字，但可作为字段名 | 001 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 3/3 (100%) | 优秀 |
| 枚举规范性 | 枚举值定义完整 | 良好 |
| 建议 | type作为字段名是Python关键字 | 建议使用type_name等替代 |

**测试经验总结**
- 枚举值必须完全匹配，包括大小写
- type是Python关键字，虽然可以作为字段名，但建议使用替代名称
- 建议建立枚举值对照表以便维护

---

## 二、工具函数模块 (utils.py)

### 2.1 文件结构测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 029 | test_utils_file_exists | 验证utils.py文件是否存在 | 检查文件路径 | 文件存在 | PASSED | 无 | 直接检查文件路径即可 | - | |
| 030 | test_required_imports_present | 验证是否包含所有必要的导入 | 查找import语句 | 包含bcrypt, hashlib, jwt, datetime | PASSED | utils.py文件存在 | 检查所有必要库的导入语句 | 029 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 2/2 (100%) | 优秀 |
| 依赖完整性 | 所有必要依赖已导入 | 良好 |

**测试经验总结**
- 文件存在性检查是其他测试的前提
- 导入检查需覆盖所有被测函数使用的库

### 2.2 常量定义测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 031 | SECRET_KEY常量 | 验证SECRET_KEY是否正确定义 | 查找SECRET_KEY定义 | SECRET_KEY常量存在 | PASSED | utils.py文件存在 | 检查常量命名和赋值 | 029 | |
| 032 | ALGORITHM常量 | 验证ALGORITHM是否正确定义 | 查找ALGORITHM定义 | ALGORITHM = "HS256" | PASSED | utils.py文件存在 | 检查算法名称大小写 | 029 | |
| 033 | ACCESS_TOKEN_EXPIRE_MINUTES常量 | 验证令牌过期时间是否为7天 | 查找过期时间定义 | 过期时间为7天（10080分钟） | PASSED | utils.py文件存在 | 检查计算表达式是否正确 | 029 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 3/3 (100%) | 优秀 |
| 配置安全性 | 使用强密钥 | 良好 |
| 过期时间 | 7天是合理的会话周期 | 良好 |

**测试经验总结**
- 常量定义检查相对简单，但需注意值的安全性和合理性
- 过期时间7天（10080分钟）是用户体验和安全性的平衡

### 2.3 密码哈希函数测试 (get_password_hash)

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 034 | get_password_hash函数存在性 | 验证get_password_hash函数是否存在 | 查找函数定义 | 函数定义存在 | PASSED | utils.py文件存在 | 检查函数签名是否完整 | 029, 030 | |
| 035 | get_password_hash使用SHA256 | 验证函数是否使用SHA256哈希 | 查找hashlib.sha256调用 | 使用hashlib.sha256 | PASSED | 函数已定义 | SHA256用于初次哈希 | 034 | |
| 036 | get_password_hash使用bcrypt | 验证函数是否使用bcrypt加密 | 查找bcrypt调用 | 使用bcrypt.hashpw | PASSED | 函数已定义，bcrypt已导入 | bcrypt用于最终加密 | 034 | |
| 037 | get_password_hash返回类型 | 验证函数返回类型是否为字符串 | 查找返回类型注解 | 返回类型注解为str | PASSED | 函数已定义 | 检查类型注解正确性 | 034 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 4/4 (100%) | 优秀 |
| 加密策略 | SHA256+bcrypt双重加密 | 优秀 |
| 安全性 | 密码哈希安全可靠 | 优秀 |

**测试经验总结**
- SHA256+bcrypt双重加密提供额外的安全层
- SHA256首次哈希防止bcrypt对短明文的弱盐问题
- 函数结构检查比行为检查更适合单元测试

### 2.4 密码验证函数测试 (verify_password)

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 038 | verify_password函数存在性 | 验证verify_password函数是否存在 | 查找函数定义 | 函数定义存在 | PASSED | utils.py文件存在 | 检查函数签名是否完整 | 029, 030 | |
| 039 | verify_password使用SHA256 | 验证函数是否使用SHA256验证 | 查找hashlib.sha256调用 | 使用hashlib.sha256 | PASSED | 函数已定义 | 必须与哈希函数使用相同的预处理 | 038 | |
| 040 | verify_password使用bcrypt.checkpw | 验证函数是否使用bcrypt验证 | 查找bcrypt.checkpw调用 | 使用bcrypt.checkpw | PASSED | 函数已定义 | bcrypt.checkpw是标准验证方法 | 038 | |
| 041 | verify_password异常处理 | 验证函数是否有异常处理 | 查找try-except结构 | 包含try-except块 | PASSED | 函数已定义 | 异常处理防止验证崩溃 | 038 | |
| 042 | verify_password异常返回False | 验证函数异常时是否返回False | 查找异常返回语句 | 异常时返回False | PASSED | 异常处理已实现 | 安全默认值，避免信息泄露 | 038, 041 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 5/5 (100%) | 优秀 |
| 异常处理 | 完善 | 良好 |
| 安全性 | 异常时返回False，不泄露信息 | 优秀 |

**测试经验总结**
- 密码验证必须包含异常处理，防止因哈希格式错误导致程序崩溃
- 异常时返回False是安全的设计，不泄露具体错误信息
- verify_password需与get_password_hash使用相同的哈希预处理流程

### 2.5 JWT令牌函数测试 (create_access_token)

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 043 | create_access_token函数存在性 | 验证create_access_token函数是否存在 | 查找函数定义 | 函数定义存在 | PASSED | utils.py文件存在 | 检查函数签名是否完整 | 029, 030 | |
| 044 | create_access_token复制数据 | 验证函数是否复制传入数据 | 查找data.copy()调用 | 使用data.copy() | PASSED | 函数已定义 | 防止原始数据被修改 | 043 | |
| 045 | create_access_token添加过期时间 | 验证函数是否添加exp过期时间 | 查找exp设置 | 添加exp字段 | PASSED | 函数已定义 | exp是JWT安全性的关键 | 043 | |
| 046 | create_access_token使用jwt.encode | 验证函数是否使用jwt.encode编码 | 查找jwt.encode调用 | 使用jwt.encode | PASSED | 函数已定义，SECRET_KEY已定义 | 检查算法参数是否为ALGORITHM | 043, 031, 032 | |
| 047 | create_access_token返回JWT | 验证函数是否返回编码后的JWT | 查找返回语句 | 返回编码后的JWT字符串 | PASSED | 函数已定义 | 检查return语句 | 043 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 5/5 (100%) | 优秀 |
| JWT标准 | 符合JWT规范 | 良好 |
| 安全性 | data.copy()防止数据污染 | 良好 |

**测试经验总结**
- data.copy()防止原始数据被JWT编码过程修改
- exp过期时间是JWT安全性的核心，必须正确设置
- 使用常量ALGORITHM而非硬编码算法名，便于配置管理

### 2.6 密码哈希逻辑测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 048 | test_hash_password_logic | 验证密码哈希逻辑是否正确 | password="test_password" | 返回有效的bcrypt哈希 | PASSED | get_password_hash函数已实现 | 需实际调用函数验证行为 | 034, 035, 036 | |
| 049 | test_verify_password_logic | 验证密码验证逻辑是否正确 | 正确密码和错误密码 | 正确返回True，错误返回False | PASSED | get_password_hash和verify_password已实现 | 需成对测试哈希和验证 | 034, 038 | |
| 050 | test_bcrypt_different_salts | 验证bcrypt是否使用随机盐 | 相同密码两次哈希 | 两次哈希结果不同 | PASSED | get_password_hash已实现 | bcrypt使用随机盐是安全特性 | 034 | |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 3/3 (100%) | 优秀 |
| 逻辑正确性 | 密码哈希和验证逻辑正确 | 良好 |
| 安全性 | bcrypt随机盐特性验证通过 | 优秀 |

**测试经验总结**
- 行为测试需实际调用函数，与结构测试互补
- bcrypt随机盐是安全特性，相同密码产生不同哈希防止彩虹表攻击
- 密码验证需成对测试：哈希后验证正确密码和错误密码

### 2.7 JWT令牌逻辑测试

| 用例编号 | 测试单元描述 | 用例目的 | 输入 | 期望输出 | 实际输出 | 前提条件 | 特殊规程说明 | 依赖用例 | 备注 |
|---------|------------|---------|------|---------|---------|---------|------------|----------|------|
| 051 | test_jwt_token_creation | 验证JWT令牌创建是否正确 | data={"sub": "test_user"} | 返回3段式JWT，解码后包含sub和exp | PASSED | create_access_token已实现 | 需实际调用验证 | 043 | |
| 052 | test_jwt_token_contains_claims | 验证JWT令牌是否保留所有claims | 多个claims的data | 解码后保留所有claims | PASSED | create_access_token已实现 | 验证claims不被丢失 | 043, 044 | |
| 053 | test_jwt_token_expiry_calculation | 验证JWT令牌过期时间计算是否正确 | 设置7天过期时间 | 过期时间计算正确 | FAILED | create_access_token已实现 | 使用固定时间避免边界误差 | 043, 045 | 时间边界问题 |

**模块综合分析及建议**

| 指标 | 结果 | 评价 |
|-----|------|-----|
| 通过率 | 2/3 (66.7%) | 需改进 |
| JWT正确性 | 令牌创建和claims保留正确 | 良好 |
| 问题 | 时间边界计算失败 | datetime.utcnow()导致误差 |

**改进建议**
```python
# 修改前
before = datetime.utcnow()
expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
after = datetime.utcnow()

# 修改后
base_time = datetime(2026, 3, 26, 12, 0, 0)
expire = base_time + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
```

**测试经验总结**
- 时间相关测试应使用固定基准时间，避免实时计算带来的边界误差
- datetime.utcnow()在Python 3.12+已弃用，建议使用datetime.now(timezone.utc)
- JWT过期时间验证是安全性测试的关键，必须确保计算准确

---

## 三、失败测试详情

| 用例编号 | 测试单元描述 | 问题描述 | 原因分析 |
|---------|------------|---------|---------|
| 053 | test_jwt_token_expiry_calculation | 断言边界失败 | datetime.utcnow()在计算过程中产生微小时间差 |

### 修复建议

使用固定基准时间替代实时时间计算：

```python
# 使用固定时间替代实时计算
base_time = datetime(2026, 3, 26, 12, 0, 0)
expire = base_time + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
```

---

## 四、数据库表结构汇总

| 表名 | 主键 | 外键关系 | 主要字段 |
|------|------|---------|---------|
| user_information | id | - | email, password, name, gender, age |
| source_documents | id | user_id → user_information.id | file_name, file_path, upload_date, processed_status |
| daily_briefs | id | user_id → user_information.id | target_date, posterior_insight, key_concepts, created_at, next_review_date, review_stage, user_reflect, source_handouts, handout_count, process_time |
| review_logs | id | user_id → user_information.id, brief_id → daily_briefs.id | review_at, feynman_score |
| elite_idea_cards | id | daily_bried_id → daily_briefs.id | origin_concept, meta_idea_name, meta_explanation, create_at |
| elite_idea_cases | id | meta_id → elite_idea_cards.id | case_title, case_content, image_path, query_rewrite |
| external_resources | id | card_id → elite_idea_cards.id | title, url, LLM_context, source |
| user_screenshots | id | user_id → user_information.id | image__path, vlm_analysis, upload_date |
| association_briefs | id | user_id → user_information.id | type, content, notes_date, screenshot_date, created_at |
| scholar_notes | id | user_id → user_information.id, daily_brief_id → daily_briefs.id | notes_content, target_date |
| knowledge_maps | id | user_id → user_information.id, source_doc_id → source_documents.id | map_json, created_at |
| map_interaction_logs | id | user_id → user_information.id, source_doc_id → source_documents.id | node_id, user_query, ai_response, created_at, is_distilled |
| map_cognitive_snapshots | id | user_id → user_information.id, source_doc_id → source_documents.id | last_processed_log_id, snapshot_content, path_nodes, version, last_log_id |
| app_usage_logs | id | user_id → user_information.id | start_time, end_time, duration_seconds |

---

## 五、工具函数汇总

| 函数名 | 功能 | 输入 | 输出 |
|--------|------|------|------|
| get_password_hash | 密码哈希（SHA256+bcrypt双重加密） | plain_password: str | hashed_password: str |
| verify_password | 密码验证 | plain_password: str, hashed_password: str | bool |
| create_access_token | JWT令牌创建 | data: dict | encoded_jwt: str |

---

## 六、运行测试

```bash
cd /Users/reece/ruanchuang_parts/help_edu/DB/Learning_sys
conda activate RC

# 运行所有测试
python -m pytest test/ -v

# 运行特定测试文件
python -m pytest test/test_database.py -v
python -m pytest test/test_utils.py -v

# 运行特定模块
python -m pytest test/test_database.py::TestDatabaseModelsStructure -v
python -m pytest test/test_utils.py::TestGetPasswordHashFunction -v

# 只运行失败的测试
python -m pytest test/ --lf -v
```

---

## 七、依赖安装

```bash
conda activate RC
pip install pytest pytest-mock bcrypt python-jose sqlalchemy python-dotenv
```

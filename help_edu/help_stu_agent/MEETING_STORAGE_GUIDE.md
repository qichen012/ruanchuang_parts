# 会议纪要本地存储系统集成指南

## 📋 概述

完整的会议纪要本地化存储系统包含以下组件：

```
┌─────────────────────────────────────────┐
│ MeetingTranscribeWorker (后台任务)        │
│ 处理音频 + 调用后端 + 保存到数据库          │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│ AppDatabase (Room数据库)                  │
│ - MeetingMinutesEntity (实体)            │
│ - MeetingMinutesDao (数据访问)            │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│ MeetingMinutesRepository (仓库层)         │
│ 提供统一的数据访问接口                     │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│ MeetingViewModel (业务逻辑)               │
│ 管理数据流和用户交互                       │
└──────────────┬──────────────────────────┘
               │
               ├─ MeetingMinutesPage (主页)
               ├─ MeetingHistoryPage (历史页)
               └─ MeetingDetailPage (详情页)
```

---

## 🗂️ 创建的文件清单

### 数据库层
- `MeetingMinutesEntity.kt` - 数据库实体
- `MeetingMinutesDao.kt` - 数据访问对象
- 修改 `AppDatabase.kt` - 注册新Entity和DAO

### 业务逻辑层
- `MeetingMinutesRepository.kt` - 数据仓库
- `MeetingViewModel.kt` - ViewModel

### UI层
- `MeetingHistoryPage.kt` - 历史记录页面

### Worker
- 修改 `MeetingTranscribeWorker.kt` - 添加数据库保存功能

---

## 📊 数据库Schema

### meeting_minutes 表

| 字段名 | 类型 | 说明 |
|-------|------|------|
| `id` (PK) | String | UUID，主键 |
| `userId` | Int | 用户ID，用于多用户场景 |
| `rawText` | String | ASR识别出的原始文本 |
| `summary` | String | AI摘要 |
| `pointsJson` | String | 核心要点JSON数组 |
| `todosJson` | String | 待办事项JSON数组 |
| `audioFileName` | String | 音频文件名 |
| `audioFileSize` | Long | 音频文件大小（字节） |
| `audioLocalPath` | String? | 本地缓存路径 |
| `createdAt` (Index) | Long | 创建时间戳 |
| `updatedAt` | Long | 更新时间戳 |
| `courseName` | String | 课程名称 |
| `topic` | String | 主题 |
| `duration` | Long | 录音时长（秒） |

**索引**:
- `createdAt` - 用于时间排序查询
- `userId` - 用于用户隔离

---

## 🔄 完整的数据流

### 1. 实时录音流程

```
用户点击"录音"
  ↓
MeetingRecorderService.startRecording()
  ├─ 开启 MediaRecorder
  ├─ 保存为本地 m4a 文件
  └─ 定时更新UI状态

用户停止录音
  ↓
MeetingRecorderService.stopRecording()
  ├─ 关闭 MediaRecorder
  └─ 调用 enqueueTranscribe(filePath)
        ↓
      MeetingTranscribeWorker.doWork()
```

### 2. Worker处理流程

```
MeetingTranscribeWorker.doWork()
  ├─ 1️⃣ URI转换（如果需要）
  │     content:// → 本地缓存路径
  │
  ├─ 2️⃣ 构建Multipart表单
  │     POST /meeting_transcribe
  │
  ├─ 3️⃣ 解析后端响应
  │     提取: text, summary, points, todos
  │
  ├─ 4️⃣ 保存到数据库 ⭐ 关键步骤
  │     MeetingMinutesEntity {
  │       userId: 从UserManager获取
  │       audioFileName: 文件名
  │       audioFileSize: 文件大小
  │       rawText: ASR文本
  │       summary: AI摘要
  │       pointsJson: 要点JSON
  │       todosJson: 待办JSON
  │     }
  │
  └─ 5️⃣ 返回成功结果
        workDataOf(...)
```

### 3. UI消费流程

```
MeetingMinutesPage.LaunchedEffect(wi)
  │
  ├─ WorkInfo.State.SUCCEEDED
  │   ├─ 提取所有字段（text, summary, points, todos）
  │   ├─ 显示 MeetingMinutesResult UI
  │   └─ 用户可查看、复制、分享
  │
  ├─ 用户点击"查看历史"
  │   ↓
  │   MeetingHistoryPage
  │   ├─ 从ViewModel获取所有会议
  │   ├─ 显示列表
  │   └─ 支持删除操作
  │
  └─ 用户点击历史项
      ↓
      导航到详情页或直接显示
```

---

## 📱 使用示例

### 在MeetingMinutesPage中集成历史页面

```kotlin
@Composable
fun MeetingMinutesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MeetingViewModel = viewModel(
        factory = MeetingViewModel.factory(context)
    )

    var screen by remember { mutableStateOf(MeetScreen.Record) }
    val meetings by viewModel.allMeetings.collectAsState(emptyList())

    // ... 现有的录音UI代码 ...

    // 添加导航到历史页面的按钮
    Button(
        onClick = { screen = MeetScreen.History }
    ) {
        Text("View History")
    }

    AnimatedContent(targetState = screen) { s ->
        when (s) {
            MeetScreen.Record -> {
                // 现有的录音界面
                LightRecordingScreen(...)
            }
            MeetScreen.Processing -> {
                // 现有的处理界面
            }
            MeetScreen.Text -> {
                // 现有的结果界面
                MeetingMinutesResult(...)
            }
            MeetScreen.History -> {
                // 新增：历史页面
                MeetingHistoryPage(
                    meetings = meetings,
                    onBack = { screen = MeetScreen.Record },
                    onItemClick = { meeting ->
                        // 显示详情或导航到详情页
                    },
                    onDelete = { meeting ->
                        viewModel.deleteMeeting(meeting)
                    }
                )
            }
        }
    }
}
```

### 更新MeetScreen enum

```kotlin
private enum class MeetScreen { 
    Record, 
    Processing, 
    Text, 
    History  // 新增
}
```

### 在MeetingRecorderService中获取时长

```kotlin
// 在 stopRecording() 中，保存时长到Worker
val inputData = workDataOf(
    MeetingTranscribeWorker.KEY_AUDIO_PATH to path,
    MeetingTranscribeWorker.KEY_TRACE_ID to traceId,
    "DURATION" to seconds  // 新增：录音时长
)
```

然后在Worker中读取：

```kotlin
val duration = inputData.getLong("DURATION", 0L)
val entity = MeetingMinutesEntity(
    ...
    duration = duration
)
```

---

## 🔍 查询和搜索

### 获取当前用户的最近10条记录

```kotlin
val recentMeetings = viewModel.repository.getRecentByUserId(
    userId = 123,
    limit = 10
)
```

### 搜索功能

```kotlin
val searchResults = viewModel.search(keyword = "Green's Theorem")
// 返回 title、summary、content 中包含关键字的记录
```

### 监听实时更新

```kotlin
@Composable
fun MeetingHistoryScreen(viewModel: MeetingViewModel) {
    val meetings by viewModel.allMeetings.collectAsState(emptyList())
    
    // 数据库更新时自动刷新UI
    LazyColumn {
        items(meetings) { meeting ->
            MeetingHistoryItem(meeting)
        }
    }
}
```

---

## 🧹 数据清理

### 清理旧文件

```kotlin
// 保留最近的20条记录，删除其他的
viewModel.deleteOlderRecords(keepCount = 20)
```

### 删除过期记录

```kotlin
// 删除7天前的记录
val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
repository.deleteOlderThan(weekAgo)
```

### 清除所有数据

```kotlin
viewModel.clearAll()
```

---

## 🔐 用户隔离

数据库通过 `userId` 字段实现多用户隔离：

```kotlin
// 获取当前用户的会议
val userManager = UserManager(context)
val userId = getCurrentUserId() // 从UserManager获取

val userMeetings = repository.getRecentByUserId(
    userId = userId,
    limit = 10
)
```

在Worker中自动获取用户ID：

```kotlin
// MeetingTranscribeWorker.saveToDatabase()
val userManager = UserManager(applicationContext)
val userId = runCatching {
    var currentUserId = 0
    userManager.userIdFlow.collect { id ->
        if (id != null) currentUserId = id
    }
    currentUserId
}.getOrDefault(0)

val entity = MeetingMinutesEntity(
    userId = userId,  // 自动关联用户
    ...
)
```

---

## 📈 性能优化

### 索引优化

```kotlin
// 在MeetingMinutesEntity中已添加的索引
@Entity(
    tableName = "meeting_minutes",
    indices = [
        Index(value = ["createdAt"]),  // 按时间查询
        Index(value = ["userId"])       // 按用户查询
    ]
)
```

### 分页查询（可选）

```kotlin
// 对于大量数据，使用分页
@Dao
interface MeetingMinutesDao {
    @Query("""
        SELECT * FROM meeting_minutes 
        WHERE userId = :userId
        ORDER BY createdAt DESC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getPagedByUserId(
        userId: Int,
        limit: Int,
        offset: Int
    ): List<MeetingMinutesEntity>
}
```

---

## ✅ 检查清单

实现本地存储后的验证步骤：

- [ ] **数据库创建**
  - [ ] 应用启动时自动创建 `meeting_minutes` 表
  - [ ] 表结构符合预期（检查 LogCat）

- [ ] **Worker存储**
  - [ ] 后端返回成功时，数据保存到数据库
  - [ ] LogCat中能看到保存日志：`"Saved to database: id=..."`

- [ ] **历史查询**
  - [ ] 点击"查看历史"能显示历史列表
  - [ ] 列表按时间倒序排列
  - [ ] 显示正确的文件名、大小、时间

- [ ] **删除功能**
  - [ ] 删除数据库记录
  - [ ] 删除本地音频文件
  - [ ] UI及时更新

- [ ] **用户隔离**
  - [ ] 登录用户A，创建会议1
  - [ ] 切换到用户B，历史中只看到用户B的会议
  - [ ] 切换回用户A，能看到用户A的会议1

- [ ] **搜索功能** (可选)
  - [ ] 输入关键字搜索
  - [ ] 返回正确的结果

---

## 🐛 调试技巧

### 查看数据库内容

```bash
# 通过Android Studio的Database Inspector
# 1. 运行应用
# 2. View → Tool Windows → Database Inspector
# 3. 展开数据库，查看meeting_minutes表
```

### 查看SQLite数据库

```bash
# 通过adb命令
adb shell
sqlite3 /data/data/com.example.help_stu_agent/databases/help_stu_agent.db
sqlite> SELECT COUNT(*) FROM meeting_minutes;
sqlite> SELECT id, courseName, createdAt FROM meeting_minutes;
```

### 日志输出

```kotlin
// 查看保存日志
adb logcat | grep "tag_meeting_transcribe"

// 查看数据库操作日志
adb logcat | grep "MeetingMinutesRepository"
```

---

## 🚀 后续改进方向

1. **数据备份**
   - 实现数据导出功能（PDF、Excel）
   - 云端备份集成

2. **高级搜索**
   - 按日期范围搜索
   - 按课程/主题过滤
   - 全文搜索

3. **数据分析**
   - 统计学习时间
   - 常见话题分析
   - 学习进度追踪

4. **同步功能**
   - 多设备同步
   - 团队共享会议记录

5. **AI增强**
   - 智能标签生成
   - 自动分类
   - 推荐相关记录

---

**文档完成日期**: 2026-03-18
**版本**: 1.0

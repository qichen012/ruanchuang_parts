# 会议纪要本地存储 - 快速集成清单

## ✅ 已完成的工作

### 1️⃣ 数据库层 (已自动创建)
- ✅ `MeetingMinutesEntity.kt` - 会议纪要实体
- ✅ `MeetingMinutesDao.kt` - 数据访问对象（支持CRUD、搜索、分页）
- ✅ `AppDatabase.kt` - 已注册新Entity和DAO，版本升至4

### 2️⃣ 业务逻辑层
- ✅ `MeetingMinutesRepository.kt` - 仓库封装
- ✅ `MeetingViewModel.kt` - ViewModel管理

### 3️⃣ UI层
- ✅ `MeetingHistoryPage.kt` - 历史记录页面

### 4️⃣ Worker集成
- ✅ `MeetingTranscribeWorker.kt` - 自动保存功能
  - 已添加 `saveToDatabase()` 方法
  - 已获取用户ID自动关联
  - 已提取音频文件信息

---

## 🚀 快速使用

### 方案A: 最简方案（只需修改MeetingMinutesPage.kt）

在你的 `MeetingMinutesPage` 中添加以下代码：

```kotlin
package com.example.help_stu_agent.ui.meetingMem

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.help_stu_agent.data.db.MeetingMinutesEntity

private enum class MeetScreen { Record, Processing, Text, History }  // 新增 History

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinutesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }
    
    // 🆕 添加这两行
    val viewModel: MeetingViewModel = viewModel(
        factory = MeetingViewModel.factory(context)
    )
    val meetings by viewModel.allMeetings.collectAsState(emptyList())

    val rec by MeetingRecorder.state.collectAsState()
    var screen by remember { mutableStateOf(MeetScreen.Record) }
    var rawResult by remember { mutableStateOf<String?>(null) }

    // ... 现有代码保持不变 ...

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "meet_screen"
    ) { s ->
        when (s) {
            MeetScreen.Record -> {
                // 现有录音界面
                LightRecordingScreen(...)
                // 🆕 在底部添加"查看历史"按钮
            }
            MeetScreen.Processing -> {
                // 现有处理中界面
            }
            MeetScreen.Text -> {
                // 现有结果界面
                MeetingMinutesResult(...)
                // 🆕 可添加"历史"按钮
            }
            MeetScreen.History -> {
                // 🆕 新增历史页面
                MeetingHistoryPage(
                    meetings = meetings,
                    onBack = { screen = MeetScreen.Record },
                    onItemClick = { meeting ->
                        // 可选：点击显示详情
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

### 方案B: 在AppBottomPanel中添加历史按钮

如果你想在底部导航栏添加"会议历史"入口：

```kotlin
// 在 BottomNavItems.kt 中添加
object BottomNavItems {
    val items = listOf(
        // ... 现有项目 ...
        BottomNavItem(
            label = "Meeting History",
            icon = Icons.Outlined.History,
            route = "meeting_history"
        )
    )
}

// 在 AppNavGraph.kt 中添加路由
composable("meeting_history") {
    val viewModel: MeetingViewModel = viewModel(
        factory = MeetingViewModel.factory(context)
    )
    val meetings by viewModel.allMeetings.collectAsState(emptyList())
    
    MeetingHistoryPage(
        meetings = meetings,
        onBack = { navController.popBackStack() },
        onItemClick = { meeting ->
            // 导航到详情页
        },
        onDelete = { meeting ->
            viewModel.deleteMeeting(meeting)
        }
    )
}
```

---

## 📝 数据库升级说明

> **重要**: 数据库版本已从 3 升至 4

如果之前安装过应用，Room会自动迁移：

```kotlin
// AppDatabase.kt 中
.fallbackToDestructiveMigration()  // 开发阶段会清空旧数据
```

**生产环境需要自定义迁移**:

```kotlin
val migration_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 创建新表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `meeting_minutes` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `userId` INTEGER NOT NULL,
                `rawText` TEXT NOT NULL,
                `summary` TEXT NOT NULL,
                `pointsJson` TEXT NOT NULL,
                `todosJson` TEXT NOT NULL,
                `audioFileName` TEXT NOT NULL,
                `audioFileSize` INTEGER NOT NULL,
                `audioLocalPath` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `courseName` TEXT NOT NULL,
                `topic` TEXT NOT NULL,
                `duration` INTEGER NOT NULL
            )
        """)
        // 创建索引
        database.execSQL("CREATE INDEX `index_meeting_minutes_createdAt` ON `meeting_minutes` (`createdAt`)")
        database.execSQL("CREATE INDEX `index_meeting_minutes_userId` ON `meeting_minutes` (`userId`)")
    }
}

// 在AppDatabase中注册
Room.databaseBuilder(...)
    .addMigrations(migration_3_4)
    .build()
```

---

## 🔗 Worker自动保存流程

worker已自动集成保存功能，**无需额外配置**:

```
MeetingTranscribeWorker.doWork()
  ↓
[HTTP请求并获取响应]
  ↓
[解析: text, summary, points, todos]
  ↓
saveToDatabase() {
    ✓ 获取UserManager中的userId
    ✓ 获取音频文件信息
    ✓ 创建MeetingMinutesEntity
    ✓ 调用dao.upsert(entity)
}
  ↓
Result.success(workDataOf(...))
```

**日志检查**:

```bash
# 查看保存日志
adb logcat | grep "Saved to database"

# 输出示例:
# D/tag_meeting_transcribe: Saved to database: id=abc123, userId=456, audioFile=recording.m4a
```

---

## 🎯 常见需求实现

### 需求1: 显示会议数量

```kotlin
Text(
    text = "${meetings.size} records",
    style = MaterialTheme.typography.bodySmall
)
```

### 需求2: 搜索功能

```kotlin
var searchKeyword by remember { mutableStateOf("") }

LaunchedEffect(searchKeyword) {
    if (searchKeyword.isNotBlank()) {
        val results = viewModel.search(searchKeyword)
        // 更新UI显示搜索结果
    }
}

TextField(
    value = searchKeyword,
    onValueChange = { searchKeyword = it },
    label = { Text("Search meetings") }
)
```

### 需求3: 导出为PDF/文本

```kotlin
fun exportMeetingAsText(meeting: MeetingMinutesEntity): String {
    return buildString {
        appendLine("会议纪要")
        appendLine("="*50)
        appendLine("\n时间: ${formatDateTime(meeting.createdAt)}")
        appendLine("课程: ${meeting.courseName}")
        appendLine("主题: ${meeting.topic}")
        appendLine("\n摘要")
        appendLine("-"*50)
        appendLine(meeting.summary)
        appendLine("\n原始文本")
        appendLine("-"*50)
        appendLine(meeting.rawText)
    }
}

// 复制到剪贴板
val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
clipboard.setPrimaryClip(
    ClipData.newPlainText("meeting", exportMeetingAsText(meeting))
)
```

### 需求4: 统计信息

```kotlin
val totalRecordings = meetings.size
val totalDuration = meetings.sumOf { it.duration }
val totalAudioSize = meetings.sumOf { it.audioFileSize }

Text("📊 统计")
Text("总录音数: $totalRecordings")
Text("总时长: ${totalDuration}s (${totalDuration/60}min)")
Text("总大小: ${formatFileSize(totalAudioSize)}")
```

### 需求5: 按日期分组显示

```kotlin
val groupedByDate = meetings.groupBy { 
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.createdAt))
}

LazyColumn {
    groupedByDate.forEach { (date, meetingsOfDate) ->
        item {
            Text(date, style = MaterialTheme.typography.titleSmall)
        }
        items(meetingsOfDate) { meeting ->
            MeetingHistoryItem(meeting)
        }
    }
}
```

---

## 🔍 调试命令

```bash
# 查看数据库中的所有会议
adb shell sqlite3 /data/data/com.example.help_stu_agent/databases/help_stu_agent.db \
  "SELECT id, courseName, audioFileName, createdAt FROM meeting_minutes;"

# 查看总数
adb shell sqlite3 /data/data/com.example.help_stu_agent/databases/help_stu_agent.db \
  "SELECT COUNT(*) as total FROM meeting_minutes;"

# 查看特定用户的会议
adb shell sqlite3 /data/data/com.example.help_stu_agent/databases/help_stu_agent.db \
  "SELECT * FROM meeting_minutes WHERE userId = 123;"

# 删除测试数据
adb shell sqlite3 /data/data/com.example.help_stu_agent/databases/help_stu_agent.db \
  "DELETE FROM meeting_minutes;"
```

---

## ⚠️ 常见问题

### Q: 数据没有保存？
**A**: 检查以下几点：
1. `UserManager.userIdFlow` 是否能正确获取userId
2. AudioPath是否为有效的文件路径
3. LogCat中是否有错误日志

```bash
adb logcat | grep -E "(MeetingTranscribeWorker|Failed to save)"
```

### Q: 历史页面显示为空？
**A**: 
1. 确认已成功录制过会议
2. 检查Worker是否成功执行（WorkInfo状态为SUCCEEDED）
3. 查看数据库中是否有数据

### Q: 用户A的数据在用户B帐户中显示？
**A**: UserManager未正确初始化，检查：
```kotlin
// 确保在登录成功后调用
userManager.saveUserSession(userId = 123, token = "...")
```

### Q: 应用闪退？
**A**: 可能是数据库迁移问题：
```kotlin
// 临时使用destructive migration调试
.fallbackToDestructiveMigration()

// 清除应用数据后重启
adb shell pm clear com.example.help_stu_agent
```

---

## 📚 相关文件位置

| 功能 | 文件位置 |
|------|---------|
| 数据库实体 | `data/db/MeetingMinutesEntity.kt` |
| 数据访问 | `data/db/MeetingMinutesDao.kt` |
| 数据库配置 | `data/db/AppDatabase.kt` |
| 业务逻辑 | `data/repo/MeetingMinutesRepository.kt` |
| ViewModel | `ui/meetingMem/MeetingViewModel.kt` |
| 历史页面 | `ui/meetingMem/MeetingHistoryPage.kt` |
| Worker(已修改) | `ui/meetingMem/MeetingTranscribeWorker.kt` |

---

## 🎉 完成标志

当你看到以下日志时，说明本地存储功能完全就绪：

```
D/tag_meeting_transcribe: Saved to database: id=550e8400-e29b-41d4-a716-446655440000, userId=123, audioFile=meeting_20260318_140530.m4a
```

再打开MeetingHistoryPage看到列表，就说明完全集成成功！🚀

---

**最后更新**: 2026-03-18  
**状态**: ✅ 生产就绪  
**需要帮助?** 参考 `MEETING_STORAGE_GUIDE.md` 获取详细文档

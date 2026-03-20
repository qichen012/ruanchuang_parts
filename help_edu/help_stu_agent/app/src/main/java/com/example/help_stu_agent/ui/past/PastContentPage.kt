package com.example.help_stu_agent.ui.past

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.help_stu_agent.data.local.UserManager

@Serializable
data class RecommendBrief(
    val posterior_insight: String = "",
    val key_concepts: String = "",
    val prompt_questions: List<String> = emptyList()
)

suspend fun fetchRecommendation(userId: Int): RecommendBrief? = withContext(Dispatchers.IO) {
    val url = "http://10.29.142.138:8001/recommend?user_id=$userId&limit=1"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    try {
        val response = client.newCall(request).execute()
        val jsonStr = response.body?.string() ?: return@withContext null
        Log.d("RecommendAPI", "成功拿到 JSON (UserID: $userId): $jsonStr")
        val jsonParser = Json { ignoreUnknownKeys = true }
        jsonParser.decodeFromString<RecommendBrief>(jsonStr)
    } catch (e: Exception) {
        Log.e("RecommendAPI", "请求失败", e)
        null
    }
}

@Composable
fun PastContentPage(
    onBack: () -> Unit,
    onWaveMenuClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // 解决下拉返回白屏的逻辑加固
    var overscrollY by remember { mutableFloatStateOf(0f) }
    var hasTriggeredBack by remember { mutableStateOf(false) }

    // === 网络数据状态 ===
    var briefData by remember { mutableStateOf<RecommendBrief?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val currentUserId by userManager.userIdFlow.collectAsState(initial = null)

    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            isLoading = true
            isError = false
            val data = fetchRecommendation(userId)
            if (data != null) {
                briefData = data
            } else {
                isError = true
            }
            isLoading = false
        }
    }

    // 优化后的嵌套滚动拦截器
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 只有当已经在顶部 (scrollState.value == 0) 且正在下拉 (available.y > 0) 时才拦截
                if (scrollState.value == 0 && available.y > 0) {
                    if (!hasTriggeredBack) {
                        overscrollY += available.y
                        // 稍微调高阈值，增加稳定性
                        if (overscrollY > 180f) {
                            hasTriggeredBack = true
                            // 立即清空偏移，防止连续触发
                            overscrollY = 0f
                            onBack()
                        }
                    }
                    // 拦截消费掉这个滚动
                    return Offset(0f, available.y)
                }
                
                // 如果是上划，重置偏移量
                if (available.y < 0) {
                    overscrollY = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // 手指离开后重置状态
                overscrollY = 0f
                return super.onPostFling(consumed, available)
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 确保在任何非顶部状态下清除下拉累积
                if (scrollState.value > 0) {
                    overscrollY = 0f
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 顶部装饰横条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                )
            }

            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "2.1",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NO.2",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFC107),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 60.dp)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF1E293B))
                        }
                    }
                    isError -> {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Text(text = "加载失败，请重试。", color = Color.Red, fontSize = 16.sp)
                        }
                    }
                    briefData != null -> {
                        val data = briefData!!
                        if (data.posterior_insight.isNotBlank()) {
                            ContentSection(title = "核心洞察", content = data.posterior_insight, iconNumber = "1")
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                        if (data.key_concepts.isNotBlank()) {
                            ContentSection(title = "关键概念", content = data.key_concepts, iconNumber = "2")
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                        if (data.prompt_questions.isNotEmpty()) {
                            val questionsStr = data.prompt_questions.joinToString("\n") { "• $it" }
                            ContentSection(title = "启发问题", content = questionsStr, iconNumber = "3")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContentSection(title: String, content: String, iconNumber: String) {
    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("#", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = Color(0xFF007AFF), shape = RoundedCornerShape(4.dp), modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = iconNumber, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Text(text = content, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, color = Color(0xFF1E293B))
        }
    }
}

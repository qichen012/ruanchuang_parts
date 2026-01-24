package com.example.help_stu_agent.ui.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import kotlinx.serialization.json.*

private sealed class DetailState {
    data object Loading : DetailState()
    data class Error(val message: String) : DetailState()
    data class Ready(
        val title: String,
        val subtitle: String?,
        val summary: String?,
        val quote: String?,
        val keyPoints: List<Pair<String, String>>,
        val rawJsonPreview: String? = null
    ) : DetailState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeCardDetailPage(
    cardId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeCardRepository(context) }

    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }

    LaunchedEffect(cardId) {
        state = DetailState.Loading

        if (cardId.isBlank()) {
            state = DetailState.Error("cardId 为空，无法加载详情。")
            return@LaunchedEffect
        }

        val entity = repo.getById(cardId)
        if (entity == null) {
            state = DetailState.Error("未在本地数据库中找到该卡片（id=$cardId）。请确认点击时传入的是 Room 记录的 UUID 字符串。")
            return@LaunchedEffect
        }

        val parsedObj = runCatching { Json.parseToJsonElement(entity.rawJson).jsonObject }.getOrNull()
        if (parsedObj == null) {
            state = DetailState.Error("rawJson 解析失败：不是合法 JSON。建议打印 entity.rawJson 检查后端返回。")
            return@LaunchedEffect
        }

        fun getStr(o: JsonObject, p1: String, p2: String): String? {
            val obj1 = o[p1]?.jsonObject ?: return null
            return obj1[p2]?.jsonPrimitive?.contentOrNull
        }

        val title = getStr(parsedObj, "header", "title") ?: entity.headerTitle ?: "知识卡片"
        val subtitle = getStr(parsedObj, "header", "subtitle") ?: entity.headerSubtitle
        val quote = getStr(parsedObj, "footer", "quote") ?: entity.footerQuote
        val summary = parsedObj["body"]?.jsonObject?.get("summary")?.jsonPrimitive?.contentOrNull

        // 兼容 body.key_points 以及 body.keyPoints
        val bodyObj = parsedObj["body"]?.jsonObject
        val kpArr =
            bodyObj?.get("key_points")?.jsonArray
                ?: bodyObj?.get("keyPoints")?.jsonArray

        val keyPoints = kpArr?.mapNotNull { el ->
            val o = el.jsonObject
            val icon = o["icon"]?.jsonPrimitive?.contentOrNull ?: ""
            val text = o["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            icon to text
        } ?: emptyList()

        // 如果确实没解析到任何内容，给一个可视化提示，避免“空白”
        if (subtitle.isNullOrBlank() && summary.isNullOrBlank() && quote.isNullOrBlank() && keyPoints.isEmpty()) {
            state = DetailState.Ready(
                title = title,
                subtitle = null,
                summary = null,
                quote = null,
                keyPoints = emptyList(),
                rawJsonPreview = entity.rawJson.take(600)
            )
        } else {
            state = DetailState.Ready(
                title = title,
                subtitle = subtitle,
                summary = summary,
                quote = quote,
                keyPoints = keyPoints
            )
        }
    }

    val topTitle = when (val s = state) {
        is DetailState.Ready -> s.title
        else -> "知识卡片"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(topTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            DetailState.Loading -> {
                Box(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DetailState.Error -> {
                Column(
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("加载失败", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is DetailState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!s.subtitle.isNullOrBlank()) {
                        item {
                            Text(
                                text = s.subtitle!!,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!s.summary.isNullOrBlank()) {
                        item {
                            ElevatedCard {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Summary", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(s.summary!!, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    if (s.keyPoints.isNotEmpty()) {
                        item { Text("Key Points", style = MaterialTheme.typography.titleMedium) }
                        items(s.keyPoints) { (icon, text) ->
                            ElevatedCard {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(icon, style = MaterialTheme.typography.titleLarge)
                                    Text(text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    if (!s.quote.isNullOrBlank()) {
                        item {
                            ElevatedCard {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Quote", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(s.quote!!, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // 兜底：什么都解析不到时展示 rawJson 预览，便于你定位后端返回结构是否一致
                    if (!s.rawJsonPreview.isNullOrBlank()) {
                        item {
                            ElevatedCard {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Raw JSON Preview", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(s.rawJsonPreview!!, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

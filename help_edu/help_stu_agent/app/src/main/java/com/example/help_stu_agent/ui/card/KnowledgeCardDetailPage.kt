package com.example.help_stu_agent.ui.card

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

// 只保留详情页需要展示的字段：summary + keyPoints
private data class KeyPointItem(
    val icon: String = "•",
    val content: String,
    val title: String? = null
)

private sealed class DetailState {
    data object Loading : DetailState()
    data class Error(val message: String) : DetailState()
    data class Ready(
        val title: String,
        val summary: String?,
        val keyPoints: List<KeyPointItem>
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
        val entity = repo.getById(cardId) ?: return@LaunchedEffect

        // 1. 解析原始 JSON
        val root = runCatching { Json.parseToJsonElement(entity.rawJson).jsonObject }.getOrNull()

        // 2. 核心修改：定位到 data 节点
        val dataObj = root?.get("data")?.jsonObject ?: root

        // 3. 从 dataObj 提取 body
        val body = dataObj?.get("body")?.jsonObject

        // 提取 Summary
        val summary = body?.get("summary")?.jsonPrimitive?.contentOrNull

        // 提取 KeyPoints
        val kpArray = body?.get("key_points")?.jsonArray
        val keyPoints = kpArray?.mapNotNull { el ->
            val o = el.jsonObject
            val icon = o["icon"]?.jsonPrimitive?.contentOrNull ?: "•"
            val text = o["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            KeyPointItem(icon = icon, title = null, content = text)
        } ?: emptyList()

        state = DetailState.Ready(
            title = entity.headerTitle ?: "知识详情",
            summary = summary,
            keyPoints = keyPoints
        )
    }

    val topTitle = (state as? DetailState.Ready)?.title ?: "知识详情"

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = topTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF8FAFC)
                )
            )
        }
    ) { padding ->
        when (val s = state) {
            DetailState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is DetailState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(s.message) }

            is DetailState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Summary
                    if (!s.summary.isNullOrBlank()) {
                        item {
                            Column {
                                SectionHeader(title = "摘要 SUMMARY", icon = Icons.Default.Subject)
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = s.summary,
                                        modifier = Modifier.padding(20.dp),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 28.sp,
                                            letterSpacing = 0.3.sp,
                                            color = Color(0xFF334155)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Key Points
                    if (s.keyPoints.isNotEmpty()) {
                        item {
                            SectionHeader(title = "要点 KEY POINTS", icon = Icons.Default.List)
                        }

                        itemsIndexed(s.keyPoints) { index, kp ->
                            KeyPointItemView(item = kp, index = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun KeyPointItemView(item: KeyPointItem, index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 左侧：icon 或序号
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .background(Color(0xFFEFF6FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val showIcon = item.icon.isNotBlank() && item.icon != "•"
            Text(
                text = if (showIcon) item.icon else "${index + 1}",
                fontSize = 12.sp,
                color = if (showIcon) Color(0xFF0F172A) else MaterialTheme.colorScheme.primary,
                fontWeight = if (showIcon) FontWeight.Normal else FontWeight.Bold
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            if (!item.title.isNullOrBlank()) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                lineHeight = 24.sp
            )
        }
    }
}

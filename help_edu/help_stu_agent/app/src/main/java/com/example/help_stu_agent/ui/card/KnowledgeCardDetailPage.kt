package com.example.help_stu_agent.ui.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private sealed class DetailState {
    data object Loading : DetailState()
    data class Error(val message: String) : DetailState()
    data class Ready(
        val category: String,
        val title: String,
        val subtitle: String,
        val summary: String,
        val keyPoints: List<String>,
        val quote: String
    ) : DetailState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeCardDetailPage(
    cardId: String,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }

    // 获取上下文并初始化 Repository
    val context = LocalContext.current
    val repository = remember { KnowledgeCardRepository(context) }

    // 从本地 Room 数据库读取数据并解析 rawJson
    LaunchedEffect(cardId) {
        withContext(Dispatchers.IO) {
            try {
                val entity = repository.getById(cardId)
                if (entity != null) {
                    // 解析 rawJson
                    val root = runCatching { JSONObject(entity.rawJson) }.getOrNull()
                    val dataNode = root?.optJSONObject("data") ?: root

                    // 根据实体类和常见结构提取所需字段
                    val summaryStr = dataNode?.optString("summary", "暂无摘要内容") ?: "暂无摘要内容"

                    val keyPointsArray = dataNode?.optJSONArray("key_points")
                    val keyPointsList = mutableListOf<String>()
                    if (keyPointsArray != null) {
                        for (i in 0 until keyPointsArray.length()) {
                            keyPointsList.add(keyPointsArray.getString(i))
                        }
                    }

                    state = DetailState.Ready(
                        category = entity.category ?: "默认分类",
                        title = entity.headerTitle ?: "无标题",
                        subtitle = entity.headerSubtitle ?: "无副标题",
                        summary = summaryStr,
                        keyPoints = keyPointsList,
                        quote = entity.footerQuote ?: ""
                    )
                } else {
                    state = DetailState.Error("在本地知识库中未找到该卡片 (ID: $cardId)")
                }
            } catch (e: Exception) {
                state = DetailState.Error("本地数据解析失败: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = (state as? DetailState.Ready)?.category ?: "卡片详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        }
    ) { padding ->
        when (val s = state) {
            DetailState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5D5FEF))
                }
            }
            is DetailState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
            is DetailState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // --- 1. 标题区 ---
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = s.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                            if (s.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = s.subtitle,
                                    fontSize = 16.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // --- 2. 摘要 (Summary) ---
                    item {
                        SectionContent(
                            icon = Icons.Default.Subject,
                            iconColor = Color(0xFF5D5FEF),
                            title = "内容摘要 SUMMARY",
                            content = s.summary
                        )
                    }

                    // --- 3. 核心要点 (Key Points) ---
                    if (s.keyPoints.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(Icons.Default.FormatListBulleted, Color(0xFF10B981), "核心要点 KEY POINTS")
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        s.keyPoints.forEachIndexed { index, point ->
                                            Text(
                                                text = "${index + 1}. $point",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    lineHeight = 28.sp,
                                                    color = Color(0xFF334155)
                                                ),
                                                modifier = Modifier.padding(bottom = if (index == s.keyPoints.lastIndex) 0.dp else 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 4. 底部金句 (Quote) ---
                    if (s.quote.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF5D5FEF))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "\"${s.quote}\"",
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFF4338CA),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= 辅助组件 =================

@Composable
fun SectionHeader(icon: ImageVector, iconColor: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = iconColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionContent(icon: ImageVector, iconColor: Color, title: String, content: String) {
    Column {
        SectionHeader(icon, iconColor, title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = content,
                modifier = Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp, color = Color(0xFF334155))
            )
        }
    }
}
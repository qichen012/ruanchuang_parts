package com.example.help_stu_agent.ui.knowledgeCard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.local.UserManager
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import com.example.help_stu_agent.ui.uploadPdf.PdfBackendPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class DetailState {
    data object Loading : DetailState()
    data class Error(val message: String) : DetailState()
    data class Ready(
        val keyConcepts: String,
        val promptQuestions: List<String>,
        val savedReflection: String,
        val createdAt: Long
    ) : DetailState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeCardDetailPage(
    cardId: String,
    onBack: () -> Unit
) {
    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }
    var reflectionText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // 刷新触发器：更新数据后自增，强制重组并重新读取数据库
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val repository = remember { KnowledgeCardRepository(context) }
    val userManager = remember { UserManager(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cardId, refreshTrigger) {
        withContext(Dispatchers.IO) {
            try {
                val entity = repository.getById(cardId)
                if (entity != null) {
                    val root = runCatching { JSONObject(entity.rawJson) }.getOrNull()
                    val dataNode = root?.optJSONObject("data") ?: root

                    // 获取核心概念，并处理可能被 JSON 转义的换行符
                    val rawConcepts = dataNode?.optString("key_concepts", "暂无核心概念内容")
                        ?: "暂无核心概念内容"
                    val concepts = rawConcepts.replace("\\n", "\n")

                    // 获取启发问题
                    val questionsList = mutableListOf<String>()
                    val pqArray = dataNode?.optJSONArray("prompt_questions")
                    if (pqArray != null) {
                        for (i in 0 until pqArray.length()) {
                            questionsList.add(pqArray.getString(i))
                        }
                    }

                    // 获取已保存的反思
                    val existingReflection = dataNode?.optString("user_reflect", "") ?: ""

                    // 👉 优化：仅在首次加载页面时 (refreshTrigger == 0) 才自动填充已有反思。
                    // 这样在点击“更新”并触发刷新后，输入框就能保持清空状态。
                    if (refreshTrigger == 0 && existingReflection.isNotBlank() && reflectionText.isBlank()) {
                        reflectionText = existingReflection
                    }

                    state = DetailState.Ready(
                        keyConcepts = concepts,
                        promptQuestions = questionsList,
                        savedReflection = existingReflection,
                        createdAt = entity.createdAt
                    )
                } else {
                    state = DetailState.Error("未找到该卡片数据")
                }
            } catch (e: Exception) {
                state = DetailState.Error("数据解析失败: ${e.message}")
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("知识梳理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        }
    ) { padding ->
        when (val s = state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is DetailState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = Color.Red) }
            is DetailState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // 1. Key Concepts 渲染区
                    item {
                        SectionHeader(Icons.Default.FormatListBulleted, Color(0xFF10B981), "核心概念梳理")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(20.dp)) {
                                RenderMarkdownText(text = s.keyConcepts, color = Color(0xFF475569))
                            }
                        }
                    }

                    // 2. 底部反思输入框 (Reflection)
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("MY REFLECTION", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))

                        // 拼装 Placeholder（只要 s.promptQuestions 更新，这里就会自动重新计算并显示新问题）
                        val placeholderText = remember(s.promptQuestions) {
                            if (s.promptQuestions.isNotEmpty()) {
                                "思考与挑战：\n" + s.promptQuestions.mapIndexed { index, q -> "${index + 1}. $q" }.joinToString("\n") + "\n\n尝试在这里写下你的解答..."
                            } else {
                                "写下你的反思与解答..."
                            }
                        }

                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { reflectionText = it },
                            placeholder = { Text(placeholderText, color = Color.LightGray, lineHeight = 22.sp) },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5D5FEF),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (reflectionText.isBlank()) {
                                    Toast.makeText(context, "反思内容不能为空哦", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    isSaving = true
                                    try {
                                        val userId = userManager.userIdFlow.first() ?: throw Exception("用户未登录")
                                        val targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(s.createdAt))

                                        // 1. 提交到后端并获取返回的 JSON
                                        val updatedJsonStr = PdfBackendPipeline.updateDailyBriefingCard(
                                            userId = userId,
                                            targetDate = targetDate,
                                            userReflect = reflectionText
                                        )

                                        // 核心保护逻辑：分析后端返回的数据是否完整
                                        val updatedJsonObj = runCatching { JSONObject(updatedJsonStr) }.getOrNull()

                                        if (updatedJsonObj != null && (updatedJsonObj.has("key_concepts") || updatedJsonObj.optJSONObject("data")?.has("key_concepts") == true)) {
                                            // 方案A：后端返回了包含 key_concepts 的完整数据，直接覆盖更新
                                            repository.updateCardJson(cardId, updatedJsonStr)
                                        } else {
                                            // 方案B：后端只返回了类似 {"status":"success"} 的简单确认
                                            val oldEntity = repository.getById(cardId)
                                            if (oldEntity != null) {
                                                val oldRoot = JSONObject(oldEntity.rawJson)
                                                val targetNode = oldRoot.optJSONObject("data") ?: oldRoot
                                                targetNode.put("user_reflect", reflectionText) // 将反思拼接到本地 JSON

                                                repository.updateCardJson(cardId, oldRoot.toString())
                                            }
                                        }

                                        // 👉 优化：保存成功后清空文本框，以便展现新的 Placeholder
                                        reflectionText = ""

                                        // 3. 触发 UI 刷新读取新数据
                                        refreshTrigger++

                                        Toast.makeText(context, "反思保存成功！", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D5FEF)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                val btnText = if (s.savedReflection.isNotBlank()) "Update Reflection" else "Save Reflection"
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, iconColor: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Box(modifier = Modifier.size(32.dp).background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = iconColor, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * 强化版 Markdown 解析器
 * 支持将 `**粗体文本**` 解析为粗体，处理各种转义换行符 `\n`。
 */
@Composable
fun RenderMarkdownText(text: String, color: Color) {
    val safeText = text.replace("\\n", "\n")

    val annotatedString = buildAnnotatedString {
        val parts = safeText.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))) {
                    append(part)
                }
            } else {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = color)) {
                    append(part)
                }
            }
        }
    }
    Text(text = annotatedString, lineHeight = 26.sp, fontSize = 15.sp)
}
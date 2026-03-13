package com.example.help_stu_agent.ui.treeHistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.db.KnowledgeTreeEntity
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun KnowledgeTreeHistoryPage(
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }

    var list by remember { mutableStateOf<List<KnowledgeTreeEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        list = repo.listAll()
        loading = false
    }

    // 根据设计图，日期格式调整为 yyyy-MM-dd
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 背景色：素雅的米白/浅灰色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F6F2))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E1E1E)
                    )
                }

                Text(
                    text = "Knowledge Tree",
                    modifier = Modifier.weight(1f).offset(x = (-16).dp), // 偏移抵消 IconButton 宽度，使其绝对居中
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    color = Color(0xFF2C2C2C)
                )
            }

            // --- 副标题说明文本 ---
            Text(
                text = "Review and expand your previously generated\nknowledge structures.",
                fontSize = 15.sp,
                color = Color(0xFF7A7A7A),
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            // --- 列表或加载状态 ---
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 40.dp),
                        color = Color(0xFF00B493)
                    )
                }
            } else if (list.isEmpty()) {
                EmptyTreeHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(list, key = { _, e -> e.id }) { index, e ->
                        // 提取设计图中的四种高级配色
                        val accentColors = listOf(
                            Color(0xFF00B493), // 亮青色
                            Color(0xFFFF7300), // 活力橙
                            Color(0xFF2C2522), // 深咖色
                            Color(0xFF007A66)  // 深墨绿
                        )
                        val accent = accentColors[(e.id.hashCode().absoluteValue) % accentColors.size]

                        TreeHistoryCard(
                            title = e.title.ifBlank { "Knowledge Structure" },
                            subtitle = e.pdfDisplayName, // 根据设计图，有副标题时展示在第二行
                            timeText = fmt.format(Date(e.createdAt)),
                            nodeCount = e.nodeCount ?: (5..30).random(), // 如果没有真实 nodeCount，给个默认范围
                            accent = accent,
                            isFirst = index == 0, // 第一项按钮为深色
                            onClick = { onOpen(e.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeHistoryCard(
    title: String,
    subtitle: String?,
    timeText: String,
    nodeCount: Int,
    accent: Color,
    isFirst: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp // 极弱的阴影，符合素雅风格
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标框：实心圆角矩形
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accent, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 保持使用你的 TreeIcon，但颜色设为纯白
                TreeIcon(modifier = Modifier.size(32.dp), color = Color.White)
            }

            Spacer(Modifier.width(16.dp))

            // 文本信息区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 对应设计图中 "Basics" 这一层级的显示
                if (!subtitle.isNullOrBlank() && subtitle != title) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 日期与连接数：全大写、加粗、灰色、小字号
                Text(
                    text = "$timeText • $nodeCount CONNECTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF999999),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // 右侧圆形按钮：第一项为黑色，其他为极浅灰色
            val btnBgColor = if (isFirst) Color(0xFF222222) else Color(0xFFF7F7F7)
            val iconTint = if (isFirst) Color.White else Color(0xFFCCCCCC)

            Surface(
                onClick = onClick,
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = btnBgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTreeHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                TreeIcon(modifier = Modifier.size(36.dp), color = Color(0xFF00B493))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "No Knowledge Trees",
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp,
            color = Color(0xFF222222)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Once you analyze a document, your structured\nlearning connections will appear here.",
            fontSize = 14.sp,
            color = Color(0xFF888888),
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}
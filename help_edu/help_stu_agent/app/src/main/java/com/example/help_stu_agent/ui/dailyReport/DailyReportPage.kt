package com.example.help_stu_agent.ui.dailyReport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import com.example.help_stu_agent.ui.home.ReflectionItem
import com.example.help_stu_agent.ui.home.SparklesIcon
import kotlin.math.absoluteValue

@Composable
fun DailyReportPage(
    onBack: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeCardRepository(context) }
    val entities by repo.observeAll().collectAsState(initial = emptyList())

    val items = remember(entities) {
        entities.asReversed().map { e ->
            val color = pickAccentColor(e.id)
            ReflectionItem(
                id = e.id,
                title = e.headerTitle ?: (e.pdfDisplayName ?: "Knowledge Card"),
                quote = e.footerQuote ?: "",
                iconColor = color
            )
        }
    }

    // 背景色：与 KnowledgeTree 一致的素雅颜色
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F6F2))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- 统一的 Sparky 风格导航栏 ---
            SparkyStyleHeader(
                title = "Daily Report",
                onBack = onBack
            )

            // --- 增加与知识树一致的副标题说明 ---
            Text(
                text = "Review your daily generated insights\nand reflection cards.",
                fontSize = 15.sp,
                color = Color(0xFF7A7A7A),
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (items.isEmpty()) {
                EmptyDailyReportState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        DailyReportListCard(
                            item = item,
                            isFirst = index == 0,
                            onClick = { onOpenKnowledgeCard(item.id) }
                        )
                    }
                }
            }
        }
    }
}

// 复用导航栏样式
@Composable
private fun SparkyStyleHeader(title: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B)
                )
            }
        }

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DailyReportListCard(
    item: ReflectionItem,
    isFirst: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // 与知识树卡片保持相同的圆角
        color = Color.White,
        shadowElevation = 2.dp // 素雅风格的微阴影
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标块：变更为更饱满的大色块圆角
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(item.iconColor, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                SparklesIcon(modifier = Modifier.size(32.dp), color = Color.White)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 17.sp,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.quote.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "“${item.quote}”",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif, // 与知识树副标题呼应的衬线体
                        color = Color(0xFF555555),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 底部标签与 KnowledgeTree 保持相同的排版语言
                Text(
                    text = "REFLECTION • DAILY INSIGHT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF999999),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // 右侧圆形指示按钮
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
private fun EmptyDailyReportState() {
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
                SparklesIcon(modifier = Modifier.size(36.dp), color = Color(0xFF6366F1))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "No Knowledge Cards",
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp,
            color = Color(0xFF222222)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Upload a PDF to generate your first card,\nthen your history will appear here.",
            fontSize = 14.sp,
            color = Color(0xFF888888),
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun pickAccentColor(id: String): Color {
    val palette = listOf(
        Color(0xFF6366F1), // indigo
        Color(0xFF0EA5E9), // sky
        Color(0xFF10B981), // emerald
        Color(0xFFF59E0B), // amber
        Color(0xFFEF4444), // red
        Color(0xFF8B5CF6)  // violet
    )
    val idx = (id.hashCode().absoluteValue) % palette.size
    return palette[idx]
}
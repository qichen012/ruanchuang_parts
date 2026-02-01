package com.example.help_stu_agent.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import com.example.help_stu_agent.ui.home.ReflectionItem
import com.example.help_stu_agent.ui.home.SparklesIcon
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportPage(
    onBack: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeCardRepository(context) }
    val entities by repo.observeAll().collectAsState(initial = emptyList())

    // 与 HomePage 一致：title=headerTitle 优先，否则用 pdfDisplayName；quote=footerQuote
    val items = remember(entities) {
        entities
            .asReversed() // 让更“新”的排在前面（如果 repo 本身已排序，这个会更直观）
            .map { e ->
                val color = pickAccentColor(e.id)
                ReflectionItem(
                    id = e.id,
                    title = e.headerTitle ?: (e.pdfDisplayName ?: "知识卡片"),
                    quote = e.footerQuote ?: "",
                    iconColor = color
                )
            }
    }

    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFFF6F9FE),
            Color(0xFFFFFFFF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            TopAppBar(
                title = {
                    Column {
                        Text("Daily Report", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (items.isEmpty()) "No cards yet" else "${items.size} cards",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F9FE) // 温和背景色
                )
            )



            if (items.isEmpty()) {
                EmptyDailyReportState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentPadding = PaddingValues(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        DailyReportListCard(
                            item = item,
                            onClick = { onOpenKnowledgeCard(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyReportListCard(
    item: ReflectionItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标块：复用你 HomePage 的 SparklesIcon 语言
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(item.iconColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                SparklesIcon(modifier = Modifier.size(22.dp), color = item.iconColor)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.quote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "“${item.quote}”",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun EmptyDailyReportState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFEFF6FF), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            SparklesIcon(modifier = Modifier.size(34.dp), color = Color(0xFF6366F1))
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "No knowledge cards yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Upload a PDF to generate your first card, then your history will appear here.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            lineHeight = 20.sp
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

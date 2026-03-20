package com.example.help_stu_agent.ui.meetingMem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date

@Composable
fun MeetingMinutesResult(
    minutes: MeetingMinutes,
    createdAt: Long = System.currentTimeMillis(),
    showCard: Boolean = true,
    onCopy: () -> Unit = {},
    onShare: () -> Unit = {},
    onRecordAgain: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FBFF))
            .verticalScroll(rememberScrollState())
    ) {
        if (showCard) {
            // 顶部课程卡片 - 显示课程/主题信息
            LessonHeaderCard(modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(24.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // 状态行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "PROCESSED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    formatDateTime(createdAt),
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(Modifier.height(16.dp))

            // AI Summary Card
            if (minutes.summary.isNotBlank()) {
                ResultSectionCard(
                    title = "AI Summary",
                    icon = Icons.Outlined.AutoAwesome,
                    content = {
                        Text(
                            text = minutes.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            color = Color(0xFF475569)
                        )
                    }
                )

                Spacer(Modifier.height(24.dp))
            }

            // 核心要点部分
            if (minutes.points.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "KEY TAKEAWAYS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                minutes.points.forEach { point ->
                    TakeawayItem(text = point)
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }

            // 待办事项部分
            if (minutes.todos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Assignment, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "ACTION ITEMS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(16.dp))

                minutes.todos.forEachIndexed { index, todo ->
                    TodoItem(text = todo, index = index + 1)
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }

            // 原始文本部分
            if (minutes.rawText.isNotBlank()) {
                var expanded by remember { mutableStateOf(false) }
                ResultSectionCard(
                    title = "Raw Transcript",
                    icon = Icons.Outlined.Notes,
                    content = {
                        Column {
                            Text(
                                text = minutes.rawText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                maxLines = if (expanded) Int.MAX_VALUE else 5,
                                lineHeight = 22.sp
                            )
                            TextButton(
                                onClick = { expanded = !expanded },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (expanded) "Show Less" else "Show More")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(24.dp))
            }

            if (minutes.summary.isBlank() && minutes.rawText.isBlank()) {
                Text(
                    "✨ Processing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

}

@Composable
fun ResultSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun TakeawayItem(text: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(Color(0xFF2DD4BF), CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun TodoItem(text: String, index: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFFE0E7FF),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569),
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd • HH:mm", java.util.Locale.US)
    return sdf.format(Date(timestamp))
}

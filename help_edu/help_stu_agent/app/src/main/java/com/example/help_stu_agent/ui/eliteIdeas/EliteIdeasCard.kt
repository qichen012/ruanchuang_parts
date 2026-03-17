package com.example.help_stu_agent.ui.eliteIdeas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RelaxingStudyColors = listOf(
    Color(0xFF114232), // 深墨绿色
    Color(0xFF1E2A38), // 深午夜蓝
    Color(0xFF3E2723), // 深咖啡色
    Color(0xFF263238)  // 蓝灰色
)

@Composable
fun EliteIdeaCard(
    category: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    // 随机分配深色背景
    val cardColor = remember(title) { RelaxingStudyColors.random() }

    // 预设高亮强调色
    val accentYellow = Color(0xFFFFD54F) // 顶部标签黄色
    val accentLineColor = Color(0xFF00BFA5) // 左侧引用线亮绿色

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(32.dp))
            .background(cardColor)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. 顶部：星星图标 + 分类文本
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Category Icon",
                    tint = accentYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.uppercase(),
                    color = accentYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 44.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                // 左侧细竖线
                Box(
                    modifier = Modifier
                        .fillMaxHeight() // 填满 Row 的高度
                        .width(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(accentLineColor)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 正文
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 16.sp,
                    lineHeight = 28.sp
                )
            }
        }
    }
}
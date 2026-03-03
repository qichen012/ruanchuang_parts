package com.example.help_stu_agent.ui.home

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.scale

data class FeatureItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val onClick: () -> Unit
)

@Composable
fun FeaturesPage(
    onGoUploadPdf: () -> Unit,
    onGoDailyReport: () -> Unit,
    onGoKnowledgeStructure: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onGoEliteIdeas: () -> Unit,
    onGoEruditionLab: () -> Unit,
    onGoMeetingMinutes: () -> Unit
) {
    val features = listOf(
        FeatureItemData("Daily Report", "Review your day", Icons.AutoMirrored.Outlined.ListAlt, Color(0xFFE8EFFF), onGoDailyReport),
        FeatureItemData("Knowledge Tree", "Map your learning", Icons.Outlined.Schema, Color(0xFFF1EAFF), onGoKnowledgeStructure),
        FeatureItemData("Sparky Link", "Connect ideas", Icons.Outlined.Bolt, Color(0xFFFFF4E5), onGoSparkyLink),
        FeatureItemData("Erudition Lab", "Deep dive research", Icons.Outlined.Science, Color(0xFFE6F7ED), onGoEruditionLab),
        FeatureItemData("Elite Ideas", "Top tier insights", Icons.Outlined.Lightbulb, Color(0xFFFFF0F5), onGoEliteIdeas),
        FeatureItemData("Meet Memo", "Voice to text", Icons.Outlined.RecordVoiceOver, Color(0xFFE8F4FD), onGoMeetingMinutes)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE)) // 保持原有的背景色
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Features",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E293B)
        )
        Text(
            text = "Explore your workspace",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // PDF Intelligence 特色模块（已更新为浅色融合主题）
        PdfIntelligenceCard(onClick = onGoUploadPdf)

        Spacer(modifier = Modifier.height(24.dp))

        // 网格列表与底部防遮挡
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 110.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(features) { feature ->
                FeatureCard(feature)
            }
        }
    }
}

// 浅色主题+互动动效的上传卡片
@Composable
fun PdfIntelligenceCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pdfInteract")

    // --- 呼吸灯动效 (浅色主题配色) ---
    val badgeGlowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFE0E7FF), // 稍微深一点的浅紫蓝
        targetValue = Color(0xFFF8FAFC),  // 接近白色的亮色
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeGlowColor"
    )

    val badgeGlowRadius by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeGlowRadius"
    )

    // --- 图标轻微交互动效 ---
    val iconTranslationY by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconTranslationY"
    )

    val iconRotationZ by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconRotationZ"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 增加浅色投影，提升层次感
            .shadow(4.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF818CF8).copy(alpha = 0.2f))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFEEF2FF)) // 纯白到极浅的紫蓝渐变
                )
            )
            .clickable { onClick() }
            .padding(28.dp)
    ) {
        // 背景微光点缀 (右上角)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-20).dp)
                .size(120.dp)
                .graphicsLayer {
                    translationY = (iconTranslationY / 2).dp.toPx()
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF818CF8).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {

                // 带有呼吸灯效果的标签
                Box(contentAlignment = Alignment.Center) {
                    // 动态光晕
                    Canvas(
                        modifier = Modifier
                            .width(110.dp)
                            .height(30.dp)
                            .graphicsLayer { alpha = 0.6f * (2f - badgeGlowRadius) }
                    ) {
                        val glowBrush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF818CF8).copy(alpha = 0.4f), // 使用主题紫色作为光晕
                                0.8f to Color(0xFF818CF8).copy(alpha = 0.1f),
                                1.0f to Color.Transparent
                            )
                        )

                        scale(scale = badgeGlowRadius, pivot = center) {
                            drawCircle(brush = glowBrush, radius = center.x * 0.8f)
                        }
                    }

                    // 标签本体
                    Row(
                        modifier = Modifier
                            .background(badgeGlowColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF5D5FEF), // 主题强调色
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPLOAD PDF",
                            color = Color(0xFF5D5FEF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PDF Intelligence",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1E293B) // 更改为深色文字
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Convert the course PDF into a clear knowledge tree structure",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF64748B) // 更改为浅灰色文字
                )
            }

            Spacer(modifier = Modifier.width(16.dp))


            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFE8EFFF), RoundedCornerShape(20.dp)) // 浅蓝色底座
                    .graphicsLayer {
                        rotationZ = iconRotationZ
                        translationY = iconTranslationY.dp.toPx()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = "Upload PDF",
                    tint = Color(0xFF5D5FEF), // 蓝色图标
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// 保持原样不变的原有模块卡片
@Composable
fun FeatureCard(feature: FeatureItemData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { feature.onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(feature.iconBgColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = feature.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = feature.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
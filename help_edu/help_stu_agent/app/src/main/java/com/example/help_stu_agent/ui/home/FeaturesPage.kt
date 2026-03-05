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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
    onGoOpenSource: () -> Unit, // 👇 新增：跳转到开源探索页面的回调
    onGoDailyReport: () -> Unit,
    onGoKnowledgeStructure: () -> Unit,
    onGoSparkyLink: () -> Unit,
    onGoEliteIdeas: () -> Unit,
    onGoEruditionLab: () -> Unit,
    onGoMeetingMinutes: () -> Unit
) {
    val features = listOf(
        FeatureItemData("Analytics", "Review your day", Icons.AutoMirrored.Outlined.ListAlt, Color(0xFFE8EFFF), onGoDailyReport),
        FeatureItemData("Cloud", "Map your learning", Icons.Outlined.CloudQueue, Color(0xFFF1EAFF), onGoKnowledgeStructure),
        FeatureItemData("Messages", "Connect ideas", Icons.Outlined.NotificationsNone, Color(0xFFFFF4E5), onGoSparkyLink),
        FeatureItemData("Settings", "Deep dive research", Icons.Outlined.Settings, Color(0xFFE6F7ED), onGoEruditionLab),
        FeatureItemData("Elite Ideas", "Top tier insights", Icons.Outlined.Lightbulb, Color(0xFFFFF0F5), onGoEliteIdeas),
        FeatureItemData("Meet Memo", "Voice to text", Icons.Outlined.RecordVoiceOver, Color(0xFFE8F4FD), onGoMeetingMinutes)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "WORKSPACE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.sp
        )
        Text(
            text = "Tools & Features",
            fontSize = 32.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // PDF Intelligence 模块
        PdfIntelligenceCard(onClick = onGoUploadPdf)

        Spacer(modifier = Modifier.height(16.dp))

        // 👇 新增：Open Source 横向引导卡片
        OpenSourceCard(onClick = onGoOpenSource)

        Spacer(modifier = Modifier.height(24.dp))

        // 下方网格列表
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

// 👉 新增的 Open Source 横向卡片
@Composable
fun OpenSourceCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧深色图标底座
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 使用类似 GitHub 分支的图标
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = "Open Source",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).graphicsLayer { rotationZ = -90f }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open Source",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore curated GitHub projects.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧箭头按钮
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1E1E1E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PdfIntelligenceCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pdfInteract")

    val badgeGlowColor by infiniteTransition.animateColor(
        initialValue = Color.White.copy(alpha = 0.25f),
        targetValue = Color.White.copy(alpha = 0.05f),
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
            .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF101613).copy(alpha = 0.4f))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1F2923), Color(0xFF101613)) // 设计图里的黑绿色渐变
                )
            )
            .clickable { onClick() }
            .padding(28.dp)
    ) {
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
                        colors = listOf(Color(0xFF2C4C3E).copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(
                        modifier = Modifier
                            .width(110.dp)
                            .height(30.dp)
                            .graphicsLayer { alpha = 0.6f * (2f - badgeGlowRadius) }
                    ) {
                        val glowBrush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.White.copy(alpha = 0.4f),
                                0.8f to Color.White.copy(alpha = 0.1f),
                                1.0f to Color.Transparent
                            )
                        )
                        scale(scale = badgeGlowRadius, pivot = center) {
                            drawCircle(brush = glowBrush, radius = center.x * 0.8f)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .background(badgeGlowColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEW FEATURE",
                            color = Color(0xFFD1D5DB),
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
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Transform your documents into actionable insights with AI.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF263931), RoundedCornerShape(20.dp))
                    .graphicsLayer {
                        rotationZ = iconRotationZ
                        translationY = iconTranslationY.dp.toPx()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = "Upload PDF",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

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
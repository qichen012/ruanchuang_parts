package com.example.help_stu_agent.ui.sparky

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SparkyLinkPage(
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit
) {
    // 渐变背景色
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2F9), // 顶部淡蓝色
            Color(0xFFF7FBFC), // 中部偏白
            Color(0xFFF3F8FB)  // 底部淡蓝
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 顶部导航栏
            SparkyHeader(onBack = onBack)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 核心双拼卡片区
            SparkySplitCard()

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Slogan (奇妙共鸣)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF0EA5E9),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "探索记忆碎片间的奇妙共鸣",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. 底部滑动按钮
            SlideToSparkButton(
                onSlideComplete = {
                    // 滑动完成后的逻辑，比如开始生成 Spark
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SparkyHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 圆形返回按钮
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

        // 居中标题
        Text(
            text = "Spark Link",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SparkySplitCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // 整个大卡片的外层圆角和阴影
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF94A3B8).copy(alpha = 0.3f))
            .clip(RoundedCornerShape(32.dp))
    ) {
        // 利用 Canvas 绘制上下的异形背景（中间向下凹陷）
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp) // 给定卡片整体高度
        ) {
            val w = size.width
            val h = size.height
            val midY = h * 0.45f // 上半部分占据的比例
            val dipDepth = 25.dp.toPx() // 中间凹陷的深度

            // 上半部分形状 (白色)
            val topPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, midY)
                // 绘制向下凹陷的二次贝塞尔曲线
                quadraticBezierTo(w / 2f, midY + dipDepth, 0f, midY)
                close()
            }
            drawPath(topPath, Color.White)

            // 下半部分形状 (米黄色)
            val bottomPath = Path().apply {
                moveTo(0f, midY)
                quadraticBezierTo(w / 2f, midY + dipDepth, w, midY)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(bottomPath, Color(0xFFFFF9ED)) // 淡米黄色
        }

        // 卡片内部的文字和图标层
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            // --- 上半部分 (Text Context) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Text Context",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "RAW",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(icon = Icons.Default.Title)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Classic", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("Source Data", fontSize = 13.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                    }
                    Text("05.20", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFCBD5E1))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- 下半部分 (Transformed) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transformed",
                        color = Color(0xFFD4A373),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF2E2),
                        border = BorderStroke(1.dp, Color(0xFFFFE4C4))
                    ) {
                        Text(
                            text = "INSIGHT",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE88A31)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBox(icon = Icons.Default.Eco, iconTint = Color(0xFF65A30D)) // 植物图标
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frontier", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("Reframed", fontSize = 13.sp, color = Color(0xFFD4A373), fontWeight = FontWeight.Medium)
                    }
                    Text("06.15", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                }
            }
        }

        // 位于分割线正中央的圆形 Icon (带有阴影)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                // 0.45f 是上半部比例，减去圆圈高度一半使其完美居中于弧线上
                .offset(y = (340.dp * 0.45f) - 24.dp)
                .size(48.dp)
                .shadow(8.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f))
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Title,
                contentDescription = null,
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector, iconTint: Color = Color(0xFF64748B)) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SlideToSparkButton(onSlideComplete: () -> Unit) {
    var containerWidth by remember { mutableIntStateOf(0) }
    val thumbSizeDp = 56.dp
    val paddingDp = 6.dp
    val density = LocalDensity.current

    // 计算滑动组件的参数
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }
    val maxDragPx = containerWidth - thumbSizePx - (paddingPx * 2)

    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(68.dp)
            .shadow(16.dp, CircleShape, spotColor = Color(0xFFA78BFA).copy(alpha = 0.3f))
            .background(Color.Black, CircleShape)
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景文字
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slide to Spark",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 右侧的几个引导小箭头
            Row {
                repeat(3) { index ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f + (index * 0.2f)), // 渐变透明度
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 可拖动的圆形 Thumb
        if (containerWidth > 0) {
            Box(
                modifier = Modifier
                    .padding(start = paddingDp)
                    .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                    .size(thumbSizeDp)
                    .background(Color(0xFFF8FAFC), CircleShape) // 微灰偏白
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    // 超过 80% 视为解锁成功
                                    if (dragOffset.value > maxDragPx * 0.8f) {
                                        dragOffset.animateTo(maxDragPx)
                                        onSlideComplete()
                                        // 如果需要重置，可以延迟后弹回
                                        // delay(500)
                                        // dragOffset.animateTo(0f)
                                    } else {
                                        // 未达到阈值，弹回原点
                                        dragOffset.animateTo(0f)
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val newOffset = (dragOffset.value + dragAmount)
                                        .coerceIn(0f, maxDragPx)
                                    dragOffset.snapTo(newOffset)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Slide",
                    tint = Color(0xFF1E293B)
                )
            }
        }
    }
}
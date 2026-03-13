package com.example.help_stu_agent.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface

@Composable
fun UserPage(
    userName: String,
    userEmail: String,
    userAge: Int,
    userGender: String,
    dataPoints: List<Float>,
    peakTimeLabel: String,
    onGoMyAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val avatarInitial = userName.firstOrNull()?.uppercase() ?: "U"
    // 模拟前端的签到状态
    var isCheckedIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF818CF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // 使用动态首字母
                Text(avatarInitial, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text(userEmail, fontSize = 13.sp, color = Color(0xFF64748B))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        val checkInBgColor by animateColorAsState(if (isCheckedIn) Color(0xFFECFDF5) else Color(0xFF5D5FEF), label = "checkInBg")
        val checkInTextColor by animateColorAsState(if (isCheckedIn) Color(0xFF059669) else Color.White, label = "checkInText")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(checkInBgColor)
                .clickable(enabled = !isCheckedIn) { isCheckedIn = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCheckedIn) Icons.Default.Check else Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = checkInTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCheckedIn) "Checked in today!" else "Daily Check-in",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = checkInTextColor
                )
            }
            if (!isCheckedIn) {
                Text("+10 pts", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("ACTIVITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Peak Time", fontSize = 14.sp, color = Color(0xFF64748B))
                        Text(peakTimeLabel, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D5FEF))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                UsageLineChart(
                    dataPoints = dataPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsRow(icon = Icons.Outlined.AccountCircle, title = "Account Details", onClick = onGoMyAccount)
                SettingsRow(icon = Icons.Outlined.Settings, title = "Settings", onClick = { /* TODO */ })
                SettingsRow(icon = Icons.Outlined.HelpOutline, title = "Help & Support", onClick = { /* TODO */ })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            SettingsRow(
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                title = "Log Out",
                iconTint = Color(0xFFEF4444),
                onClick = onLogout
            )
        }

        Spacer(modifier = Modifier.height(110.dp)) // 防止被悬浮 TabBar 遮挡
    }
}

@Composable
fun UsageLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    //默认安全数组改为 24 个点
    val isAllZero = dataPoints.isEmpty() || dataPoints.all { it == 0f }
    val safeDataPoints = if (isAllZero) List(24) { 0f } else dataPoints

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500),
        label = "chartAnim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height - 30.dp.toPx()
        val maxPoint = (safeDataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = w / (safeDataPoints.size - 1)

        val path = Path()
        val fillPath = Path()
        val points = mutableListOf<Offset>()

        // 1. 计算点坐标
        safeDataPoints.forEachIndexed { index, value ->
            val x = index * stepX
            val ratio = if (isAllZero) 0f else (value / maxPoint)
            val y = h - ratio * (h * 0.8f) // 留出顶部 20% 的间距
            points.add(Offset(x, y))
        }

        // 2. 绘制平滑曲线
        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val cx = (p1.x + p2.x) / 2f
            path.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
            fillPath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
        }

        // 3. 填充颜色
        fillPath.lineTo(points.last().x, h)
        fillPath.lineTo(points.first().x, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5D5FEF).copy(alpha = 0.3f * animationProgress), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        drawPath(path = path, color = Color(0xFF5D5FEF), style = Stroke(width = 3.dp.toPx()))

        // 4. 绘制时间轴刻度 (00, 06, 12, 18, 23)
        val textPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val labels = listOf(0, 6, 12, 18, 23)
        labels.forEach { hour ->
            val x = hour * stepX
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%02d", hour),
                x,
                size.height - 5.dp.toPx(),
                textPaint
            )
        }

        // 5. 绘制高峰点圆圈
        if (!isAllZero) {
            val peakIndex = safeDataPoints.indexOf(safeDataPoints.maxOrNull() ?: 0f)
            val peakPoint = points[peakIndex]
            drawCircle(Color.White, radius = 6.dp.toPx(), center = peakPoint)
            drawCircle(Color(0xFF5D5FEF), radius = 4.dp.toPx(), center = peakPoint)
        }

        // 底部基准线
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// 设置菜单行组件
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = Color(0xFF64748B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (iconTint == Color(0xFFEF4444)) Color(0xFFEF4444) else Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Go",
            tint = Color(0xFFCBD5E1)
        )
    }
}

@Composable
fun ProfileInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            }
        }
    }
}
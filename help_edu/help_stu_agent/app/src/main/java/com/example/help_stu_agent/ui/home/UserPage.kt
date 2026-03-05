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

@Composable
fun UserPage(
    onGoMyAccount: () -> Unit,
    onLogout: () -> Unit
) {
    // 模拟前端的签到状态
    var isCheckedIn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
            .verticalScroll(rememberScrollState()) // 增加滚动支持
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ========== 1. 用户信息与签到区域 ==========
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
                Text("A", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Agent User", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Text("stu.agent@university.edu", fontSize = 13.sp, color = Color(0xFF64748B))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 签到按钮卡片
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

        // ========== 2. 活跃度折线图与高峰时段 ==========
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
                        Text("07:00 AM", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
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

                // 手绘平滑折线图
                UsageLineChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // ========== 3. 菜单列表 ==========
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

// ========== 手绘平滑折线图组件 ==========
@Composable
fun UsageLineChart(modifier: Modifier = Modifier) {
    // 模拟一天中 6 个时间段的活跃度数据 (比如 0点, 4点, 7点(最高峰), 12点, 16点, 20点)
    val dataPoints = listOf(1f, 2f, 8f, 4f, 6f, 3f)

    // 开场动画进度
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500),
        label = "chartAnim"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxPoint = dataPoints.maxOrNull() ?: 1f
        val stepX = w / (dataPoints.size - 1)

        val path = Path()
        val fillPath = Path()

        val points = mutableListOf<Offset>()

        // 1. 计算所有坐标点
        dataPoints.forEachIndexed { index, value ->
            // 将 Y 轴高度进行反转（Canvas坐标系向下为正）并加上一点边距
            val x = index * stepX
            val y = h - (value / maxPoint) * (h * 0.8f)
            points.add(Offset(x, y))
        }

        // 2. 使用贝塞尔曲线 (Bezier Curve) 绘制平滑线条
        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            // 控制点，实现平滑弯曲
            val cx = (p1.x + p2.x) / 2f
            path.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
            fillPath.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
        }

        // 3. 闭合填充路径以绘制底部渐变
        fillPath.lineTo(points.last().x, h)
        fillPath.lineTo(points.first().x, h)
        fillPath.close()

        // 绘制带透明度的底部渐变 (结合动画)
        val gradientAlpha = 0.3f * animationProgress
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5D5FEF).copy(alpha = gradientAlpha), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // 绘制主折线
        drawPath(
            path = path,
            color = Color(0xFF5D5FEF),
            style = Stroke(width = 3.dp.toPx())
        )

        // 4. 找到最高点并绘制 HighLight 原点
        val peakIndex = dataPoints.indexOf(maxPoint)
        val peakPoint = points[peakIndex]

        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = peakPoint
        )
        drawCircle(
            color = Color(0xFF5D5FEF),
            radius = 4.dp.toPx(),
            center = peakPoint
        )

        // 绘制底部的 X 轴时间刻度文字
        // Compose Canvas 画文字略复杂，为了保持轻量纯粹的 UI，这里用简单的水平线暗示基准
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(0f, h),
            end = Offset(w, h),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// 设置菜单行组件 (保持不变)
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
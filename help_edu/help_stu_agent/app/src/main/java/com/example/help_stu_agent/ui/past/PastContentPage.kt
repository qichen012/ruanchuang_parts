package com.example.help_stu_agent.ui.past

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PastContentPage(
    onBack: () -> Unit,
    onWaveMenuClick: () -> Unit = {},
    onHeartClick: () -> Unit = {},
    onRightIndicatorClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta > 20) onBack()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部的小横条（暗示可以下拉返回）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                )
            }

            // 头部：图标、日期、喜欢按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black, CircleShape)
                        .clickable { onWaveMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    WaveIcon(modifier = Modifier.size(24.dp), color = Color.White)
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "2.1",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NO.2",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    color = Color.White,
                    onClick = onHeartClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 内容区
            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Row {
                    Text(
                        "#",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "卷积神经网络 (CNN): 核心是局部连接和权值共享，通过卷积层提取特征，池化层进行降维和特征聚合（如最大池化、平均池化），最终通过全连接层输出。它擅长处理具有空间结构的数据，如图像。",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row {
                    Text(
                        "#",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF007AFF),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("7", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "循环神经网络 (RNN): 核心是循环结构和状态记忆。它的隐藏层 t 不仅依赖于当前输入 x_t，还依赖于上一时刻的隐藏状态 s_{t-1}，从而能处理序列数据。但存在长期依赖问题。",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // 右侧悬浮指示器
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .clickable { onRightIndicatorClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("费曼", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(32.dp)
            )
            Text("学习法", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun WaveIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.5.dp.toPx()
        for (i in 0..2) {
            val y = h * (0.35f + i * 0.15f)
            val path = Path().apply {
                moveTo(w * 0.2f, y)
                quadraticTo(w * 0.35f, y - 4.dp.toPx(), w * 0.5f, y)
                quadraticTo(w * 0.65f, y + 4.dp.toPx(), w * 0.8f, y)
            }
            drawPath(path, color = color, style = Stroke(width = stroke))
        }
    }
}
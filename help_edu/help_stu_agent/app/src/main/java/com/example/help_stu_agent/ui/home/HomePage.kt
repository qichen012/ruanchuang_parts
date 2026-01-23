package com.example.help_stu_agent.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Screen { Home, Reading, PastContent }

data class ReflectionItem(
    val title: String,
    val description: String,
    val iconColor: Color = Color(0xFF6366F1)
)

data class HomeClickCallbacks(
    val onTopMenuClick: () -> Unit = {},
    val onNotebookClick: () -> Unit = {},
    val onReflectionCardClick: (ReflectionItem) -> Unit = {},
    val onSwipeUpToPast: () -> Unit = {},
    val onPastWaveMenuClick: () -> Unit = {},
    val onPastHeartClick: () -> Unit = {},
    val onPastRightIndicatorClick: () -> Unit = {},
    val onSaveReflectionClick: (ReflectionItem) -> Unit = {},
)

@Composable
fun HomePage(
    onMenuClick: () -> Unit,
    onRightActionClick: () -> Unit,
    onGoUploadPdf: () -> Unit,
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedCard by remember { mutableStateOf<ReflectionItem?>(null) }

    val reflections = remember {
        listOf(
            ReflectionItem(
                "What was the highlight of your day today?",
                "Take a moment to appreciate a small win or a pleasant surprise."
            ),
            ReflectionItem(
                "What are you grateful for today?",
                "Think about someone who made your day better."
            ),
            ReflectionItem(
                "What challenge did you overcome?",
                "Acknowledge your strength in handling difficult moments."
            )
        )
    }

    // 这里统一配置：右上角只进上传 PDF
    val callbacks = remember(onMenuClick, onRightActionClick, onGoUploadPdf) {
        HomeClickCallbacks(
            onTopMenuClick = onMenuClick,

            onNotebookClick = {
                onRightActionClick()
                onGoUploadPdf() // 固定进入上传 PDF
            },

            onReflectionCardClick = { item ->
                selectedCard = item
                currentScreen = Screen.Reading
            },

            onSwipeUpToPast = {
                currentScreen = Screen.PastContent
            },

            onPastWaveMenuClick = {
                onRightActionClick()
            },

            onPastHeartClick = {
                // TODO: 收藏/喜欢（预留）
            },

            // 右侧“费曼学习法”入口也改为进入上传 PDF（防止仍可直达知识树）
            onPastRightIndicatorClick = {
                onGoUploadPdf()
            },

            onSaveReflectionClick = {
                // TODO: 保存 reflection（预留）
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (targetState == Screen.PastContent || initialState == Screen.PastContent) {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(500)) togetherWith
                            slideOutVertically(targetOffsetY = { it }, animationSpec = tween(500))
                } else {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                }
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.Home -> ReflectionHomeScreen(
                    items = reflections,
                    callbacks = callbacks
                )

                Screen.Reading -> if (selectedCard != null) {
                    ReadingModeScreen(
                        item = selectedCard!!,
                        onBack = { currentScreen = Screen.Home },
                        onSave = { callbacks.onSaveReflectionClick(selectedCard!!) }
                    )
                }

                Screen.PastContent -> PastContentScreen(
                    onBack = { currentScreen = Screen.Home },
                    callbacks = callbacks
                )
            }
        }
    }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }
}

@Composable
fun ReflectionHomeScreen(
    items: List<ReflectionItem>,
    callbacks: HomeClickCallbacks
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp, y = 50.dp)
                .size(300.dp, 600.dp)
                .rotate(-15f)
                .background(Color(0xFFE8EFFF), RoundedCornerShape(80.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    onClick = callbacks.onTopMenuClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Menu,
                            null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFE4D6),
                    onClick = callbacks.onNotebookClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        NotebookIcon(modifier = Modifier.size(24.dp), color = Color(0xFF64748B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "Good evening",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 28.dp)
            )
            Text(
                "Ready to reflect on your day?",
                fontSize = 18.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 40.dp),
                pageSpacing = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                ReflectionCard(
                    item = items[page],
                    onClick = { callbacks.onReflectionCardClick(items[page]) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta < -20) callbacks.onSwipeUpToPast()
                        }
                    )
                    .clickable { callbacks.onSwipeUpToPast() }
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("回顾", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(40.dp)
                )
                Text("往日内容", fontSize = 15.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PastContentScreen(
    onBack: () -> Unit,
    callbacks: HomeClickCallbacks
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
                        .clickable { callbacks.onPastWaveMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    WaveIcon(modifier = Modifier.size(24.dp), color = Color.White)
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "7.15",
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
                    onClick = callbacks.onPastHeartClick
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
                        text = "极简导航结构：底部五栏覆盖 DeFi 核心场景 “Stake | Portfolio | Swap | Liquidity | Settings” 五个图标精准对应质押、资产管理、兑换、流动性提供、设置，满足资深用户一站式需求。",
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
                                text = "动线闭环：从“选择币种”到“完成兑换”3步达成 用户只需：选择支付/接收币种 → 输入数量 → 滑动确认，即可完成兑换。系统自动计算最优路径与预估到账金额，无需手动查询。",
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

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .clickable { callbacks.onPastRightIndicatorClick() },
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

@Composable
fun ReflectionCard(item: ReflectionItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(400.dp)
                .offset(x = 12.dp, y = 12.dp)
                .rotate(3f)
                .background(Color(0xFFE8EFFF), RoundedCornerShape(40.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(400.dp)
                .offset(x = 6.dp, y = 6.dp)
                .rotate(1.5f)
                .background(Color(0xFFF1F5FF), RoundedCornerShape(40.dp))
        )

        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .shadow(10.dp, RoundedCornerShape(40.dp)),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(32.dp).fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SparklesIcon(modifier = Modifier.size(28.dp), color = item.iconColor)
                }
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    item.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    item.description,
                    fontSize = 17.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D5FEF)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("View", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ReadingModeScreen(
    item: ReflectionItem,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF475569))
            }
            Text(
                "READING MODE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFEEF2FF), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                SparklesIcon(modifier = Modifier.size(32.dp), color = item.iconColor)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                item.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E293B),
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                item.description,
                fontSize = 18.sp,
                color = Color(0xFF64748B),
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(32.dp))
                    .shadow(1.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(0.05f))
                    .padding(24.dp)
            ) {
                Text("Type your reflection here...", color = Color(0xFF94A3B8), fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB4B9FF)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Save Reflection", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun NotebookIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.8f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRect(
            color = color,
            topLeft = Offset(w * 0.3f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.15f),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(color = color, start = Offset(w * 0.3f, h * 0.4f), end = Offset(w * 0.7f, h * 0.4f), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = Offset(w * 0.3f, h * 0.55f), end = Offset(w * 0.7f, h * 0.55f), strokeWidth = 2.dp.toPx())
        drawLine(color = color, start = Offset(w * 0.3f, h * 0.7f), end = Offset(w * 0.7f, h * 0.7f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
fun SparklesIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.5f
            val radius = size.width * 0.4f
            moveTo(centerX, centerY - radius)
            quadraticTo(centerX, centerY, centerX + radius, centerY)
            quadraticTo(centerX, centerY, centerX, centerY + radius)
            quadraticTo(centerX, centerY, centerX - radius, centerY)
            quadraticTo(centerX, centerY, centerX, centerY - radius)
            close()
        }
        drawPath(path, color = color)
        drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.2f, size.height * 0.2f))
        drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.8f, size.height * 0.8f))
    }
}

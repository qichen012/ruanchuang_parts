package com.example.help_stu_agent.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import androidx.compose.ui.platform.LocalContext
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository


enum class Screen { Home, Reading, PastContent }

data class ReflectionItem(
    val id: String,
    val title: String,
    val description: String,
    val quote: String,
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
    val onSaveReflectionClick: (ReflectionItem) -> Unit = {}
)

@Composable
fun HomePage(
    onMenuClick: () -> Unit,
    onGoUploadPdf: () -> Unit,
    onOpenKnowledgeCard: (String) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var selectedCard by remember { mutableStateOf<ReflectionItem?>(null) }

    val context = LocalContext.current
    val cardRepo = remember { KnowledgeCardRepository(context) }
    val cardEntities by cardRepo.observeAll().collectAsState(initial = emptyList())

    val reflections = remember(cardEntities) {
        if (cardEntities.isNotEmpty()) {
            cardEntities.map { e ->
                ReflectionItem(
                    id = e.id,
                    title = e.headerTitle ?: (e.pdfDisplayName ?: "知识卡片"),
                    description = e.headerSubtitle ?: (e.category ?: ""),
                    quote = e.footerQuote ?: ""
                )
            }
        } else {
            listOf(
                ReflectionItem(
                    id = "demo_01",
                    title = "上传 PDF 生成知识卡片",
                    description = "上传后将自动生成章节要点与关键知识点。",
                    quote = "从历史中回看你的知识结构。"
                ),
                ReflectionItem(
                    id = "demo_02",
                    title = "支持知识树结构化",
                    description = "点击卡片进入详情，查看 key_points。",
                    quote = "结构化比堆材料更重要。"
                ),
                ReflectionItem(
                    id = "demo_03",
                    title = "自动保存为历史",
                    description = "每次上传都会保存到 Room，随时回溯。",
                    quote = "让知识形成网络，而不是列表。"
                )
            )
        }
    }


    // 这里统一配置：右上角只进上传 PDF
    val callbacks = remember(onMenuClick, onGoUploadPdf) {
        HomeClickCallbacks(
            onTopMenuClick = onMenuClick,

            // 右上角按钮：只进上传 PDF（避免触发其他导航）
            onNotebookClick = {
                onGoUploadPdf()
            },

            onReflectionCardClick = { item ->
                onOpenKnowledgeCard(item.id)
            },

            onSwipeUpToPast = {
                currentScreen = Screen.PastContent
            },

            onPastWaveMenuClick = {
                // TODO:
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

            FlipPager(
                items = items,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                onCardClick = { callbacks.onReflectionCardClick(it) }
            )

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
                Spacer(modifier = Modifier.height(18.dp))
                if (item.quote.isNotBlank()) {
                    Text(
                        "“${item.quote}”",
                        fontSize = 16.sp,
                        color = Color(0xFF334155),
                        lineHeight = 24.sp
                    )
                }
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

private enum class DeckPhase { Idle, Returning, FlyingOut, Shifting }

private data class Pose(
    val x: Float,
    val y: Float,
    val rot: Float,
    val scale: Float,
    val alpha: Float
)

private fun poseForLayer(
    layer: Int,
    fanDx: Float,
    fanDy: Float,
    fanRot: Float,
    fanScale: Float,
    alphaDecay: Float
): Pose {
    val x = layer * fanDx
    val y = layer * fanDy
    val rot = layer * fanRot
    val scale = 1f - layer * fanScale
    // 底层不要太透明，否则容易产生“虚影”观感
    val alpha = (1f - layer * alphaDecay).coerceAtLeast(0.92f)
    return Pose(x, y, rot, scale, alpha)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun floorMod(a: Int, n: Int): Int {
    val r = a % n
    return if (r < 0) r + n else r
}

@Composable
private fun FlipPager(
    items: List<ReflectionItem>,
    modifier: Modifier = Modifier,
    onCardClick: (ReflectionItem) -> Unit
) {
    if (items.isEmpty()) return

    val scope = rememberCoroutineScope()

    // 当前顶卡索引（循环）
    var topIndex by remember { mutableIntStateOf(0) }

    // 顶卡跟手位移（无需协程，避免卡顿）
    var followX by remember { mutableFloatStateOf(0f) }
    var followY by remember { mutableFloatStateOf(0f) }

    // 动画位移（只在“回弹 / 飞出”时使用）
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }

    // 补位动画：控制“下一张从第二层平滑顶到顶层”
    // 1f=堆叠已到位；补位时 snapTo(0f) -> animateTo(1f)
    val shift = remember { Animatable(1f) }

    var phase by remember { mutableStateOf(DeckPhase.Idle) }

    // 一次拖拽只触发一次翻页
    var flippedInThisDrag by remember { mutableStateOf(false) }

    // 手势累计
    var accumX by remember { mutableFloatStateOf(0f) }
    var accumY by remember { mutableFloatStateOf(0f) }

    // --------- 扇形堆叠参数（只影响堆叠，不改变卡片样式） ---------
    val visibleCount = 6.coerceAtMost(items.size)

    // 更明显的堆叠
    val fanDx = 34f
    val fanDy = 22f
    val fanRot = 7.5f
    val fanScale = 0.06f
    val alphaDecay = 0.10f // 不要太大，否则虚影感会回来

    // 跟手最大距离
    val maxFollow = 280f

    // 触发阈值（拖到这个距离就“甩出下一张”）
    val triggerDist = 130f

    // 飞出距离（px）
    val outDist = 1000f

    fun idx(offset: Int): Int = floorMod(topIndex + offset, items.size)

    fun smoothBack() {
        if (phase != DeckPhase.Idle) return
        phase = DeckPhase.Returning

        val startX = followX
        val startY = followY

        scope.launch {
            animX.snapTo(startX)
            animY.snapTo(startY)

            val spec = tween<Float>(170)
            val j1 = launch { animX.animateTo(0f, spec) }
            val j2 = launch { animY.animateTo(0f, spec) }
            j1.join(); j2.join()

            followX = 0f
            followY = 0f
            phase = DeckPhase.Idle
        }
    }

    fun commitNextWithDirection(dx: Float, dy: Float) {
        if (phase != DeckPhase.Idle || items.size <= 1) return
        phase = DeckPhase.FlyingOut

        // 方向归一化：飞出轨迹严格沿手指方向
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val ndx = dx / len
        val ndy = dy / len

        val startX = followX
        val startY = followY

        val targetX = ndx * outDist
        val targetY = ndy * outDist

        scope.launch {
            // Phase A: 顶卡飞出
            animX.snapTo(startX)
            animY.snapTo(startY)

            val specOut = tween<Float>(240)
            val j1 = launch { animX.animateTo(targetX, specOut) }
            val j2 = launch { animY.animateTo(targetY, specOut) }
            j1.join(); j2.join()

            // 更新索引：永远到“下一张”（因此下一张必然是第二层卡片）
            topIndex = floorMod(topIndex + 1, items.size)

            // 清空跟手位移
            followX = 0f
            followY = 0f
            animX.snapTo(0f)
            animY.snapTo(0f)

            // Phase B: 堆叠补位（关键：避免“瞬移”）
            phase = DeckPhase.Shifting
            shift.snapTo(0f)
            shift.animateTo(1f, tween(durationMillis = 220))

            phase = DeckPhase.Idle
        }
    }

    // 计算当前顶卡渲染位移：跟手 or 动画
    val tx = when (phase) {
        DeckPhase.Returning, DeckPhase.FlyingOut -> animX.value
        else -> followX
    }
    val ty = when (phase) {
        DeckPhase.Returning, DeckPhase.FlyingOut -> animY.value
        else -> followY
    }

    // 顶卡“抽出感”：轻微旋转/放大（不改变卡片样式，仅 transform）
    val dist = sqrt(tx * tx + ty * ty)
    val lift = (dist / 1400f).coerceIn(0f, 0.10f)
    val rot = (-tx / 26f).coerceIn(-20f, 20f) + (ty / 220f).coerceIn(-5f, 5f)

    // shift 进度：补位时 0->1，平时保持 1
    val tShift = shift.value.coerceIn(0f, 1f)

    // 顶卡 base pose：补位阶段让它从 layer=1 平滑到 layer=0
    // 飞出阶段不要叠加 base（避免飞出轨迹偏移），直接以 (0,0) 为基准
    val baseTop = if (phase == DeckPhase.FlyingOut) {
        poseForLayer(0, fanDx, fanDy, fanRot, fanScale, alphaDecay)
    } else {
        val from = poseForLayer(1, fanDx, fanDy, fanRot, fanScale, alphaDecay)
        val to = poseForLayer(0, fanDx, fanDy, fanRot, fanScale, alphaDecay)
        Pose(
            x = lerp(from.x, to.x, tShift),
            y = lerp(from.y, to.y, tShift),
            rot = lerp(from.rot, to.rot, tShift),
            scale = lerp(from.scale, to.scale, tShift),
            alpha = 1f // 顶卡拖拽时不透明，避免虚影
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        // 背景堆叠：补位时每层从(layer+1)->(layer)平滑前移
        for (layer in (visibleCount - 1) downTo 1) {
            val item = items[idx(layer)]

            val from = poseForLayer(layer + 1, fanDx, fanDy, fanRot, fanScale, alphaDecay)
            val to = poseForLayer(layer, fanDx, fanDy, fanRot, fanScale, alphaDecay)

            val px = lerp(from.x, to.x, tShift)
            val py = lerp(from.y, to.y, tShift)
            val prot = lerp(from.rot, to.rot, tShift)
            val pscale = lerp(from.scale, to.scale, tShift)
            val palpha = lerp(from.alpha, to.alpha, tShift)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 关键修复：layer 越小越靠上，确保“第二层”成为下一张顶卡
                    .zIndex((visibleCount - layer).toFloat())
                    .graphicsLayer {
                        translationX = px
                        translationY = py
                        rotationZ = prot
                        scaleX = pscale
                        scaleY = pscale
                        alpha = palpha
                    }
            ) {
                ReflectionCard(item = item, onClick = { onCardClick(item) })
            }
        }

        // 顶卡：支持任意方向扇出；底层补位动画期间会平滑到顶层
        val topItem = items[idx(0)]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(999f)
                .pointerInput(items.size) {
                    detectDragGestures(
                        onDragStart = {
                            if (phase != DeckPhase.Idle) return@detectDragGestures
                            accumX = 0f
                            accumY = 0f
                            flippedInThisDrag = false
                        },
                        onDrag = { change, dragAmount ->
                            if (phase != DeckPhase.Idle || flippedInThisDrag) return@detectDragGestures
                            change.consume()

                            accumX += dragAmount.x
                            accumY += dragAmount.y

                            // 1:1 跟手（无协程，不卡）
                            followX = accumX.coerceIn(-maxFollow, maxFollow)
                            followY = accumY.coerceIn(-maxFollow, maxFollow)

                            val d = sqrt(accumX * accumX + accumY * accumY)
                            if (d > triggerDist) {
                                flippedInThisDrag = true
                                commitNextWithDirection(accumX, accumY)
                            }
                        },
                        onDragEnd = {
                            if (phase == DeckPhase.Idle && !flippedInThisDrag) smoothBack()
                            accumX = 0f
                            accumY = 0f
                            flippedInThisDrag = false
                        },
                        onDragCancel = {
                            if (phase == DeckPhase.Idle) smoothBack()
                            accumX = 0f
                            accumY = 0f
                            flippedInThisDrag = false
                        }
                    )
                }
                .graphicsLayer {
                    translationX = baseTop.x + tx
                    translationY = baseTop.y + ty
                    rotationZ = baseTop.rot + rot
                    val s = (1f + lift) * baseTop.scale
                    scaleX = s
                    scaleY = s

                    // 拖拽时不透明；飞出阶段最多轻微淡出
                    alpha = if (phase == DeckPhase.FlyingOut) (1f - (dist / 1800f).coerceIn(0f, 0.10f)) else 1f

                    // 离屏合成，减少“虚影/重影”
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            ReflectionCard(item = topItem, onClick = { onCardClick(topItem) })
        }
    }
}

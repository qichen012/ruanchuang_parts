package com.example.help_stu_agent.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository

// 移除 PastContent，使之变为由 NavHost 接管的独立页面
enum class Screen { Home, Reading }

data class ReflectionItem(
    val id: String,
    val title: String,
    val quote: String,
    val iconColor: Color = Color(0xFF6366F1)
)

data class HomeClickCallbacks(
    val onReflectionCardClick: (ReflectionItem) -> Unit = {},
    val onSwipeUpToPast: () -> Unit = {},
    val onPastWaveMenuClick: () -> Unit = {},
    val onPastHeartClick: () -> Unit = {},
    val onPastRightIndicatorClick: () -> Unit = {},
    val onSaveReflectionClick: (ReflectionItem) -> Unit = {}
)

@Composable
fun HomePage(
    onOpenKnowledgeCard: (String) -> Unit,
    onGoPastContent: () -> Unit // 新增：通过路由跳转到往日内容页面
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
                    quote = e.footerQuote ?: ""
                )
            }
        } else {
            listOf(
                ReflectionItem(
                    id = "demo_01",
                    title = "上传 PDF 生成知识卡片",
                    quote = "从历史中回看你的知识结构。"
                ),
                ReflectionItem(
                    id = "demo_02",
                    title = "支持知识树结构化",
                    quote = "结构化比堆材料更重要。"
                ),
                ReflectionItem(
                    id = "demo_03",
                    title = "自动保存为历史",
                    quote = "让知识形成网络，而不是列表。"
                )
            )
        }
    }

    val callbacks = remember {
        HomeClickCallbacks(
            onReflectionCardClick = { item ->
                onOpenKnowledgeCard(item.id)
            },
            onSwipeUpToPast = {
                onGoPastContent() // 触发外部传入的路由跳转
            },
            onPastWaveMenuClick = {
                // TODO:
            },
            onPastHeartClick = {
                // TODO: 收藏/喜欢（预留）
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
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
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
            }
        }
    }
}

@Composable
fun ReflectionHomeScreen(
    items: List<ReflectionItem>,
    callbacks: HomeClickCallbacks
) {
    // 获取当前时间并动态生成问候语
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

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

            Spacer(modifier = Modifier.height(40.dp))

            // 使用动态计算的问候语
            Text(
                text = greeting,
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

            // 【关键修改】使用 weight(1f) 将卡片下方的所有空白区域作为上滑/点击的触发区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta < -20) callbacks.onSwipeUpToPast()
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // 移除点击涟漪，让手势区更自然
                    ) { callbacks.onSwipeUpToPast() },
                contentAlignment = Alignment.BottomCenter
            ) {
                // 保持 padding 避免被 TabBar 遮挡
                Column(
                    modifier = Modifier.padding(bottom = 110.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("上滑探索", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        null,
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(36.dp)
                    )
                    Text("往日记忆", fontSize = 13.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
                }
            }
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
                .height(480.dp) // 稍微加高一点，留白更美观
                .shadow(12.dp, RoundedCornerShape(40.dp)),
            shape = RoundedCornerShape(40.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(32.dp).fillMaxSize()) {
                // 1. 顶部图标
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SparklesIcon(modifier = Modifier.size(32.dp), color = item.iconColor)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2.  (header.title)
                Text(
                    text = item.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    lineHeight = 34.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                // 3.  (footer.quote)
                if (item.quote.isNotBlank()) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.width(40.dp),
                            thickness = 3.dp,
                            color = item.iconColor.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "“${item.quote}”",
                            fontSize = 18.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF64748B),
                            lineHeight = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 4. 按钮
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D5FEF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("view", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            if (item.quote.isNotBlank()) {
                Text(
                    "QUOTE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“${item.quote}”",
                    fontSize = 18.sp,
                    color = Color(0xFF475569),
                    lineHeight = 28.sp,
                    fontStyle = FontStyle.Italic
                )
            } else {
                Text(
                    text = "（此卡片暂无引用）",
                    fontSize = 16.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 24.sp
                )
            }
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

    var topIndex by remember { mutableIntStateOf(0) }

    var followX by remember { mutableFloatStateOf(0f) }
    var followY by remember { mutableFloatStateOf(0f) }

    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }

    val shift = remember { Animatable(1f) }

    var phase by remember { mutableStateOf(DeckPhase.Idle) }

    var flippedInThisDrag by remember { mutableStateOf(false) }

    var accumX by remember { mutableFloatStateOf(0f) }
    var accumY by remember { mutableFloatStateOf(0f) }

    val visibleCount = 6.coerceAtMost(items.size)

    val fanDx = 34f
    val fanDy = 22f
    val fanRot = 7.5f
    val fanScale = 0.06f
    val alphaDecay = 0.10f

    val maxFollow = 280f
    val triggerDist = 130f
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

        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val ndx = dx / len
        val ndy = dy / len

        val startX = followX
        val startY = followY

        val targetX = ndx * outDist
        val targetY = ndy * outDist

        scope.launch {
            animX.snapTo(startX)
            animY.snapTo(startY)

            val specOut = tween<Float>(240)
            val j1 = launch { animX.animateTo(targetX, specOut) }
            val j2 = launch { animY.animateTo(targetY, specOut) }
            j1.join(); j2.join()

            topIndex = floorMod(topIndex + 1, items.size)

            followX = 0f
            followY = 0f
            animX.snapTo(0f)
            animY.snapTo(0f)

            phase = DeckPhase.Shifting
            shift.snapTo(0f)
            shift.animateTo(1f, tween(durationMillis = 220))

            phase = DeckPhase.Idle
        }
    }

    val tx = when (phase) {
        DeckPhase.Returning, DeckPhase.FlyingOut -> animX.value
        else -> followX
    }
    val ty = when (phase) {
        DeckPhase.Returning, DeckPhase.FlyingOut -> animY.value
        else -> followY
    }

    val dist = sqrt(tx * tx + ty * ty)
    val lift = (dist / 1400f).coerceIn(0f, 0.10f)
    val rot = (-tx / 26f).coerceIn(-20f, 20f) + (ty / 220f).coerceIn(-5f, 5f)

    val tShift = shift.value.coerceIn(0f, 1f)

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
            alpha = 1f
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {

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

                    alpha = if (phase == DeckPhase.FlyingOut) (1f - (dist / 1800f).coerceIn(0f, 0.10f)) else 1f

                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            ReflectionCard(item = topItem, onClick = { onCardClick(topItem) })
        }
    }
}
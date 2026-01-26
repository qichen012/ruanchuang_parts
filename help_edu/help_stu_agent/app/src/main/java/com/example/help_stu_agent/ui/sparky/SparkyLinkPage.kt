package com.example.help_stu_agent.ui.sparky

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt


data class SparkyReport(
    val id: String,
    val title: String,
    val quote: String,
    val createdAt: Long
)

data class SparkyDayGroup(
    val dayKey: String,
    val dayLabel: String,
    val reports: List<SparkyReport>,
    val color: Color
)

private sealed class SparkyMode {
    data object Collapsed : SparkyMode()
    data class Focus(
        val dayKey: String,
        val anchor: Offset, // The center of the day bubble that was clicked -> used for the return animation
        val color: Color
    ) : SparkyMode()
}

private data class Star(
    val x: Float,
    val y: Float,
    val r: Float,
    val blinkSpeed: Float,
    val offsetPhase: Float
)

private data class FusedState(
    val report: SparkyReport,
    val from: Offset,
    val color: Color
)

private val PastelRainbow = listOf(
    Color(0xFF5D5FEF), // Purple Blue
    Color(0xFFFF5D86), // Hot Pink
    Color(0xFF00C9A7), // Teal Green
    Color(0xFFFF9F43), // Orange
    Color(0xFF4D96FF), // Soft Blue
    Color(0xFF845EC2), // Deep Purple
    Color(0xFFFF6B6B)  // Red
)

@Composable
fun SparkyLinkPage(
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeCardRepository(context) }
    val cardEntities by repo.observeAll().collectAsState(initial = emptyList())

    val groups = remember(cardEntities) {
        val rawGroups = buildLast7DaysGroups(
            reports = cardEntities.map { e ->
                SparkyReport(
                    id = e.id,
                    title = e.headerTitle ?: (e.pdfDisplayName ?: "Daily Report"),
                    quote = e.footerQuote ?: "",
                    createdAt = e.createdAt
                )
            }
        )
        rawGroups.mapIndexed { index, group ->
            group.copy(color = PastelRainbow[index % PastelRainbow.size])
        }
    }

    SparkyLinkScene(
        groups = groups,
        onBack = onBack,
        onOpenReport = onOpenReport
    )
}

private fun buildLast7DaysGroups(reports: List<SparkyReport>): List<SparkyDayGroup> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val byDay = reports.groupBy { keyFmt.format(it.createdAt) }

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val weekdayFmt = SimpleDateFormat("EEE", Locale.ENGLISH)

    return (0..6).map { i ->
        val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
        val millis = c.timeInMillis
        val key = keyFmt.format(millis)
        val label = if (i == 0) "TODAY" else weekdayFmt.format(millis).uppercase(Locale.ENGLISH)
        SparkyDayGroup(key, label, byDay[key].orEmpty(), Color.Transparent)
    }.reversed()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SparkyLinkScene(
    groups: List<SparkyDayGroup>,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit
) {
    // --- Visual Constants ---
    val ink = Color(0xFF1E293B)

    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var mode by remember { mutableStateOf<SparkyMode>(SparkyMode.Collapsed) }

    // --- Time for continuous animations (breathing, floating) ---
    val inf = rememberInfiniteTransition(label = "sparky_time")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val tw = (t * 2f * PI).toFloat()
    val stars = remember { generateStars(150) }

    // --- Unified Transition for Collapsed <-> Focus modes ---
    val transition = updateTransition(targetState = mode, label = "ModeTransition")

    val focus = mode as? SparkyMode.Focus
    val isFocus = focus != null

    val w = viewport.width.toFloat()
    val h = viewport.height.toFloat()
    val center = Offset(w / 2f, h / 2f)

    // --- Animated Values ---
    val globalScale by transition.animateFloat(
        label = "GlobalScale",
        transitionSpec = { spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow) }
    ) { if (it is SparkyMode.Focus) 1.1f else 1.0f }

    // Sparky bubble's target position
    val sparkyTargetPos = remember(w, h) { Offset(w / 2f, h * 0.25f) }
    val sparkyPos by transition.animateOffset(
        label = "SparkyPos",
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) }
    ) { if (it is SparkyMode.Focus) sparkyTargetPos else center }

    // Day bubble's target position
    val dayTargetPos = remember(w, h) { Offset(w / 2f, h * 0.70f) }
    val dayPos by transition.animateOffset(
        label = "DayPos",
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) }
    ) { targetMode ->
        when (targetMode) {
            // 如果目标是聚焦状态，移动到屏幕上方的固定位置
            is SparkyMode.Focus -> dayTargetPos

            // 如果目标是折叠状态，我们需要回到它原本在圆圈中的位置（anchor）
            is SparkyMode.Collapsed -> {
                // 尝试从 transition 的当前状态（即还没退出的 Focus 状态）中获取 anchor
                (transition.currentState as? SparkyMode.Focus)?.anchor ?: center
            }
        }
    }

    // Report bubbles' orbit expansion progress (0 -> 1)
    val orbitProgress by transition.animateFloat(
        label = "OrbitProgress",
        transitionSpec = { tween(420, delayMillis = 80, easing = FastOutSlowInEasing) }
    ) { if (it is SparkyMode.Focus) 1f else 0f }


    // --- Drag & Drop State ---
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val fusedIds = remember { mutableStateListOf<String>() }
    val fusedStack = remember { mutableStateListOf<FusedState>() }
    var statusAdded by remember { mutableStateOf<String?>(null) }

    // --- Animation for fusion/unfusion ---
    val sparkyPulse = remember { Animatable(1f) }
    var animBall by remember { mutableStateOf<FusedState?>(null) }
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val animS = remember { Animatable(1f) }
    val animA = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding()
            .onSizeChanged { viewport = it }
    ) {
        if (w == 0f || h == 0f) return@Box

        // --- Background ---
        StarryBackground(
            center = center,
            width = w,
            height = h,
            stars = stars,
            time = tw,
            scale = if (isFocus) 1.2f else 1.0f // Subtle zoom on background
        )

        AnimatedVisibility(visible = !isFocus, enter = fadeIn(), exit = fadeOut()) {
            TopAppBar(
                title = {
                    Column {
                        Text("Sparky Link", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Daily Report Hub",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F9FE)
                )
            )
        }


        // --- Added Report Status Bar ---
        AnimatedVisibility(
            visible = (isFocus && statusAdded != null),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
        ) {
            FusedReportStatusBar(
                statusText = statusAdded ?: "",
                canUndo = fusedStack.isNotEmpty(),
                onUndo = {
                    val last = fusedStack.removeLastOrNull() ?: return@FusedReportStatusBar
                    fusedIds.remove(last.report.id)
                    statusAdded = fusedStack.lastOrNull()?.report?.title

                    // Animate the bubble back from Sparky's core
                    scope.launch {
                        val sparkyCurrentPos = sparkyPos + Offset(0f, 6f * sin(tw))
                        animBall = last
                        animX.snapTo(sparkyCurrentPos.x); animY.snapTo(sparkyCurrentPos.y)
                        animS.snapTo(0.15f); animA.snapTo(1f)

                        launch { animX.animateTo(last.from.x, spring(dampingRatio = 0.62f)) }
                        launch { animY.animateTo(last.from.y, spring(dampingRatio = 0.62f)) }
                        launch { animS.animateTo(1f, spring(dampingRatio = 0.62f)) }
                            .invokeOnCompletion { animBall = null }
                    }
                }
            )
        }


        // --- Core Scene ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = globalScale; scaleY = globalScale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            // 1. Sparky Core Bubble
            val bigBaseR = min(w, h) * 0.17f
            val bigBreath = 1f + 0.02f * sin(tw * 1.5f)
            val sparkyFloatingY = if (isFocus) (6f * sin(tw)) else (20f * sin(tw))
            val sparkyCurrentPos = sparkyPos + Offset(0f, sparkyFloatingY)
            val sparkyCurrentR = bigBaseR * bigBreath * sparkyPulse.value

            SparkyCoreBubble(
                center = sparkyCurrentPos,
                radius = sparkyCurrentR,
                time = tw,
                isFusing = animBall != null
            )

            // 2. Day Bubbles (Collapsed Mode)
            if (!isFocus && transition.currentState == transition.targetState) {
                val orbitR = min(w, h) * 0.35f
                val n = max(1, groups.size)
                groups.forEachIndexed { idx, g ->
                    val baseAngle = (idx.toFloat() / n) * (2f * PI).toFloat() - (PI.toFloat() / 2f)
                    val wobble = 0.10f * sin(tw * 1.2f + idx * 1.7f)
                    val slowSwing = 0.25f * sin(tw * 0.35f)
                    val ang = baseAngle + wobble + slowSwing
                    val floatOffset = sin(tw * 2f + idx) * 12f

                    val bubblePos = Offset(
                        center.x + cos(ang) * orbitR,
                        center.y + sin(ang) * orbitR + floatOffset
                    )
                    val bubbleR = min(w, h) * 0.065f

                    BubbleNode(
                        pos = bubblePos,
                        radius = bubbleR,
                        color = g.color,
                        label = g.dayLabel,
                        subLabel = "${g.reports.size}",
                        onClick = {
                            mode = SparkyMode.Focus(g.dayKey, bubblePos, g.color)
                            draggingId = null
                            dragOffset = Offset.Zero
                            statusAdded = fusedStack.lastOrNull()?.report?.title
                        }
                    )
                }
            }

            // 3. Focused Day, Reports & Close Button
            if (focus != null) {
                val dayR = min(w, h) * 0.08f
                val closeR = min(w, h) * 0.06f
                val reportR = min(w, h) * 0.055f

                // Focused Day Bubble
                BubbleNode(
                    pos = dayPos,
                    radius = dayR,
                    color = focus.color,
                    label = groups.find { it.dayKey == focus.dayKey }?.dayLabel ?: "DAY",
                    onClick = { /* No-op when focused */ }
                )

                // Close Button
                val closePos = Offset(w / 2f, h - with(LocalDensity.current) { 70.dp.toPx() })
                BubbleNode(
                    pos = closePos,
                    radius = closeR,
                    color = Color(0xFFE2E8F0),
                    label = "CLOSE",
                    textColor = ink,
                    onClick = { mode = SparkyMode.Collapsed }
                )

                // Orbiting Report Bubbles
                val activeGroup = groups.find { it.dayKey == focus.dayKey }
                val reports = activeGroup?.reports.orEmpty().filter { it.id !in fusedIds }.take(12)
                val n = max(1, reports.size)
                val orbitRadius = min(w, h) * 0.15f * (1f + 0.03f * cos(tw * 2.2f)) // breathing orbit

                reports.forEachIndexed { i, r ->
                    val angle = (i.toFloat() / n) * 2f * PI.toFloat() + (tw * 0.15f) // slow rotation

                    val basePos = dayPos + Offset(
                        cos(angle) * orbitRadius * orbitProgress,
                        sin(angle) * orbitRadius * orbitProgress
                    )

                    val isDragging = draggingId == r.id
                    val currentPos = if (isDragging) basePos + dragOffset else basePos
                    val dragScale = if (isDragging) 1.18f else 1.0f

                    Box(
                        modifier = Modifier
                            .offset { IntOffset((currentPos.x - reportR).roundToInt(), (currentPos.y - reportR).roundToInt()) }
                            .size(pxToDp(reportR * 2f))
                            .graphicsLayer { scaleX = dragScale; scaleY = dragScale; alpha = orbitProgress }
                            .pointerInput(r.id) { detectTapGestures(onTap = { onOpenReport(r.id) }) }
                            .pointerInput(r.id) {
                                detectDragGestures(
                                    onDragStart = { draggingId = r.id; dragOffset = Offset.Zero },
                                    onDrag = { change, amount -> change.consume(); if (animBall == null) dragOffset += amount },
                                    onDragCancel = { draggingId = null; dragOffset = Offset.Zero },
                                    onDragEnd = {
                                        val dist = distance(currentPos, sparkyCurrentPos)
                                        if (dist < bigBaseR * 1.1f && animBall == null) {
                                            val fuseState = FusedState(r, currentPos, focus.color)
                                            scope.launch {
                                                // 1. Trigger Sparky's pulse
                                                launch {
                                                    sparkyPulse.animateTo(1.15f, tween(150))
                                                    sparkyPulse.animateTo(1.0f, spring(dampingRatio = 0.45f))
                                                }

                                                // 2. Start the fusion animation
                                                animBall = fuseState
                                                animX.snapTo(currentPos.x); animY.snapTo(currentPos.y)
                                                animS.snapTo(1f); animA.snapTo(1f)

                                                launch { animX.animateTo(sparkyCurrentPos.x, tween(420, easing = FastOutSlowInEasing)) }
                                                launch { animY.animateTo(sparkyCurrentPos.y, tween(420, easing = FastOutSlowInEasing)) }
                                                launch { animS.animateTo(0.10f, tween(420, easing = FastOutSlowInEasing)) }
                                                launch { animA.animateTo(0f, tween(280, easing = LinearEasing)) }.join()

                                                // 3. Update state after animation
                                                fusedIds.add(r.id)
                                                fusedStack.add(fuseState)
                                                statusAdded = r.title
                                                animBall = null
                                            }
                                        }
                                        draggingId = null
                                        dragOffset = Offset.Zero
                                    }
                                )
                            }
                    ) {
                        JellyBubble(color = focus.color, tag = shortTag(r.title))
                    }
                }
            }

            // 4. Fusion/Unfusion Animation Bubble (rendered on top)
            val ball = animBall
            if (ball != null) {
                val fx = animX.value; val fy = animY.value
                val fs = animS.value; val fa = animA.value.coerceIn(0f, 1f)
                val br = min(w, h) * 0.055f * fs // Use the same base radius as reports

                if (fa > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((fx - br).roundToInt(), (fy - br).roundToInt()) }
                            .size(pxToDp(br * 2f))
                            .graphicsLayer { alpha = fa }
                    ) {
                        JellyBubble(color = ball.color, tag = "")
                    }
                }
            }
        }
    }
}


// --- Visual Components ---

@Composable
private fun StarryBackground(
    center: Offset,
    width: Float,
    height: Float,
    stars: List<Star>,
    time: Float,
    scale: Float
) {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
        scaleX = scale; scaleY = scale
        transformOrigin = TransformOrigin.Center
    }) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1)),
                center = center,
                radius = max(width, height)
            )
        )
        stars.forEach { star ->
            val brightness = 0.3f + 0.7f * sin(time * star.blinkSpeed + star.offsetPhase)
            val alpha = (brightness * 0.8f).coerceIn(0f, 1f)
            val px = star.x * width
            val py = star.y * height
            drawCircle(
                color = Color.White,
                radius = star.r * min(width, height) * (0.8f + 0.4f * brightness),
                center = Offset(px, py),
                alpha = alpha
            )
            if (star.r > 0.003f) {
                drawCircle(
                    color = Color(0xFF818CF8).copy(alpha = alpha * 0.3f),
                    radius = star.r * min(width, height) * 4f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

@Composable
private fun SparkyCoreBubble(
    center: Offset,
    radius: Float,
    time: Float,
    isFusing: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val c = center
        val r = radius
        val glowColor = if (isFusing) Color(0xFFA78BFA) else Color(0xFF5D5FEF)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.3f), Color.Transparent),
                center = c,
                radius = r * 1.5f
            ),
            radius = r * 1.5f,
            center = c
        )
        val bodyBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF818CF8), Color(0xFF4338CA)),
            start = c - Offset(r, r), end = c + Offset(r, r)
        )
        drawCircle(brush = bodyBrush, radius = r, center = c)
        val swirlOffset = Offset(cos(time) * r * 0.3f, sin(time) * r * 0.3f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                center = c + swirlOffset,
                radius = r * 0.6f
            ),
            radius = r * 0.6f,
            center = c + swirlOffset
        )
        drawPath(
            path = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(center = c - Offset(0f, r * 0.4f), radius = r * 0.5f))
            },
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
                start = c - Offset(0f, r), end = c
            )
        )
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt()) }
            .size(pxToDp(radius * 2f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SPARKY", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text(
                if (isFusing) "PROCESSING..." else "LINK HUB",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
private fun BubbleNode(
    pos: Offset,
    radius: Float,
    color: Color,
    label: String,
    subLabel: String? = null,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((pos.x - radius).roundToInt(), (pos.y - radius).roundToInt()) }
            .size(pxToDp(radius * 2f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
    ) {
        JellyBubble(color = color, tag = "")
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = if (label.length > 5) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    color = textColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun JellyBubble(color: Color, tag: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = color.copy(alpha = 0.25f), radius = r * 1.1f, center = c + Offset(0f, r * 0.15f))
        val gradient = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.8f), color),
            center = c - Offset(r * 0.3f, r * 0.3f),
            radius = r * 1.5f
        )
        drawCircle(brush = gradient, radius = r, center = c)
        drawArc(
            color = Color.White.copy(alpha = 0.3f),
            startAngle = 0f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(c.x - r * 0.7f, c.y - r * 0.7f),
            size = androidx.compose.ui.geometry.Size(r * 1.4f, r * 1.4f),
            style = Stroke(width = r * 0.1f, cap = StrokeCap.Round),
            alpha = 0.5f
        )
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = r * 0.25f, center = c - Offset(r * 0.35f, r * 0.35f))
        drawCircle(color = Color.White.copy(alpha = 0.5f), radius = r * 0.1f, center = c - Offset(r * 0.15f, r * 0.55f))
    }
    if (tag.isNotEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(tag, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun FusedReportStatusBar(
    statusText: String,
    canUndo: Boolean,
    onUndo: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.80f),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Added: ",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge
            )
            if (canUndo) {
                Spacer(Modifier.width(12.dp))
                Surface(
                    onClick = onUndo,
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Refresh, null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "UNDO",
                            color = Color(0xFFA78BFA),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pxToDp(px: Float) = with(LocalDensity.current) { px.toDp() }

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun shortTag(title: String): String {
    val s = title.trim()
    if (s.isEmpty()) return "R"
    return s.take(1).uppercase(Locale.getDefault())
}

private fun generateStars(n: Int): List<Star> {
    val rnd = Random(System.currentTimeMillis())
    return List(n) {
        Star(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            r = 0.001f + rnd.nextFloat() * 0.004f,
            blinkSpeed = 1f + rnd.nextFloat() * 4f,
            offsetPhase = rnd.nextFloat() * 10f
        )
    }
}
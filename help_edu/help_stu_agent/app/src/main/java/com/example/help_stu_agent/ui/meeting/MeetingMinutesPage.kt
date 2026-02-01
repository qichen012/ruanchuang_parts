package com.example.help_stu_agent.ui.meeting

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


private enum class MeetScreen { Record, Processing, Text }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinutesPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }

    val rec by MeetingRecorder.state.collectAsState()

    var screen by remember { mutableStateOf(MeetScreen.Record) }
    var rawResult by remember { mutableStateOf<String?>(null) }

    // ✅ 从 Service 写入的 lastWorkId 来观察 WorkInfo
    val workerUuid = remember(rec.lastWorkId) {
        runCatching { rec.lastWorkId?.let { UUID.fromString(it) } }.getOrNull()
    }

    val wi by workerUuid?.let { id -> wm.getWorkInfoByIdLiveData(id).observeAsState() }
        ?: remember { mutableStateOf<WorkInfo?>(null) }

    var stage by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(wi) {
        val info = wi ?: return@LaunchedEffect
        stage = info.progress.getString(MeetingTranscribeWorker.KEY_STAGE).orEmpty()
        progress = info.progress.getFloat(MeetingTranscribeWorker.KEY_PROGRESS, 0f).coerceIn(0f, 1f)

        when (info.state) {
            WorkInfo.State.SUCCEEDED -> {
                rawResult = info.outputData.getString(MeetingTranscribeWorker.KEY_TEXT)
                screen = MeetScreen.Text
            }
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> screen = MeetScreen.Processing
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> screen = MeetScreen.Record
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) MeetingRecorder.start(context)
    }

    Scaffold(
        containerColor = Color(0xFF070A10),
        topBar = {
            TopAppBar(
                title = { Text("Meet Memo", color = Color(0xFFE8EEF8)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070A10).copy(alpha = 0.88f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A10))
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "meet_screen"
            ) { s ->
                when (s) {
                    MeetScreen.Record -> RecordingImmersive(
                        isRecording = rec.isRecording,
                        seconds = rec.seconds,
                        level01 = rec.level01,
                        onStart = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onStop = {
                            MeetingRecorder.stop(context)
                            screen = MeetScreen.Processing
                        }
                    )

                    MeetScreen.Processing -> ProcessingIridescent(
                        stage = stage.ifBlank { "Processing" },
                        progress01 = progress,
                        onBackToRecord = { screen = MeetScreen.Record },
                        // ✅ NEW: 显示检查信息
                        debugWorkId = rec.lastWorkId,
                        debugTraceId = rec.lastTraceId,
                        debugState = wi?.state?.name.orEmpty()
                    )

                    MeetScreen.Text -> {
                        val minutes = remember(rawResult) { parseMeetingMinutes(rawResult.orEmpty()) }
                        MeetingMinutesResult(
                            minutes = minutes,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Meet Memo", minutes.toPlainText()))
                            },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, minutes.toPlainText())
                                }
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            },
                            onRecordAgain = {
                                rawResult = null
                                screen = MeetScreen.Record
                            }
                        )
                    }
                }
            }
        }
    }
}

/* --------------------------- 录音：深色沉浸 + 等离子球/脉冲环 --------------------------- */

@Composable
private fun RecordingImmersive(
    isRecording: Boolean,
    seconds: Long,
    level01: Float,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "plasma")
    val spin by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "spin"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.65f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    val t = "%02d:%02d".format(seconds / 60, seconds % 60)
    val level = level01.coerceIn(0f, 1f)

    var smooth by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(level) { smooth = smooth * 0.78f + level * 0.22f }

    val orbScale = 0.92f + smooth * 0.22f
    val glowAlpha = (0.18f + smooth * 0.42f) * breathe
    val ringSweep = 210f + smooth * 120f

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val star = Color(0xFF9BB7E2).copy(alpha = 0.06f)
            val step = 54f
            var y = 0f
            while (y < h) {
                var x = 0f
                while (x < w) {
                    drawCircle(star, radius = 1.2f, center = Offset(x + (y % 3) * 2, y))
                    x += step
                }
                y += step
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isRecording) "我在记录…" else "点击开始录音",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE8EEF8)
            )
            Spacer(Modifier.height(6.dp))

            Spacer(Modifier.height(22.dp))

            Box(
                modifier = Modifier.size(320.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val c = center
                    val baseR = size.minDimension * 0.26f
                    val r = baseR * orbScale

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6EE7FF).copy(alpha = glowAlpha * 0.35f),
                                Color(0xFFB14CFF).copy(alpha = glowAlpha * 0.22f),
                                Color.Transparent
                            ),
                            center = c,
                            radius = r * 2.4f
                        ),
                        radius = r * 2.4f,
                        center = c
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB14CFF).copy(alpha = 0.38f + smooth * 0.20f),
                                Color(0xFF2B6CFF).copy(alpha = 0.24f + smooth * 0.18f),
                                Color(0xFF070A10)
                            ),
                            center = Offset(c.x - r * 0.15f, c.y - r * 0.18f),
                            radius = r * 1.25f
                        ),
                        radius = r,
                        center = c
                    )

                    val stroke = (10f + smooth * 8f)
                    val pad = stroke / 2f + 18f
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val topLeft = Offset(pad, pad)

                    drawArc(
                        color = Color(0xFF6EE7FF).copy(alpha = 0.65f),
                        startAngle = spin - 90f,
                        sweepAngle = ringSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    val lines = 6
                    for (i in 0 until lines) {
                        val a = (spin + i * (360f / lines)) * (PI / 180f).toFloat()
                        val x = c.x + cos(a) * r * (0.55f + 0.25f * smooth)
                        val y = c.y + sin(a) * r * (0.55f + 0.25f * smooth)
                        drawLine(
                            color = Color(0xFFB14CFF).copy(alpha = 0.35f + smooth * 0.25f),
                            start = c,
                            end = Offset(x, y),
                            strokeWidth = 2.2f + smooth * 2.2f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0E1422).copy(alpha = 0.92f),
                    modifier = Modifier.size(160.dp).shadow(18.dp, CircleShape).clip(CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                    }
                }

                Box(
                    modifier = Modifier.matchParentSize().padding(74.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { if (isRecording) onStop() else onStart() },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6EE7FF).copy(alpha = 0.14f),
                            contentColor = Color(0xFFE8EEF8)
                        )
                    ) { Text(if (isRecording) "Stop" else "Start") }
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0E1422).copy(alpha = 0.82f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotAlpha = (0.35f + smooth * 0.55f).coerceIn(0f, 1f)
                    Box(
                        Modifier.size(10.dp).clip(CircleShape)
                            .background(Color(0xFF6EE7FF).copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isRecording) "Recording" else "Idle",
                        color = Color(0xFFAEC3E8),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = t,
                        color = Color(0xFFE8EEF8),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/* --------------------------- 处理中：流光溢彩 AI 思考/提取动效 --------------------------- */

@Composable
private fun ProcessingIridescent(
    stage: String,
    progress01: Float,
    onBackToRecord: () -> Unit,
    debugWorkId: String?,
    debugTraceId: String?,
    debugState: String
) {
    val infinite = rememberInfiniteTransition(label = "ai")
    val sweep by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val p = progress01.coerceIn(0f, 1f)
    val bg = Brush.linearGradient(
        colors = listOf(
            Color(0xFF081021),
            Color(0xFF101A38),
            Color(0xFF2A1150),
            Color(0xFF071018)
        ),
        start = Offset(0f, 0f),
        end = Offset(1200f, 1200f)
    )

    Box(Modifier.fillMaxSize().background(bg)) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val x = w * sweep
            val bandW = w * 0.22f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF6EE7FF).copy(alpha = 0.10f * pulse),
                        Color(0xFFB14CFF).copy(alpha = 0.12f * pulse),
                        Color.Transparent
                    ),
                    start = Offset(x - bandW, 0f),
                    end = Offset(x + bandW, h)
                )
            )

            val dot = Color(0xFFAEC3E2).copy(alpha = 0.05f)
            val step = 48f
            var yy = 0f
            while (yy < h) {
                var xx = 0f
                while (xx < w) {
                    drawCircle(dot, radius = 1.2f, center = Offset(xx + (yy % 4) * 2, yy))
                    xx += step
                }
                yy += step
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "AI 正在思考与提取…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE8EEF8)
            )
            Spacer(Modifier.height(8.dp))
            Text(stage, color = Color(0xFFAEC3E8))

            Spacer(Modifier.height(26.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0E1422).copy(alpha = 0.62f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
                        color = Color(0xFF6EE7FF),
                        trackColor = Color(0xFF22304D)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${(p * 100).toInt()}%",
                        color = Color(0xFFAEC3E8),
                        style = MaterialTheme.typography.labelLarge
                    )

                    // ✅ 关键：检查信息（证明“确实入队并执行/已完成/失败”）
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "workId=${debugWorkId ?: "—"}",
                        color = Color(0xFF9FB4D9),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "traceId=${debugTraceId ?: "—"}  state=${if (debugState.isBlank()) "—" else debugState}",
                        color = Color(0xFF9FB4D9),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBackToRecord) { Text("返回录音", color = Color(0xFFAEC3E8)) }
            Spacer(Modifier.height(10.dp))
        }
    }
}


private data class MeetingMinutes(
    val summary: String = "",
    val points: List<String> = emptyList(),
    val todos: List<String> = emptyList()
) {
    fun toPlainText(): String = buildString {
        appendLine("会议纪要")
        appendLine()
        appendLine("摘要：")
        appendLine(summary.ifBlank { "（无）" })
        appendLine()
        appendLine("核心要点：")
        if (points.isEmpty()) appendLine("（无）") else points.forEach { appendLine("- $it") }
        appendLine()
        appendLine("待办事项：")
        if (todos.isEmpty()) appendLine("（无）") else todos.forEach { appendLine("- $it") }
    }
}

private fun parseMeetingMinutes(raw: String): MeetingMinutes {
    val t = raw.trim()
    if (t.isBlank()) return MeetingMinutes()

    // 1) JSON 优先（后端若直接返回 dict/JSON）
    runCatching {
        val jo = JSONObject(t)
        val summary = jo.optString("summary").ifBlank {
            jo.optString("摘要").ifBlank { jo.optString("minutes_summary") }
        }
        val points = extractStringList(jo, "points", "core_points", "核心要点", "key_points")
        val todos = extractStringList(jo, "todos", "待办事项", "todo", "action_items")
        return MeetingMinutes(summary = summary, points = points, todos = todos)
    }

    // 2) 纯文本：简单启发式解析（兼容“摘要/要点/待办”段落）
    val lines = t.lines().map { it.trim() }.filter { it.isNotBlank() }

    fun stripBullet(s: String): String =
        s.removePrefix("-").removePrefix("•").removePrefix("·").trim()

    var summary = ""
    val points = mutableListOf<String>()
    val todos = mutableListOf<String>()

    var mode: String? = null
    val sb = StringBuilder()

    for (ln in lines) {
        val lower = ln.lowercase()
        when {
            ln.contains("摘要") || lower.startsWith("summary") -> {
                if (mode == "summary" && sb.isNotBlank() && summary.isBlank()) summary = sb.toString().trim()
                mode = "summary"
                sb.clear()
                val after = ln.substringAfter("摘要", "").trim().trimStart('：', ':').trim()
                if (after.isNotBlank()) sb.append(after)
            }
            ln.contains("核心要点") || ln.contains("要点") || lower.startsWith("key points") -> {
                if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()
                mode = "points"
            }
            ln.contains("待办") || lower.startsWith("action items") || lower.startsWith("todo") -> {
                if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()
                mode = "todos"
            }
            else -> {
                when (mode) {
                    "summary" -> {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(ln)
                    }
                    "points" -> {
                        val s = stripBullet(ln)
                        if (s.isNotBlank()) points += s
                    }
                    "todos" -> {
                        val s = stripBullet(ln)
                        if (s.isNotBlank()) todos += s
                    }
                    else -> {
                        // 没有段落标记时：把前几行当 summary，其余当 points
                        if (summary.isBlank()) {
                            summary = ln
                        } else {
                            points += stripBullet(ln)
                        }
                    }
                }
            }
        }
    }
    if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()

    return MeetingMinutes(
        summary = summary,
        points = points.distinct(),
        todos = todos.distinct()
    )
}

private fun extractStringList(jo: JSONObject, vararg keys: String): List<String> {
    for (k in keys) {
        if (!jo.has(k)) continue
        val v = jo.get(k)
        when (v) {
            is JSONArray -> {
                val out = mutableListOf<String>()
                for (i in 0 until v.length()) out += v.optString(i)
                return out.filter { it.isNotBlank() }
            }
            is String -> {
                val lines = v.lines().map { it.trim() }.filter { it.isNotBlank() }
                return lines.map { it.removePrefix("-").removePrefix("•").trim() }
            }
        }
    }
    return emptyList()
}

@Composable
private fun MeetingMinutesResult(
    minutes: MeetingMinutes,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRecordAgain: () -> Unit
) {
    val bg = Brush.verticalGradient(
        0f to Color(0xFF070A10),
        1f to Color(0xFF0D1630)
    )
    Column(
        modifier = Modifier.fillMaxSize().background(bg).padding(18.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            "会议纪要",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFE8EEF8)
        )
        Spacer(Modifier.height(12.dp))

        StructuredCard(title = "摘要", content = minutes.summary.ifBlank { "（无）" })

        Spacer(Modifier.height(10.dp))
        StructuredListCard(title = "核心要点", items = minutes.points)

        Spacer(Modifier.height(10.dp))
        TodoCard(title = "待办事项", items = minutes.todos)

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCopy, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE8EEF8))
            ) {
                Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy")
            }
            OutlinedButton(
                onClick = onShare, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE8EEF8))
            ) {
                Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text("Share")
            }
        }

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onRecordAgain,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6EE7FF).copy(alpha = 0.16f),
                contentColor = Color(0xFFE8EEF8)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("继续录音")
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun StructuredCard(title: String, content: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E1422).copy(alpha = 0.78f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFFAEC3E8), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(content, color = Color(0xFFE8EEF8), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StructuredListCard(title: String, items: List<String>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E1422).copy(alpha = 0.78f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFFAEC3E8), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            if (items.isEmpty()) {
                Text("（无）", color = Color(0xFFE8EEF8))
            } else {
                items.forEachIndexed { idx, s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "•",
                            color = Color(0xFF6EE7FF),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            s,
                            color = Color(0xFFE8EEF8),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (idx != items.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TodoCard(title: String, items: List<String>) {
    // 本地勾选状态（不影响后端与其他功能）
    val checked = remember(items) { items.map { mutableStateOf(false) } }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E1422).copy(alpha = 0.78f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFFAEC3E8), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))

            if (items.isEmpty()) {
                Text("（无）", color = Color(0xFFE8EEF8))
            } else {
                items.forEachIndexed { idx, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = checked[idx].value,
                            onCheckedChange = { checked[idx].value = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF6EE7FF),
                                uncheckedColor = Color(0xFFAEC3E8),
                                checkmarkColor = Color(0xFF070A10)
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            s,
                            color = Color(0xFFE8EEF8),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

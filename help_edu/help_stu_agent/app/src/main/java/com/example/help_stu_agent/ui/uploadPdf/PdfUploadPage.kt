package com.example.help_stu_agent.ui.uploadPdf

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfUploadPage(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }

    val workInfos by remember {
        wm.getWorkInfosByTagFlow(PdfWorkQueue.TAG)
    }.collectAsState(initial = emptyList())

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult

        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        val rawNames = uris.map { uri -> queryDisplayName(context, uri) ?: "PDF" }
        val dedupNames = dedupFileNames(rawNames)

        val items = uris.mapIndexed { idx, uri ->
            uri.toString() to dedupNames[idx]
        }

        PdfWorkQueue.enqueueBatch(context, items)
    }

    // ========== UI tokens ==========
    val c = MaterialTheme.colorScheme

    val bgBrush = remember(c.background, c.surfaceVariant) {
        Brush.verticalGradient(
            0f to c.background,
            1f to c.surfaceVariant.copy(alpha = 0.55f)
        )
    }

    val orbFillBrush = remember(c.primaryContainer, c.surface) {
        Brush.radialGradient(
            colors = listOf(c.primaryContainer, c.surface.copy(alpha = 0.92f))
        )
    }

    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF 导入") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(padding)
        ) {
            SubtleDotBackdrop(
                modifier = Modifier.matchParentSize(),
                dotAlpha = 0.06f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "批量导入课程/文献 PDF",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "上传后可返回主界面，后台会继续静默处理",
                    fontSize = 12.sp,
                    color = c.onBackground.copy(alpha = 0.65f)
                )

                Spacer(Modifier.height(22.dp))

                // ========== 中央圆形按钮 ==========
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .shadow(18.dp, CircleShape)
                        .clip(CircleShape)
                        .background(orbFillBrush)
                        .clickable { pickMultipleLauncher.launch(arrayOf("application/pdf")) },
                    contentAlignment = Alignment.Center
                ) {
                    // 呼吸光晕
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.10f * breathe))
                    )

                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = "Upload",
                        tint = c.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                // ========== 队列列表 ==========
                Text(
                    text = "任务队列",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    color = c.onBackground
                )
                Spacer(Modifier.height(10.dp))

                if (workInfos.isEmpty()) {
                    EmptyQueueHint()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = workInfos.sortedBy { it.state.ordinal },
                            key = { it.id }
                        ) { wi ->
                            QueueItemCard(
                                wi = wi,
                                onCancel = { wm.cancelWorkById(wi.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(
    wi: WorkInfo,
    onCancel: () -> Unit
) {
    val c = MaterialTheme.colorScheme

    val name =
        wi.progress.getString(PdfProcessWorker.KEY_NAME)
            ?: wi.outputData.getString(PdfProcessWorker.OUT_NAME)
            ?: "PDF"


    val stage =
        wi.progress.getString(PdfProcessWorker.KEY_STAGE)
            ?: when (wi.state) {
                WorkInfo.State.SUCCEEDED -> "Done"
                WorkInfo.State.FAILED -> "Error"
                WorkInfo.State.CANCELLED -> "Cancelled"
                else -> "Queued"
            }

    val p =
        wi.progress.getFloat(PdfProcessWorker.KEY_PROGRESS, 0f)
            .coerceIn(0f, 1f)

    val status =
        wi.progress.getString(PdfProcessWorker.KEY_STATUS)
            ?: stage

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressBubble(progress = if (wi.state == WorkInfo.State.RUNNING) p else if (wi.state == WorkInfo.State.SUCCEEDED) 1f else p)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = c.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = status,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = c.onSurface.copy(alpha = 0.60f)
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (wi.state == WorkInfo.State.SUCCEEDED) 1f else p },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "State: ${wi.state.name.lowercase(Locale.getDefault())} · Stage: $stage",
                    fontSize = 11.sp,
                    color = c.onSurface.copy(alpha = 0.55f)
                )
            }

            Spacer(Modifier.width(10.dp))

            // 删除/取消键移动到队列项里
            IconButton(
                onClick = onCancel,
                enabled = wi.state == WorkInfo.State.ENQUEUED || wi.state == WorkInfo.State.RUNNING
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    tint = if (wi.state == WorkInfo.State.ENQUEUED || wi.state == WorkInfo.State.RUNNING)
                        c.error else c.onSurface.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun ProgressBubble(progress: Float) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 5.dp.toPx()
            val r = (min(size.width, size.height) - stroke) / 2f
            // track
            drawCircle(
                color = c.outlineVariant.copy(alpha = 0.35f),
                radius = r,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // progress
            drawArc(
                color = c.primary,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 10.sp,
            color = c.onSurface.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun EmptyQueueHint() {
    val c = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = c.surface.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("暂无任务", fontWeight = FontWeight.SemiBold, color = c.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                "点击上方圆形按钮批量选择 PDF，任务会加入队列并后台处理。",
                fontSize = 12.sp,
                color = c.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun SubtleDotBackdrop(
    modifier: Modifier = Modifier,
    dotAlpha: Float = 0.06f
) {
    val c = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val step = 44.dp.toPx()
        val r = 1.2.dp.toPx()
        val col = c.onSurface.copy(alpha = dotAlpha)
        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawCircle(col, r, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

// --------- helpers ---------

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? {
    val cr = context.contentResolver
    var cursor: Cursor? = null
    return try {
        cursor = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    } catch (_: Throwable) {
        null
    } finally {
        cursor?.close()
    }
}

/**
 * 同名文件去重：a.pdf, a.pdf, a.pdf -> a.pdf, a (2).pdf, a (3).pdf
 */
private fun dedupFileNames(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { n ->
        val key = n.trim().ifBlank { "PDF" }
        val count = (seen[key] ?: 0) + 1
        seen[key] = count
        if (count == 1) key else addSuffixBeforeExt(key, " ($count)")
    }
}

private fun addSuffixBeforeExt(name: String, suffix: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot > 0 && dot < name.length - 1) {
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        base + suffix + ext
    } else {
        name + suffix
    }
}

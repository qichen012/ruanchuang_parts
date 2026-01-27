@file:Suppress("unused")

package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.example.help_stu_agent.ui.theme.*
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import androidx.documentfile.provider.DocumentFile


enum class PdfStage { Idle, Uploading, Uploaded, Processing, Done, Error }

@Stable
private data class AppPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val accent2: Color,
    val accentGlow: Color
)

@Composable
private fun rememberAppPalette(): AppPalette {
    val dark = isSystemInDarkTheme()
    return remember(dark) {
        if (!dark) {
            AppPalette(
                primary = Primary,
                onPrimary = OnPrimary,
                primaryContainer = PrimaryContainer,
                onPrimaryContainer = OnPrimaryContainer,
                background = Background,
                onBackground = OnBackground,
                surface = Surface,
                onSurface = OnSurface,
                surfaceVariant = SurfaceVariant,
                onSurfaceVariant = OnSurfaceVariant,
                outline = Outline,
                outlineVariant = OutlineVariant,
                accent2 = BranchPaletteLight.getOrNull(1) ?: Primary,
                accentGlow = Primary.copy(alpha = 0.18f),
            )
        } else {
            AppPalette(
                primary = PrimaryDark,
                onPrimary = OnPrimaryDark,
                primaryContainer = PrimaryContainerDark,
                onPrimaryContainer = OnPrimaryContainerDark,
                background = BackgroundDark,
                onBackground = OnBackgroundDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                surfaceVariant = SurfaceVariantDark,
                onSurfaceVariant = OnSurfaceVariantDark,
                outline = OutlineDark,
                outlineVariant = OutlineVariantDark,
                accent2 = BranchPaletteDark.getOrNull(1) ?: PrimaryDark,
                accentGlow = PrimaryDark.copy(alpha = 0.18f),
            )
        }
    }
}

private data class QueueItemUi(
    val id: UUID,
    val name: String,
    val uriStr: String,
    val state: WorkInfo.State,
    val stageText: String,
    val statusText: String,
    val progress01: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfUploadPage(
    modifier: Modifier = Modifier,
    onGoToKnowledgeTree: () -> Unit,
    simulateFlow: Boolean = true,
) {
    val c = rememberAppPalette()
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }

    var selectedWorkId by remember { mutableStateOf<UUID?>(null) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf<String?>(null) }

    var stage by remember { mutableStateOf(PdfStage.Idle) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("上传 PDF，开始构建知识树") }

    val workInfos by wm.getWorkInfosByTagLiveData(PdfWorkQueue.TAG)
        .observeAsState(initial = emptyList())

    val queueUi = remember(workInfos) {
        workInfos
            .map { wi ->
                val name = wi.progress.getString(PdfProcessWorker.KEY_NAME) ?: "PDF"
                val uriStr = wi.progress.getString(PdfProcessWorker.KEY_URI).orEmpty()
                val stg = wi.progress.getString(PdfProcessWorker.KEY_STAGE) ?: ""
                val stat = wi.progress.getString(PdfProcessWorker.KEY_STATUS) ?: ""

                val p = wi.progress.getFloat(PdfProcessWorker.KEY_PROGRESS, -1f)
                val progress01 = when {
                    p >= 0f -> p.coerceIn(0f, 1f)
                    wi.state == WorkInfo.State.SUCCEEDED -> 1f
                    wi.state == WorkInfo.State.FAILED || wi.state == WorkInfo.State.CANCELLED -> 0f
                    else -> 0f
                }

                QueueItemUi(
                    id = wi.id,
                    name = name,
                    uriStr = uriStr,
                    state = wi.state,
                    stageText = stg.ifBlank {
                        when (wi.state) {
                            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> "Queued"
                            WorkInfo.State.RUNNING -> "Running"
                            WorkInfo.State.SUCCEEDED -> "Done"
                            WorkInfo.State.FAILED -> "Error"
                            WorkInfo.State.CANCELLED -> "Cancelled"
                        }
                    },
                    statusText = stat.ifBlank {
                        when (wi.state) {
                            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> "排队中…"
                            WorkInfo.State.RUNNING -> "处理中…"
                            WorkInfo.State.SUCCEEDED -> "完成"
                            WorkInfo.State.FAILED -> "失败"
                            WorkInfo.State.CANCELLED -> "已取消"
                        }
                    },
                    progress01 = progress01
                )
            }
            .sortedWith(compareBy<QueueItemUi> {
                when (it.state) {
                    WorkInfo.State.RUNNING -> 0
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> 1
                    WorkInfo.State.SUCCEEDED -> 2
                    WorkInfo.State.FAILED -> 3
                    WorkInfo.State.CANCELLED -> 4
                }
            }.thenBy { it.name })
    }

    fun resetAllUiOnly() {
        pdfUri = null
        pdfName = null
        selectedWorkId = null
        stage = PdfStage.Idle
        progress = 0f
        statusText = "上传 PDF，开始构建知识树"
        PdfTreeCache.latestJson = null
        PdfTreeCache.latestJobId = null
    }

    LaunchedEffect(queueUi, selectedWorkId) {
        val selected = selectedWorkId?.let { id -> queueUi.firstOrNull { it.id == id } }
        val driving = selected ?: queueUi.firstOrNull()

        if (driving == null) {
            resetAllUiOnly()
            return@LaunchedEffect
        }

        pdfName = driving.name
        pdfUri = driving.uriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

        stage = when (driving.state) {
            WorkInfo.State.RUNNING -> {
                if (driving.stageText.contains("Upload", ignoreCase = true)) PdfStage.Uploading else PdfStage.Processing
            }
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> PdfStage.Uploaded
            WorkInfo.State.SUCCEEDED -> PdfStage.Done
            WorkInfo.State.FAILED -> PdfStage.Error
            WorkInfo.State.CANCELLED -> PdfStage.Error
        }

        progress = driving.progress01
        statusText = driving.statusText
    }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult

        val raw = uris.mapIndexed { idx, uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val n1 = queryDisplayName(context, uri)
            val n2 = DocumentFile.fromSingleUri(context, uri)?.name
            val fallback = "PDF_${System.currentTimeMillis()}_${idx + 1}.pdf"

            val baseName = (n1 ?: n2 ?: fallback).ifBlank { fallback }
            uri.toString() to baseName
        }

        val counts = linkedMapOf<String, Int>()
        val items = raw.map { (uriStr, baseName) ->
            val n = (counts[baseName] ?: 0) + 1
            counts[baseName] = n

            val uniqueName = if (n == 1) {
                baseName
            } else {
                val dot = baseName.lastIndexOf('.')
                if (dot > 0) {
                    val stem = baseName.substring(0, dot)
                    val ext = baseName.substring(dot)
                    "$stem ($n)$ext"
                } else {
                    "$baseName ($n)"
                }
            }

            uriStr to uniqueName
        }



        val ids = PdfWorkQueue.enqueueBatch(context, items)
        selectedWorkId = ids.lastOrNull()

        val last = items.lastOrNull()
        pdfUri = last?.first?.let { Uri.parse(it) }
        pdfName = last?.second

        stage = PdfStage.Uploaded
        progress = 0f
        statusText = "已加入队列：${pdfName ?: "PDF"}"
    }

    val canPick = true

    val isBusy = stage == PdfStage.Uploading || stage == PdfStage.Processing

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(240, easing = LinearOutSlowInEasing),
        label = "progress"
    )

    val bgBrush = remember(c.background, c.surfaceVariant) {
        Brush.verticalGradient(
            0f to c.background, 1f to c.surfaceVariant.copy(alpha = 0.55f)
        )
    }

    val orbFillBrush = remember(c.primaryContainer, c.surface) {
        Brush.radialGradient(
            colors = listOf(
                c.primaryContainer, c.surface.copy(alpha = 0.92f)
            )
        )
    }

    val ringTrackColor = c.outlineVariant.copy(alpha = 0.40f)
    val ringProgressColor = c.primary
    val ringHeadColor = c.accent2

    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0.35f, targetValue = 0.85f, animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "breatheAlpha"
    )

    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing), repeatMode = RepeatMode.Restart
        ), label = "spinAngle"
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PDF 导入") })
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(padding)
        ) {
            SubtleDotBackdrop(
                modifier = Modifier.matchParentSize(),
                dotColor = c.onSurface.copy(alpha = 0.06f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(26.dp))

                Text(
                    text = "导入课程/文献 PDF",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "自动解析目录、生成知识树卡片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )

                Spacer(Modifier.height(26.dp))

                Box(
                    modifier = Modifier.size(290.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RingProgress(
                        modifier = Modifier.fillMaxSize(),
                        progress = animatedProgress,
                        showDeterminate = isBusy || stage == PdfStage.Done || stage == PdfStage.Uploaded,
                        indeterminateStartAngle = spin,
                        trackColor = ringTrackColor,
                        progressColor = ringProgressColor,
                        headColor = ringHeadColor,
                        idleGlowAlpha = if (stage == PdfStage.Idle) breathe else 0.0f
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val r = size.minDimension * 0.32f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(c.accentGlow, Color.Transparent),
                                center = center,
                                radius = r * 2.2f
                            ),
                            radius = r * 2.2f,
                            center = center
                        )
                    }

                    OrbUploadButton(
                        enabled = canPick,
                        stage = stage,
                        fileName = pdfName,
                        fillBrush = orbFillBrush,
                        onPrimaryText = c.onPrimaryContainer,
                        modifier = Modifier.size(178.dp),
                        onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) }
                    )

                    this@Column.AnimatedVisibility(
                        visible = stage == PdfStage.Uploaded || stage == PdfStage.Processing || stage == PdfStage.Done,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        val checkScale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "checkScale"
                        )
                        val checkAlpha by animateFloatAsState(
                            targetValue = 1f, animationSpec = tween(180), label = "checkAlpha"
                        )
                        Surface(
                            shape = CircleShape,
                            color = c.primaryContainer,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(38.dp)
                                .graphicsLayer {
                                    alpha = checkAlpha
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = c.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(visible = pdfUri != null) {
                    FileInfoCard(
                        palette = c,
                        fileName = pdfName ?: "PDF 文件",
                        stage = stage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )

                if (isBusy) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface
                    )
                }

                Spacer(Modifier.height(18.dp))


                if (queueUi.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    QueueListCard(
                        palette = c,
                        items = queueUi,
                        selectedId = selectedWorkId,
                        onSelect = { item ->
                            selectedWorkId = item.id
                            pdfName = item.name
                            pdfUri = item.uriStr.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                            statusText = item.statusText
                            progress = item.progress01
                        },
                        onDeleteOne = { item ->
                            // 删除=取消该任务（如果已完成，取消无害）
                            wm.cancelWorkById(item.id)
                            if (selectedWorkId == item.id) selectedWorkId = null
                        },
                        onClearAll = {
                            wm.cancelAllWorkByTag(PdfWorkQueue.TAG)
                            resetAllUiOnly()
                        },
                        onPruneFinished = {
                            // 清理已完成/失败/取消的任务记录（WorkManager 行为：清空 finished works）
                            wm.pruneWork()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QueueListCard(
    palette: AppPalette,
    items: List<QueueItemUi>,
    selectedId: UUID?,
    onSelect: (QueueItemUi) -> Unit,
    onDeleteOne: (QueueItemUi) -> Unit,
    onClearAll: () -> Unit,
    onPruneFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = palette.surface.copy(alpha = 0.82f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "处理队列",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.onSurface
                )
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = { Text("${items.size} 个任务") }
                )
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = onPruneFinished) { Text("清理完成") }
                TextButton(onClick = onClearAll) { Text("清空队列") }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { it ->
                    val isSelected = (selectedId == it.id)
                    QueueRow(
                        palette = palette,
                        item = it,
                        selected = isSelected,
                        onClick = { onSelect(it) },
                        onDelete = { onDeleteOne(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    palette: AppPalette,
    item: QueueItemUi,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val border = if (selected) palette.primary.copy(alpha = 0.55f) else palette.outlineVariant.copy(alpha = 0.40f)
    val bg = if (selected) palette.primaryContainer.copy(alpha = 0.35f) else palette.surface.copy(alpha = 0.65f)

    val deletable = item.state == WorkInfo.State.ENQUEUED ||
            item.state == WorkInfo.State.BLOCKED ||
            item.state == WorkInfo.State.RUNNING ||
            item.state == WorkInfo.State.FAILED ||
            item.state == WorkInfo.State.CANCELLED ||
            item.state == WorkInfo.State.SUCCEEDED

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = palette.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))

                AssistChip(
                    onClick = { /* no-op */ },
                    enabled = false,
                    label = { Text(item.stageText.take(12)) }
                )

                if (deletable) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.surface.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除/取消任务",
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = item.statusText,
                style = MaterialTheme.typography.labelMedium,
                color = palette.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            val p = item.progress01.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = palette.primary,
                trackColor = palette.outlineVariant.copy(alpha = 0.35f)
            )

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(p * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .height(1.dp)
                        .width(0.dp)
                        .background(border)
                )
            }
        }
    }
}

@Composable
private fun OrbUploadButton(
    enabled: Boolean,
    stage: PdfStage,
    fileName: String?,
    fillBrush: Brush,
    onPrimaryText: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val label = when (stage) {
        PdfStage.Idle, PdfStage.Error -> "上传 PDF"
        PdfStage.Uploading -> "上传中"
        PdfStage.Uploaded -> "已入队"
        PdfStage.Processing -> "处理中"
        PdfStage.Done -> "已完成"
    }
    val icon = when (stage) {
        PdfStage.Idle, PdfStage.Error -> Icons.Default.UploadFile
        else -> Icons.Default.PictureAsPdf
    }

    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .shadow(18.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fillBrush),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.28f, size.height * 0.22f),
                        radius = size.minDimension * 0.55f
                    )
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = onPrimaryText,
                    modifier = Modifier.size(46.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = onPrimaryText
                )

                AnimatedContent(
                    targetState = (fileName != null && stage != PdfStage.Idle),
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                    label = "fileHint"
                ) { show ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (show) (fileName ?: "已选择 PDF").take(18) else "点击选择文件",
                        style = MaterialTheme.typography.labelMedium,
                        color = onPrimaryText.copy(alpha = if (show) 0.85f else 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RingProgress(
    modifier: Modifier = Modifier,
    progress: Float,
    showDeterminate: Boolean,
    indeterminateStartAngle: Float,
    trackColor: Color,
    progressColor: Color,
    headColor: Color,
    idleGlowAlpha: Float
) {
    val p = progress.coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val stroke = 12.dp.toPx()
        val pad = stroke / 2f + 8.dp.toPx()
        val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
        val topLeft = Offset(pad, pad)

        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        if (!showDeterminate && idleGlowAlpha > 0f) {
            drawArc(
                color = progressColor.copy(alpha = (0.18f * idleGlowAlpha).coerceIn(0f, 0.22f)),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        if (showDeterminate) {
            val sweep = 360f * p
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (p in 0.02f..0.98f) {
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val rx = arcSize.width / 2.0
                val ry = arcSize.height / 2.0
                val cx = topLeft.x + arcSize.width / 2f
                val cy = topLeft.y + arcSize.height / 2f
                val x = (cx + rx * cos(angle)).toFloat()
                val y = (cy + ry * sin(angle)).toFloat()

                drawCircle(
                    color = headColor,
                    radius = stroke * 0.36f,
                    center = Offset(x, y)
                )
            }
        } else {
            drawArc(
                color = progressColor,
                startAngle = indeterminateStartAngle - 90f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun FileInfoCard(
    palette: AppPalette,
    fileName: String,
    stage: PdfStage,
    modifier: Modifier = Modifier
) {
    val stageText = when (stage) {
        PdfStage.Uploading -> "Uploading"
        PdfStage.Uploaded -> "Queued"
        PdfStage.Processing -> "Processing"
        PdfStage.Done -> "Done"
        PdfStage.Error -> "Error"
        PdfStage.Idle -> "Idle"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = palette.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "已选择文件",
                style = MaterialTheme.typography.labelLarge,
                color = palette.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = palette.onSurface
            )
            Spacer(Modifier.height(10.dp))
            AssistChip(
                onClick = { /* no-op */ },
                label = { Text(stageText) },
                enabled = false
            )
        }
    }
}

@Composable
private fun SubtleDotBackdrop(
    modifier: Modifier = Modifier,
    dotColor: Color
) {
    Canvas(modifier = modifier) {
        val step = 56.dp.toPx()
        val r = 1.2.dp.toPx()
        var y = 0f
        while (y <= size.height) {
            var x = 0f
            while (x <= size.width) {
                drawCircle(dotColor, radius = r, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
        }
    }.getOrNull()
}

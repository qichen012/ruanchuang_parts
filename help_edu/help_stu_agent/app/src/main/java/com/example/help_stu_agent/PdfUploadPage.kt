@file:Suppress("unused")

package com.example.help_stu_agent

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.help_stu_agent.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin


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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfUploadPage(
    modifier: Modifier = Modifier,
    onGoToKnowledgeTree: () -> Unit,
    simulateFlow: Boolean = true, // 你接入真实处理时设为 false，并用真实进度驱动 stage/progress
) {
    val c = rememberAppPalette()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf<String?>(null) }

    var stage by remember { mutableStateOf(PdfStage.Idle) }
    var progress by remember { mutableFloatStateOf(0f) } // 0..1
    var statusText by remember { mutableStateOf("上传 PDF，开始构建知识树") }

    val canPick = stage == PdfStage.Idle || stage == PdfStage.Error
    val isBusy = stage == PdfStage.Uploading || stage == PdfStage.Processing
    val canDelete = pdfUri != null && !isBusy
    val showGo = stage == PdfStage.Done

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(240, easing = LinearOutSlowInEasing),
        label = "progress"
    )

    fun resetAll() {
        pdfUri = null
        pdfName = null
        stage = PdfStage.Idle
        progress = 0f
        statusText = "上传 PDF，开始构建知识树"
        PdfTreeCache.latestJson = null
        PdfTreeCache.latestJobId = null
    }

    // SAF 选择 PDF
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        pdfUri = uri
        pdfName = queryDisplayName(context, uri)

        if (!simulateFlow) {
            stage = PdfStage.Uploaded
            statusText = "已选择：${pdfName ?: "PDF"}"
            progress = 0f
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            try {
                // 运行整条后端管线（上传->处理->取json）
                PdfBackendPipeline.runPipeline(
                    context = context,
                    pdfUri = uri,
                    onUpdate = { up ->
                        stage = up.stage
                        progress = up.progress01
                        statusText = up.statusText
                    }
                )
                // 结果已在 PdfTreeCache.latestJson 中
                // stage/progress/statusText 已被更新为 Done
            } catch (e: Exception) {
                stage = PdfStage.Error
                progress = 0f
                statusText = "失败：${e.message ?: "unknown error"}"
            }
        }

    }

    // 背景渐变
    val bgBrush = remember(c.background, c.surfaceVariant) {
        Brush.verticalGradient(
            0f to c.background,
            1f to c.surfaceVariant.copy(alpha = 0.55f)
        )
    }

    // 中央圆按钮填充（玻璃拟态：径向渐变）
    val orbFillBrush = remember(c.primaryContainer, c.surface) {
        Brush.radialGradient(
            colors = listOf(
                c.primaryContainer,
                c.surface.copy(alpha = 0.92f)
            )
        )
    }

    // 进度环颜色（只用你颜色库）
    val ringTrackColor = c.outlineVariant.copy(alpha = 0.40f)
    val ringProgressColor = c.primary
    val ringHeadColor = c.accent2

    // idle 光晕呼吸
    val breathe by rememberInfiniteTransition(label = "breathe")
        .animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breatheAlpha"
        )

    // 旋转角（如果你以后想用 indeterminate）
    val spin by rememberInfiniteTransition(label = "spin")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spinAngle"
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF 导入") }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(padding)
        ) {
            // 背景点阵（增强设计感）
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
                    text = "自动解析目录、生成卡片并进入知识树",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant
                )

                Spacer(Modifier.height(26.dp))

                // 中央：进度环 + 悬浮圆按钮 + 删除按钮 + 完成勾
                Box(
                    modifier = Modifier.size(290.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RingProgress(
                        modifier = Modifier.fillMaxSize(),
                        progress = animatedProgress,
                        showDeterminate = isBusy || stage == PdfStage.Done,
                        indeterminateStartAngle = spin,
                        trackColor = ringTrackColor,
                        progressColor = ringProgressColor,
                        headColor = ringHeadColor,
                        idleGlowAlpha = if (stage == PdfStage.Idle) breathe else 0.0f
                    )

                    // 外层柔光
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

                    // 删除（右上）
                    androidx.compose.animation.AnimatedVisibility(visible = canDelete) {
                        IconButton(
                            onClick = { resetAll() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-6).dp, y = 8.dp)
                                .size(44.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(c.surface.copy(alpha = 0.92f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "删除 PDF",
                                tint = c.onSurfaceVariant
                            )
                        }
                    }

                    // 上传完成/处理中/完成：勾选徽标动效（右上）
                    androidx.compose.animation.AnimatedVisibility(
                        visible = stage == PdfStage.Uploaded || stage == PdfStage.Processing || stage == PdfStage.Done,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        val checkScale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "checkScale"
                        )
                        val checkAlpha by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(180),
                            label = "checkAlpha"
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

                // 文件信息卡
                androidx.compose.animation.AnimatedVisibility(visible = pdfUri != null) {
                    FileInfoCard(
                        palette = c,
                        fileName = pdfName ?: "PDF 文件",
                        stage = stage,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 状态 & 百分比
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

                // 完成后进入知识树
                androidx.compose.animation.AnimatedVisibility(visible = showGo) {
                    Button(
                        onClick = onGoToKnowledgeTree,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = c.primary,
                            contentColor = c.onPrimary
                        )
                    ) {
                        Text("进入知识树")
                    }
                }

                Spacer(Modifier.weight(1f))

            }
        }
    }
}

/* --------------------------------- UI building blocks --------------------------------- */

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
        PdfStage.Uploaded -> "已上传"
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
            // 玻璃高光（纯绘制，不取主题色）
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

        // track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // idle：呼吸光环（不显示实际进度）
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
            // determinate：从顶部开始
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

            // 头部高亮点（增强“设计感”）
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
            // indeterminate：旋转短弧（保留能力）
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
        PdfStage.Uploaded -> "Uploaded"
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

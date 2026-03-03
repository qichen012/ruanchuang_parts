package com.example.help_stu_agent.ui.uploadPdf

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager

@Composable
fun PdfUploadPage(
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }

    val workInfos by remember {
        wm.getWorkInfosByTagFlow(PdfWorkQueue.TAG)
    }.collectAsState(initial = emptyList())

    // 状态1：暂存选中的文件，不立刻执行
    var selectedFiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    // 状态2：是否显示沉浸式进度
    var showImmersiveProgress by remember { mutableStateOf(false) }

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

        val rawNames = uris.map { uri -> queryDisplayName(context, uri) ?: "PDF Document" }
        val dedupNames = dedupFileNames(rawNames)

        // 选中后暂存，触发动效
        selectedFiles = uris.mapIndexed { idx, uri ->
            uri.toString() to dedupNames[idx]
        }
    }

    // 获取当前正在处理的任务
    val activeWork = workInfos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    val isAnyRunning = activeWork != null

    // 如果任务全部完成，自动关闭沉浸式界面
    LaunchedEffect(isAnyRunning) {
        if (!isAnyRunning && showImmersiveProgress) {
            showImmersiveProgress = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FAFB))
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827))
                    }
                }
                Text(
                    text = "PDF Intelligence",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Analyze Knowledge",
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Our AI will extract key insights, summarize content, and answer your questions about the document.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(32.dp))

                UploadInteractiveArea(
                    hasSelection = selectedFiles.isNotEmpty(),
                    selectedCount = selectedFiles.size,
                    onClick = { pickMultipleLauncher.launch(arrayOf("application/pdf")) }
                )

                AnimatedVisibility(visible = selectedFiles.isNotEmpty()) {
                    Button(
                        onClick = {
                            // 正式提交给 WorkManager
                            PdfWorkQueue.enqueueBatch(context, selectedFiles)
                            selectedFiles = emptyList() // 清空选中状态
                            showImmersiveProgress = true // 展开沉浸式进度
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("Start Analysis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT INSIGHTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.clickable { /* TODO: View All Actions */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = workInfos.sortedByDescending { it.id }, // 最新在最上
                        key = { it.id }
                    ) { wi ->
                        InsightQueueCard(wi)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showImmersiveProgress,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            ImmersiveProgressOverlay(
                activeWork = activeWork,
                onMinimize = { showImmersiveProgress = false }
            )
        }
    }
}

@Composable
fun UploadInteractiveArea(
    hasSelection: Boolean,
    selectedCount: Int,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "upload_anim")

    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val tiltAngle by animateFloatAsState(
        targetValue = if (hasSelection) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f),
        label = "tilt"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 绘制虚线边框与扫描线
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(
                width = 4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 24f), 0f)
            )
            // 边框颜色（选中时带点绿色提示）
            val outlineColor = if (hasSelection) Color(0xFFA7F3D0) else Color(0xFFE5E7EB)
            drawRoundRect(
                color = outlineColor,
                style = stroke,
                cornerRadius = CornerRadius(80f, 80f) // 大圆角
            )

            // 如果有选中文件，绘制动态激光扫描线
            if (hasSelection) {
                val yPos = size.height * scanY
                val scanBrush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF10B981).copy(alpha = 0.3f), Color(0xFF10B981)),
                    startY = yPos - 40.dp.toPx(),
                    endY = yPos
                )
                drawRect(
                    brush = scanBrush,
                    topLeft = Offset(10.dp.toPx(), yPos - 40.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), 40.dp.toPx())
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(10.dp.toPx(), yPos),
                    end = Offset(size.width - 10.dp.toPx(), yPos),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // 内容区
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { rotationZ = tiltAngle }
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = if (hasSelection) Color(0xFF10B981) else Color.Black.copy(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (hasSelection) {
                        Box {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Selected",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(36.dp)
                            )
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Check",
                                tint = Color(0xFF10B981),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 4.dp, y = 4.dp)
                            )
                        }
                    } else {
                        // 空状态：向上的上传箭头
                        Icon(
                            imageVector = Icons.Outlined.Upload,
                            contentDescription = "Upload",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (hasSelection) "$selectedCount File(s) Ready" else "Upload your PDF here",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (hasSelection) "AI is scanning for preparation" else " tap to browse your library",
                fontSize = 14.sp,
                color = if (hasSelection) Color(0xFF059669) else Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
fun ImmersiveProgressOverlay(
    activeWork: WorkInfo?,
    onMinimize: () -> Unit
) {
    // 读取任务进度，若为空则设为 0
    val progress = activeWork?.progress?.getFloat(PdfProcessWorker.KEY_PROGRESS, 0f)?.coerceIn(0f, 1f) ?: 0f
    val statusText = activeWork?.progress?.getString(PdfProcessWorker.KEY_STATUS) ?: "Preparing Analysis..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB).copy(alpha = 0.96f)) // 极强烈的半透明遮罩
            .clickable(enabled = true) {} // 拦截点击事件
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 大圆环进度
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFE5E7EB),
                    strokeWidth = 8.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A),
                    strokeWidth = 8.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "AI is Processing",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusText,
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(80.dp))

            TextButton(onClick = onMinimize) {
                Text("Run in Background", color = Color(0xFF0D9488), fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun InsightQueueCard(wi: WorkInfo) {
    val name = wi.progress.getString(PdfProcessWorker.KEY_NAME)
        ?: wi.outputData.getString(PdfProcessWorker.OUT_NAME)
        ?: "PDF Document"

    // 判断状态并配置胶囊颜色 (完全按照设计图还原)
    val (statusText, bgColor, textColor) = when (wi.state) {
        WorkInfo.State.SUCCEEDED -> Triple("COMPLETED", Color(0xFFECFDF5), Color(0xFF059669))
        WorkInfo.State.FAILED -> Triple("ERROR", Color(0xFFFEF2F2), Color(0xFFDC2626))
        WorkInfo.State.CANCELLED -> Triple("CANCELLED", Color(0xFFF3F4F6), Color(0xFF6B7280))
        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> Triple("PROCESSING", Color(0xFFEFF6FF), Color(0xFF2563EB))
        else -> Triple("QUEUED", Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    // 默认显示昨天与大小的占位，真实中可以通过文件读取
    val subStatus = if (wi.state == WorkInfo.State.SUCCEEDED) "Yesterday • 2.4 MB" else "Processing now..."

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp // 细腻的悬停阴影感
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标底座 (设计图中的灰白圆角矩形)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subStatus,
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 状态胶囊 (COMPLETED 等)
            Box(
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// --------- Helpers (保持不变) ---------
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
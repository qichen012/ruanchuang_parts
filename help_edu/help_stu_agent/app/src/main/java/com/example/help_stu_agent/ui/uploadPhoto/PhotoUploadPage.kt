package com.example.help_stu_agent.ui.uploadPhoto

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SwapHoriz
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

enum class TaskState { QUEUED, PROCESSING, SUCCEEDED, FAILED }

data class PhotoAnalysisTask(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val uri: String,
    var state: TaskState = TaskState.QUEUED,
    var resultJson: String? = null
)

@Composable
fun UploadPhotoPage(
    onBack: (() -> Unit)? = null,
    onSwitchToPdf: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showImmersiveProgress by remember { mutableStateOf(false) }

    // 用于保存历史任务列表
    var taskHistory by remember { mutableStateOf<List<PhotoAnalysisTask>>(emptyList()) }

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val rawNames = uris.map { uri -> queryDisplayName(context, uri) ?: "Image_${System.currentTimeMillis()}.jpg" }
        selectedFiles = uris.mapIndexed { idx, uri -> uri.toString() to dedupFileNames(rawNames)[idx] }
    }

    val isAnyRunning = taskHistory.any { it.state == TaskState.PROCESSING || it.state == TaskState.QUEUED }

    LaunchedEffect(isAnyRunning) {
        if (!isAnyRunning && showImmersiveProgress) showImmersiveProgress = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F9FE)).statusBarsPadding()) {

            // ========== 顶部导航栏 ==========
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                    }
                }
                Text("Visual Intelligence", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

                IconButton(onClick = onSwitchToPdf, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "Switch to PDF", tint = Color(0xFF1E293B))
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                Text("Analyze Image", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Extract text, identify objects, and generate insights from your images using AI.", fontSize = 15.sp, lineHeight = 22.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(32.dp))

                UploadPhotoInteractiveArea(
                    hasSelection = selectedFiles.isNotEmpty(),
                    selectedCount = selectedFiles.size,
                    onClick = { pickMultipleLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )

                AnimatedVisibility(visible = selectedFiles.isNotEmpty()) {
                    Button(
                        onClick = {
                            val currentSelection = selectedFiles.toList()
                            selectedFiles = emptyList()
                            showImmersiveProgress = true

                            // 创建任务记录并加入列表头部
                            val newTasks = currentSelection.map { (uri, name) ->
                                PhotoAnalysisTask(name = name, uri = uri)
                            }
                            taskHistory = newTasks + taskHistory

                            // 启动协程进行网络请求
                            coroutineScope.launch {
                                newTasks.forEach { task ->
                                    // 更新状态为处理中
                                    taskHistory = taskHistory.map { if (it.id == task.id) it.copy(state = TaskState.PROCESSING) else it }

                                    val result = uploadScreenshot(context, task.uri, task.name)

                                    // 更新结果状态
                                    taskHistory = taskHistory.map {
                                        if (it.id == task.id) {
                                            if (result != null) it.copy(state = TaskState.SUCCEEDED, resultJson = result)
                                            else it.copy(state = TaskState.FAILED)
                                        } else it
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0xFF5D5FEF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D5FEF))
                    ) {
                        Text("Start Analysis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("RECENT INSIGHTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
                    Text("View All", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D5FEF), modifier = Modifier.clickable { })
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = taskHistory, key = { it.id }) { task ->
                        InsightPhotoQueueCard(task)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showImmersiveProgress, enter = fadeIn(tween(400)), exit = fadeOut(tween(400))) {
            ImmersiveProgressOverlay(
                isProcessing = isAnyRunning,
                onMinimize = { showImmersiveProgress = false }
            )
        }
    }
}

// ========== 网络请求逻辑 ==========
private suspend fun uploadScreenshot(context: Context, uriString: String, fileName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return@withContext null

            // 模拟器访问宿主机通常用 10.0.2.2
            val serverUrl = "http://10.29.142.138:8001/extract_screenshot_info"

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    bytes.toRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null // 可以在这里打印错误日志: Log.e("Upload", "Code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ========== 其余 UI 组件（适配了新的状态机） ==========

@Composable
fun UploadPhotoInteractiveArea(hasSelection: Boolean, selectedCount: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "upload_anim")
    val scanY by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scanLine")
    val tiltAngle by animateFloatAsState(targetValue = if (hasSelection) 8f else 0f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 100f), label = "tilt")

    Box(modifier = Modifier.fillMaxWidth().height(260.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 24f), 0f))
            val outlineColor = if (hasSelection) Color(0xFF818CF8).copy(alpha = 0.5f) else Color(0xFFCBD5E1)
            drawRoundRect(color = outlineColor, style = stroke, cornerRadius = CornerRadius(80f, 80f))
            if (hasSelection) {
                val yPos = size.height * scanY
                val scanBrush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF818CF8).copy(alpha = 0.2f), Color(0xFF5D5FEF)), startY = yPos - 40.dp.toPx(), endY = yPos)
                drawRect(brush = scanBrush, topLeft = Offset(10.dp.toPx(), yPos - 40.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width - 20.dp.toPx(), 40.dp.toPx()))
                drawLine(color = Color(0xFF5D5FEF), start = Offset(10.dp.toPx(), yPos), end = Offset(size.width - 10.dp.toPx(), yPos), strokeWidth = 2.dp.toPx())
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(80.dp).graphicsLayer { rotationZ = tiltAngle }.shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = if (hasSelection) Color(0xFF5D5FEF) else Color.Black.copy(alpha = 0.3f)), shape = RoundedCornerShape(24.dp), color = Color.White) {
                Box(contentAlignment = Alignment.Center) {
                    if (hasSelection) {
                        Box {
                            Icon(imageVector = Icons.Outlined.Image, contentDescription = "Selected", tint = Color(0xFF5D5FEF), modifier = Modifier.size(36.dp))
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Check", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
                        }
                    } else {
                        Icon(imageVector = Icons.Outlined.Upload, contentDescription = "Upload", tint = Color(0xFF94A3B8), modifier = Modifier.size(36.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = if (hasSelection) "$selectedCount Image(s) Ready" else "Upload your Image here", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = if (hasSelection) "AI is scanning for preparation" else "tap to open gallery", fontSize = 14.sp, color = if (hasSelection) Color(0xFF5D5FEF) else Color(0xFF64748B))
        }
    }
}

@Composable
fun ImmersiveProgressOverlay(isProcessing: Boolean, onMinimize: () -> Unit) {
    // 替换了原本 WorkManager 的具体进度，改为无限循环的加载动画
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F9FE).copy(alpha = 0.96f)).clickable(enabled = true) {}.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize(), color = Color(0xFFE2E8F0), strokeWidth = 8.dp)
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize(), color = Color(0xFF5D5FEF), strokeWidth = 8.dp, strokeCap = StrokeCap.Round)
                }
                Icon(imageVector = Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(text = "AI is Processing", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Extracting information from server...", fontSize = 16.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(80.dp))
            TextButton(onClick = onMinimize) { Text("Run in Background", color = Color(0xFF5D5FEF), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun InsightPhotoQueueCard(task: PhotoAnalysisTask) {
    val (statusText, bgColor, textColor) = when (task.state) {
        TaskState.SUCCEEDED -> Triple("COMPLETED", Color(0xFFECFDF5), Color(0xFF059669))
        TaskState.FAILED -> Triple("ERROR", Color(0xFFFEF2F2), Color(0xFFDC2626))
        TaskState.PROCESSING -> Triple("PROCESSING", Color(0xFFEEF2FF), Color(0xFF5D5FEF))
        TaskState.QUEUED -> Triple("QUEUED", Color(0xFFF8FAFC), Color(0xFF64748B))
    }
    val subStatus = if (task.state == TaskState.SUCCEEDED) "Data Extracted" else "Connecting to server..."

    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 2.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Outlined.Image, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subStatus, fontSize = 13.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.background(bgColor, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(text = statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ========== 文件处理辅助函数 ==========
private fun queryDisplayName(context: Context, uri: Uri): String? {
    val cr = context.contentResolver
    var cursor: Cursor? = null
    return try {
        cursor = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) cursor.getString(idx) else null
        } else null
    } catch (_: Throwable) { null } finally { cursor?.close() }
}

private fun dedupFileNames(names: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return names.map { n ->
        val key = n.trim().ifBlank { "Image" }
        val count = (seen[key] ?: 0) + 1
        seen[key] = count
        if (count == 1) key else addSuffixBeforeExt(key, " ($count)")
    }
}

private fun addSuffixBeforeExt(name: String, suffix: String): String {
    val dot = name.lastIndexOf('.')
    return if (dot > 0 && dot < name.length - 1) { name.substring(0, dot) + suffix + name.substring(dot) } else { name + suffix }
}
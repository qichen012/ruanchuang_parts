package com.example.help_stu_agent.ui.sparkyLink

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.PhotoLogEntity
import com.example.help_stu_agent.data.db.SparkyLinkLogEntity
import com.example.help_stu_agent.data.local.UserManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SparkyLinkPage(
    onBack: () -> Unit,
    viewModel: SparkyLinkViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val currentUserId by userManager.userIdFlow.collectAsState(initial = 0)

    val db = remember { AppDatabase.getInstance(context) }
    val photoLogDao = db.photoLogDao()
    val knowledgeCardDao = db.knowledgeCardDao()
    val sparkyLinkLogDao = db.sparkyLinkLogDao()

    var showPhotoDateSelector by remember { mutableStateOf(false) }
    var showReportDateSelector by remember { mutableStateOf(false) }

    val uploadedDates by photoLogDao.getUploadedDates(currentUserId ?: 0)
        .collectAsState(initial = emptyList())
    val reportDates by knowledgeCardDao.getReportDates()
        .collectAsState(initial = emptyList())

    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE3F2F9),
            Color(0xFFF7FBFC),
            Color(0xFFF3F8FB)
        )
    )

    LaunchedEffect(currentUserId) {
        currentUserId?.let { viewModel.updateUserId(it.toString()) }
    }

    LaunchedEffect(uiState.posteriorInsight, uiState.keyConcepts, uiState.isLoading) {
        if (uiState.posteriorInsight.isNotBlank() && !uiState.isLoading) {
            sparkyLinkLogDao.insert(
                SparkyLinkLogEntity(
                    userId = currentUserId ?: 0,
                    dateA = uiState.dateA,
                    dateB = uiState.dateB,
                    insight = uiState.posteriorInsight,
                    concepts = uiState.keyConcepts
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 0.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SparkyHeader(onBack = onBack)
                }

                item {
                    SparkySplitCard(
                        rawDate = uiState.dateA,
                        transformedDate = uiState.dateB,
                        onRawClick = { showPhotoDateSelector = true },
                        onTransformedClick = { showReportDateSelector = true }
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(color = Color(0xFFA78BFA))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "正在分析跨时空关联...",
                                color = Color(0xFF64748B),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    item {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (uiState.posteriorInsight.isNotBlank()) {
                    item {
                        InsightResultCard(
                            insight = uiState.posteriorInsight,
                            concepts = uiState.keyConcepts
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                SlideToSparkButton(
                    enabled = !uiState.isLoading,
                    onSlideComplete = {
                        viewModel.updateMock(false)
                        viewModel.generateSparkLinkBrief()
                    }
                )
            }
        }

        if (showPhotoDateSelector) {
            PhotoDateSelectionDialog(
                dates = uploadedDates,
                onDismiss = { showPhotoDateSelector = false },
                onDateSelected = {
                    viewModel.updateDateA(it)
                    showPhotoDateSelector = false
                },
                getPhotos = { date ->
                    photoLogDao.getPhotosByDate(currentUserId ?: 0, date)
                }
            )
        }

        if (showReportDateSelector) {
            ReportDateSelectionDialog(
                dates = reportDates,
                onDismiss = { showReportDateSelector = false },
                onDateSelected = {
                    viewModel.updateDateB(it)
                    showReportDateSelector = false
                }
            )
        }
    }
}

@Composable
fun PhotoDateSelectionDialog(
    dates: List<String>,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    getPhotos: suspend (String) -> List<PhotoLogEntity>
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "选择图片上传日期",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (dates.isEmpty()) {
                    Text(
                        text = "暂无上传记录",
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(dates) { date ->
                            val photos by produceState<List<PhotoLogEntity>>(
                                initialValue = emptyList(),
                                key1 = date
                            ) {
                                value = getPhotos(date)
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDateSelected(date) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = date,
                                        fontSize = 16.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (photos.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(photos) { photo ->
                                            Image(
                                                painter = rememberAsyncImagePainter(photo.localUri),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFF1F5F9)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("取消", color = Color(0xFF5D5FEF))
                }
            }
        }
    }
}

@Composable
fun ReportDateSelectionDialog(
    dates: List<String>,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "选择 Daily Report 日期",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (dates.isEmpty()) {
                    Text(
                        text = "暂无报告记录",
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(dates) { date ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDateSelected(date) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFD4A373),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = date,
                                    fontSize = 16.sp,
                                    color = Color(0xFF334155)
                                )
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("取消", color = Color(0xFF5D5FEF))
                }
            }
        }
    }
}

@Composable
private fun SparkyHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 4.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF1E293B)
                )
            }
        }

        Text(
            text = "Spark Link",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SparkySplitCard(
    rawDate: String,
    transformedDate: String,
    onRawClick: () -> Unit,
    onTransformedClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                16.dp,
                RoundedCornerShape(32.dp),
                spotColor = Color(0xFF94A3B8).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(32.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h * 0.45f
            val dipDepth = 25.dp.toPx()

            drawPath(
                path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, midY)
                    quadraticBezierTo(w / 2f, midY + dipDepth, 0f, midY)
                    close()
                },
                color = Color.White
            )

            drawPath(
                path = Path().apply {
                    moveTo(0f, midY)
                    quadraticBezierTo(w / 2f, midY + dipDepth, w, midY)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                },
                color = Color(0xFFFFF9ED)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .clickable { onRawClick() }
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Image Context",
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "HISTORY",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(icon = Icons.Default.Title)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Uploaded Photos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = rawDate,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .clickable { onTransformedClick() }
                    .padding(24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Report Data",
                        color = Color(0xFFD4A373),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF2E2),
                        border = BorderStroke(1.dp, Color(0xFFFFE4C4))
                    ) {
                        Text(
                            text = "HISTORY",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE88A31)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(
                        icon = Icons.Default.Eco,
                        iconTint = Color(0xFF65A30D)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Reports",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = transformedDate,
                            fontSize = 13.sp,
                            color = Color(0xFFD4A373)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (320.dp * 0.45f) - 24.dp)
                .size(48.dp)
                .shadow(
                    8.dp,
                    CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun IconBox(
    icon: ImageVector,
    iconTint: Color = Color(0xFF64748B)
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .border(
                1.dp,
                Color(0xFFF1F5F9),
                RoundedCornerShape(16.dp)
            )
            .background(
                Color.White,
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SlideToSparkButton(
    enabled: Boolean = true,
    onSlideComplete: () -> Unit
) {
    var containerWidth by remember { mutableIntStateOf(0) }
    val thumbSizeDp = 56.dp
    val paddingDp = 6.dp
    val density = LocalDensity.current

    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val paddingPx = with(density) { paddingDp.toPx() }

    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val maxDragPx = (containerWidth - thumbSizePx - (paddingPx * 2)).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) {
        (dragOffset.value / maxDragPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    val thumbScale by animateFloatAsState(
        targetValue = if (progress > 0f) 1.04f else 1f,
        animationSpec = tween(180),
        label = "thumbScale"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = (1f - progress * 0.5f).coerceIn(0.45f, 1f),
        animationSpec = tween(180),
        label = "labelAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                spotColor = Color(0xFFA78BFA).copy(alpha = 0.20f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(Color(0xFF0F172A), Color(0xFF111827))
                    } else {
                        listOf(Color(0xFF475569), Color(0xFF475569))
                    }
                )
            )
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        if (containerWidth > 0 && progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.14f))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8).copy(alpha = 0.18f),
                                Color(0xFF6366F1).copy(alpha = 0.22f)
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) "Slide to Spark" else "Generating...",
                color = Color.White.copy(alpha = labelAlpha),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row {
                repeat(3) { index ->
                    val arrowAlpha = (0.22f + index * 0.18f) * labelAlpha
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = arrowAlpha),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (containerWidth > 0) {
            Box(
                modifier = Modifier
                    .padding(start = paddingDp)
                    .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                    .size(thumbSizeDp)
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                    }
                    .shadow(
                        elevation = if (progress > 0f) 10.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.14f)
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFF1F5F9))
                        ),
                        shape = CircleShape
                    )
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(maxDragPx) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (dragOffset.value > maxDragPx * 0.82f) {
                                                dragOffset.animateTo(
                                                    targetValue = maxDragPx,
                                                    animationSpec = tween(180)
                                                )
                                                onSlideComplete()
                                                dragOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                            } else {
                                                dragOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            dragOffset.snapTo(
                                                (dragOffset.value + dragAmount)
                                                    .coerceIn(0f, maxDragPx)
                                            )
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Slide",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun InsightResultCard(
    insight: String,
    concepts: String
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0xFF6366F1).copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Posterior Insight",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString(insight + "\n\n" + concepts)
                        )
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = insight,
                    color = Color(0xFF334155),
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )
            }

            if (concepts.isNotBlank()) {
                Spacer(modifier = Modifier.height(22.dp))
                StructuredConceptsSection(concepts = concepts)
            }
        }
    }
}

@Composable
private fun StructuredConceptsSection(concepts: String) {
    val sections = remember(concepts) { parseConceptSections(concepts) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Key Concepts",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (sections.isEmpty()) {
            PlainConceptCard(
                title = "内容",
                body = concepts
            )
        } else {
            sections.forEachIndexed { index, section ->
                PlainConceptCard(
                    title = section.title,
                    body = section.body,
                    accentColor = when {
                        section.title.contains("课外") -> Color(0xFF6366F1)
                        section.title.contains("课内") -> Color(0xFFF59E0B)
                        section.title.contains("关联") -> Color(0xFF10B981)
                        section.title.contains("复习") || section.title.contains("练习") -> Color(0xFFEC4899)
                        else -> Color(0xFF64748B)
                    }
                )
                if (index != sections.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private data class ConceptSection(
    val title: String,
    val body: String
)

private fun parseConceptSections(text: String): List<ConceptSection> {
    val normalized = text
        .replace("\r\n", "\n")
        .replace(Regex("""^\s*1\.\s*课外信息要点"""), "## 课外信息要点")
        .replace(Regex("""\n\s*2\.\s*课内简报要点"""), "\n## 课内简报要点")
        .replace(Regex("""\n\s*3\.\s*关联点列表"""), "\n## 关联点列表")
        .replace(Regex("""\n\s*4\.\s*建议的复习/练习路径"""), "\n## 建议的复习/练习路径")
        .trim()

    val regex = Regex(
        pattern = """##\s*(.+?)\n(.*?)(?=\n##\s*.+?$|\Z)""",
        option = RegexOption.DOT_MATCHES_ALL
    )

    return regex.findAll(normalized).map {
        ConceptSection(
            title = it.groupValues[1].trim(),
            body = it.groupValues[2].trim()
        )
    }.toList()
}

@Composable
private fun PlainConceptCard(
    title: String,
    body: String,
    accentColor: Color = Color(0xFF6366F1)
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formatConceptBody(body),
                color = Color(0xFF475569),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private fun formatConceptBody(text: String): String {
    return text
        .replace("**", "")
        .replace(" - ", "\n• ")
        .replace(Regex("""\n\s*-\s+"""), "\n• ")
        .replace(Regex("""\n\s*(\d+\))"""), "\n$1")
        .trim()
}
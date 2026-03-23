package com.example.help_stu_agent.ui.sparkyLink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.PhotoLogEntity
import com.example.help_stu_agent.data.db.SparkyLinkLogEntity
import com.example.help_stu_agent.data.local.UserManager
import java.time.LocalDate

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

    var previewPhotoDate by remember { mutableStateOf<LocalDate?>(null) }
    var previewReportDate by remember { mutableStateOf<LocalDate?>(null) }
    var enlargedImageUri by remember { mutableStateOf<String?>(null) }

    val uploadedDates by photoLogDao.getUploadedDates(currentUserId ?: 0)
        .collectAsState(initial = emptyList())
    val reportDates by knowledgeCardDao.getReportDates()
        .collectAsState(initial = emptyList())

    val availablePhotoMap by produceState<Map<LocalDate, List<PhotoLogEntity>>>(
        initialValue = emptyMap(),
        key1 = uploadedDates,
        key2 = currentUserId
    ) {
        val uid = currentUserId ?: 0
        val result = linkedMapOf<LocalDate, List<PhotoLogEntity>>()
        uploadedDates.forEach { dateString ->
            val parsed = parseToLocalDate(dateString)
            if (parsed != null) {
                val photos = photoLogDao.getPhotosByDate(uid, dateString)
                if (photos.isNotEmpty()) {
                    result[parsed] = photos
                }
            }
        }
        value = result
    }

    val availableReportMap by produceState<Map<LocalDate, List<ReportPreviewItem>>>(
        initialValue = emptyMap(),
        key1 = reportDates
    ) {
        val result = linkedMapOf<LocalDate, List<ReportPreviewItem>>()
        reportDates.forEach { dateString ->
            val parsed = parseToLocalDate(dateString)
            if (parsed != null) {
                val titles = try {
                    knowledgeCardDao.getCardsByDate(dateString).map {
                        ReportPreviewItem(title = it.pdfDisplayName ?: "Untitled Report")
                    }
                } catch (_: Exception) {
                    emptyList()
                }
                result[parsed] = titles
            }
        }
        value = result
    }

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
                    SparkyHeader(onBackClick = onBack)
                }

                item {
                    SparkySplitCard(
                        rawDate = uiState.dateA,
                        transformedDate = uiState.dateB,
                        onRawClick = { showPhotoDateSelector = true },
                        onTransformedClick = { showReportDateSelector = true }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✦",
                            fontSize = 16.sp,
                            color = Color(0xFF00C2FF),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "探索记忆碎片间的奇妙共鸣",
                            color = Color(0xFF64748B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
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
                    enabled = uiState.dateA.isNotBlank() && uiState.dateB.isNotBlank(),
                    isLoading = uiState.isLoading,
                    onSlideComplete = { viewModel.generateSparkLinkBrief() }
                )
            }
        }

        if (showPhotoDateSelector) {
            PhotoDateSelectionDialog(
                availablePhotoMap = availablePhotoMap,
                onDismiss = { showPhotoDateSelector = false },
                onDateSelected = { dateString ->
                    viewModel.updateDateA(dateString)
                    showPhotoDateSelector = false
                },
                onDateLongPress = { localDate ->
                    previewPhotoDate = localDate
                }
            )
        }

        if (showReportDateSelector) {
            ReportDateSelectionDialog(
                availableReportMap = availableReportMap,
                onDismiss = { showReportDateSelector = false },
                onDateSelected = { dateString ->
                    viewModel.updateDateB(dateString)
                    showReportDateSelector = false
                },
                onDateLongPress = { localDate ->
                    previewReportDate = localDate
                }
            )
        }

        previewPhotoDate?.let { localDate ->
            PhotoPreviewDialog(
                date = localDate,
                photos = availablePhotoMap[localDate].orEmpty(),
                onDismiss = { previewPhotoDate = null },
                onPhotoClick = { uri ->
                    if (!uri.isNullOrBlank()) {
                        enlargedImageUri = uri
                    }
                }
            )
        }

        previewReportDate?.let { localDate ->
            ReportPreviewDialog(
                date = localDate,
                reports = availableReportMap[localDate].orEmpty(),
                onDismiss = { previewReportDate = null }
            )
        }

        enlargedImageUri?.let { uri ->
            FullscreenImageDialog(
                imageUri = uri,
                onDismiss = { enlargedImageUri = null }
            )
        }
    }
}

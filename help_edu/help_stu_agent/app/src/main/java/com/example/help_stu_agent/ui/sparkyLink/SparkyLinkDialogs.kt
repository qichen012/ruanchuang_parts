package com.example.help_stu_agent.ui.sparkyLink

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.help_stu_agent.data.db.PhotoLogEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PhotoDateSelectionDialog(
    availablePhotoMap: Map<LocalDate, List<PhotoLogEntity>>,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    onDateLongPress: (LocalDate) -> Unit
) {
    CalendarSelectionDialog(
        title = "选择照片日期",
        subtitle = "长按可快速预览当日照片",
        availableDates = availablePhotoMap.keys,
        accentColor = Color(0xFF6366F1),
        iconTint = Color(0xFF6366F1),
        onDismiss = onDismiss,
        onDateSelected = { localDate ->
            onDateSelected(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        },
        onDateLongPress = onDateLongPress
    )
}

@Composable
fun ReportDateSelectionDialog(
    availableReportMap: Map<LocalDate, List<ReportPreviewItem>>,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit,
    onDateLongPress: (LocalDate) -> Unit
) {
    CalendarSelectionDialog(
        title = "选择简报日期",
        subtitle = "长按可预览当日学习要点",
        availableDates = availableReportMap.keys,
        accentColor = Color(0xFFF59E0B),
        iconTint = Color(0xFFF59E0B),
        onDismiss = onDismiss,
        onDateSelected = { localDate ->
            onDateSelected(localDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        },
        onDateLongPress = onDateLongPress
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarSelectionDialog(
    title: String,
    subtitle: String,
    availableDates: Set<LocalDate>,
    accentColor: Color,
    iconTint: Color,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDateLongPress: (LocalDate) -> Unit
) {
    var currentMonth by remember {
        mutableStateOf(
            if (availableDates.isNotEmpty()) {
                YearMonth.from(availableDates.maxOrNull() ?: LocalDate.now())
            } else {
                YearMonth.now()
            }
        )
    }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val cells = remember(currentMonth) { buildMonthCells(currentMonth) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(24.dp, RoundedCornerShape(32.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Month Navigator
                CalendarMonthHeader(
                    currentMonth = currentMonth,
                    accentColor = accentColor,
                    onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                CalendarWeekHeader()

                Spacer(modifier = Modifier.height(8.dp))

                if (availableDates.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无可用记录",
                            color = Color(0xFF94A3B8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.height(300.dp),
                        userScrollEnabled = false
                    ) {
                        items(cells) { cell ->
                            val date = cell.date
                            val isEnabled = date != null && availableDates.contains(date)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()

                            CalendarDayCell(
                                date = date,
                                isCurrentMonth = cell.isCurrentMonth,
                                isEnabled = isEnabled,
                                isSelected = isSelected,
                                isToday = isToday,
                                accentColor = accentColor,
                                onClick = {
                                    if (date != null) {
                                        selectedDate = date
                                        onDateSelected(date)
                                    }
                                },
                                onLongClick = {
                                    if (date != null) {
                                        onDateLongPress(date)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("取消", color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarMonthHeader(
    currentMonth: YearMonth,
    accentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            fontSize = 17.sp
        )

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CalendarWeekHeader() {
    val weeks = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth()) {
        weeks.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarDayCell(
    date: LocalDate?,
    isCurrentMonth: Boolean,
    isEnabled: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> accentColor
        isEnabled -> accentColor.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    val textColor = when {
        !isCurrentMonth || date == null -> Color.Transparent
        isSelected -> Color.White
        isEnabled -> Color(0xFF334155)
        else -> Color(0xFFCBD5E1)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                color = if (isToday && !isSelected) accentColor.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                enabled = isCurrentMonth && isEnabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (date != null && isCurrentMonth) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    date: LocalDate,
    photos: List<PhotoLogEntity>,
    onDismiss: () -> Unit,
    onPhotoClick: (String?) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(32.dp, RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MM月dd日 记忆碎片")),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("当天未上传照片", color = Color(0xFF94A3B8))
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(photos) { photo ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { onPhotoClick(photo.localUri) }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(photo.localUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击图片可查看大图",
                    fontSize = 12.sp,
                    color = Color(0xFFA1A1AA),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ReportPreviewDialog(
    date: LocalDate,
    reports: List<ReportPreviewItem>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(32.dp, RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MM月dd日 学习简报")),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("当天未生成简报", color = Color(0xFF94A3B8))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reports) { report ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFDF7F2), RoundedCornerShape(16.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = report.title,
                                    color = Color(0xFF78350F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun FullscreenImageDialog(
    imageUri: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
            }
        }
    }
}

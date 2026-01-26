package com.example.help_stu_agent.ui.elite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteIdeasPage(
    onBack: () -> Unit,
) {
    var calendarExpanded by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var keyword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Elite Ideas", fontWeight = FontWeight.SemiBold)
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { calendarExpanded = !calendarExpanded }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null)
                        }
                    }
                )

                AnimatedVisibility(visible = calendarExpanded) {
                    CalendarStrip(
                        month = currentMonth,
                        selectedDate = selectedDate,
                        onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                        onSelect = { selectedDate = it }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部 Ideas 卡片
            item {
                IdeaHighlightCard()
            }

            // 关键词检索
            item {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search ideas, concepts, keywords") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
            }

            // 底部图片主题卡片
            items(sampleImageIdeas) { idea ->
                ImageIdeaCard(idea)
            }
        }
    }
}

@Composable
private fun IdeaHighlightCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0F766E), Color(0xFF064E3B))
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("★ WISDOM", color = Color(0xFFFACC15), fontSize = 12.sp)
            Column {
                Text(
                    "Circle of Competence",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Know what you know and what you do not know. The size of the circle is not as important as knowing its boundaries.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarStrip(
    month: YearMonth,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    val titleFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
    val title = remember(month) { month.atDay(1).format(titleFmt) }

    val first = remember(month) { month.atDay(1) }
    val daysInMonth = remember(month) { month.lengthOfMonth() }

    // 以「周日」为一周第一天：S M T W T F S
    // DayOfWeek.value: Monday=1 ... Sunday=7
    val leadingBlanks = remember(month) { first.dayOfWeek.value % 7 } // Sunday->0, Monday->1, ...

    val totalCells = leadingBlanks + daysInMonth
    val rows = remember(month) { ceil(totalCells / 7f).toInt() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // 顶部：左右切换 + 月份标题（居中）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevMonth) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prev month")
            }

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onNextMonth) {
                // 这里用 ArrowBack 旋转也行；你也可以换成 Icons.AutoMirrored.Filled.ArrowForward
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Next month",
                    modifier = Modifier.rotate(180f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 周标题：S M T W T F S
        val week = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { w ->
                Text(
                    text = w,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 日期网格（整月）
        var dayCounter = 1
        repeat(rows) { r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(7) { c ->
                    val cellIndex = r * 7 + c
                    val inMonth = cellIndex >= leadingBlanks && dayCounter <= daysInMonth

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (inMonth) {
                            val date = month.atDay(dayCounter)
                            val selected = date == selectedDate

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { onSelect(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayCounter.toString(),
                                    fontSize = 13.sp,
                                    color = if (selected) Color.White
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            dayCounter++
                        } else {
                            // 空白占位（上月/下月）
                            Spacer(Modifier.size(34.dp))
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ImageIdeaCard(idea: ImageIdea) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(id = idea.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                idea.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (idea.subtitle != null) {
                Text(
                    idea.subtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}


data class ImageIdea(
    val title: String,
    val subtitle: String?,
    val imageRes: Int
)

private val sampleImageIdeas = listOf(
    ImageIdea("Zermatt Ski Resort", "Valais, Switzerland", android.R.drawable.ic_menu_gallery),
    ImageIdea("Memento Mori", null, android.R.drawable.ic_menu_gallery)
)

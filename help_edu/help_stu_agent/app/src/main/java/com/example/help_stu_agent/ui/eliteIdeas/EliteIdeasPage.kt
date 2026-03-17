package com.example.help_stu_agent.ui.eliteIdeas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.R
import com.example.help_stu_agent.data.repo.EliteIdeaRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

private val LocalCaseImages = listOf(
    R.drawable.elite_ideas_photo_0,
    R.drawable.elite_ideas_photo_1,
    R.drawable.elite_ideas_photo_2,
    R.drawable.elite_ideas_photo_3,
    R.drawable.elite_ideas_photo_4,
    R.drawable.elite_ideas_photo_5,
    R.drawable.elite_ideas_photo_6,
    R.drawable.elite_ideas_photo_7
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EliteIdeasPage(
    onBack: () -> Unit,
    onOpenIdeaDetail: (String) -> Unit
) {
    var calendarExpanded by remember { mutableStateOf(false) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val context = LocalContext.current
    val repository = remember { EliteIdeaRepository(context) }

    val ideaEntities by repository.observeAll().collectAsState(initial = emptyList())

    val pageDataList = remember(ideaEntities) {
        ideaEntities.map { entity ->
            val instances = mutableListOf<RealizationInstance>()
            runCatching {
                val jsonArray = JSONArray(entity.instancesJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i)
                    if (obj != null) {
                        instances.add(
                            RealizationInstance(
                                title = obj.optString("title", obj.optString("name", "实例 ${i + 1}")),
                                description = obj.optString("description", obj.optString("content", "")),
                                imageRes = LocalCaseImages.random() // 👉 2. 在解析时就分配好随机图片，防止滑动时闪烁
                            )
                        )
                    }
                }
            }
            IdeaPageData(
                category = entity.category,
                title = entity.title,
                description = entity.description,
                instances = instances
            )
        }
    }

    val backgroundColor = Color(0xFFF7F7F5)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor),
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { calendarExpanded = !calendarExpanded }
                        ) {
                            Text("Elite Ideas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Icon(Icons.Default.ExpandMore, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
                )
                AnimatedVisibility(visible = calendarExpanded) {
                    CalendarStrip(currentMonth, selectedDate, { currentMonth = currentMonth.minusMonths(1) }, { currentMonth = currentMonth.plusMonths(1) }, { selectedDate = it })
                }
            }
        }
    ) { padding ->
        if (pageDataList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无 Elite Ideas，上传 PDF 获取你的核心思想卡片吧！", color = Color.Gray)
            }
            return@Scaffold
        }

        val pagerState = rememberPagerState(pageCount = { pageDataList.size })
        val coroutineScope = rememberCoroutineScope()
        val currentPageData = pageDataList[pagerState.currentPage]

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 40.dp)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { pageIndex ->
                        val pageData = pageDataList[pageIndex]
                        EliteIdeaCard(
                            category = pageData.category,
                            title = pageData.title,
                            description = pageData.description,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = 24.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(shape = CircleShape, shadowElevation = 4.dp, color = Color.White, modifier = Modifier.size(48.dp), onClick = { if (pagerState.currentPage > 0) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                            Icon(Icons.Default.ChevronLeft, "Previous", tint = Color.Gray, modifier = Modifier.padding(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(pageDataList.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(modifier = Modifier.height(6.dp).width(if (isSelected) 24.dp else 6.dp).clip(CircleShape).background(if (isSelected) Color(0xFF1F1F1F) else Color.White))
                            }
                        }
                        Surface(shape = CircleShape, shadowElevation = 4.dp, color = Color.White, modifier = Modifier.size(48.dp), onClick = { if (pagerState.currentPage < pagerState.pageCount - 1) coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                            Icon(Icons.Default.ChevronRight, "Next", tint = Color.Gray, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Adjust, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REALIZATION INSTANCES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            items(currentPageData.instances) { instance ->
                InstanceCard(instance = instance, onClick = { onOpenIdeaDetail(instance.title) })
            }
        }
    }
}


@Composable
private fun InstanceCard(instance: RealizationInstance, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp) // 卡片高度，接近你截图中的比例
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = instance.imageRes),
            contentDescription = instance.title,
            contentScale = ContentScale.Crop, // 裁剪填充整个 Box
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f) // 底部加深
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )


        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = instance.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (instance.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = instance.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CalendarStrip( month: YearMonth, selectedDate: LocalDate, onPrevMonth: () -> Unit, onNextMonth: () -> Unit, onSelect: (LocalDate) -> Unit) {
    val titleFmt = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
    val title = remember(month) { month.atDay(1).format(titleFmt) }
    val first = remember(month) { month.atDay(1) }
    val daysInMonth = remember(month) { month.lengthOfMonth() }
    val leadingBlanks = remember(month) { first.dayOfWeek.value % 7 }
    val totalCells = leadingBlanks + daysInMonth
    val rows = remember(month) { ceil(totalCells / 7f).toInt() }

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) { Icon(Icons.Default.ArrowBack, contentDescription = "Prev month") }
            Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onNextMonth) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Next month", modifier = Modifier.rotate(180f)) }
        }
        Spacer(Modifier.height(10.dp))
        val week = listOf("S", "M", "T", "W", "T", "F", "S")
        Row(modifier = Modifier.fillMaxWidth()) { week.forEach { w -> Text(text = w, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)) } }
        Spacer(Modifier.height(8.dp))
        var dayCounter = 1
        repeat(rows) { r ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(7) { c ->
                    val cellIndex = r * 7 + c
                    val inMonth = cellIndex >= leadingBlanks && dayCounter <= daysInMonth
                    Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        if (inMonth) {
                            val date = month.atDay(dayCounter)
                            val selected = date == selectedDate
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { onSelect(date) }, contentAlignment = Alignment.Center) {
                                Text(text = dayCounter.toString(), fontSize = 13.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                            dayCounter++
                        } else { Spacer(Modifier.size(34.dp)) }
                    }
                }
            }
        }
    }
}

// 👉 4. 更新数据模型，增加 imageRes
data class RealizationInstance(
    val title: String,
    val description: String,
    val imageRes: Int // 新增图片资源 ID
)
data class IdeaPageData(val category: String, val title: String, val description: String, val instances: List<RealizationInstance>)
package com.example.help_stu_agent.ui.tree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.help_stu_agent.data.db.KnowledgeTreeEntity
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTreeHistoryPage(
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { KnowledgeTreeRepository(context) }

    var list by remember { mutableStateOf<List<KnowledgeTreeEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        list = repo.listAll()
        loading = false
    }

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F9FE)) // 与 HomePage 主背景一致
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp, y = 60.dp)
                .size(300.dp, 620.dp)
                .rotate(-15f)
                .background(Color(0xFFE8EFFF), RoundedCornerShape(80.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            TopAppBar(
                title = {
                    Column {
                        Text("Knowledge Trees", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (loading) "Loading…" else "${list.size} saved",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F9FE)
                )
            )

            Spacer(Modifier.height(14.dp))

            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 24.dp),
                        color = Color(0xFF6366F1)
                    )
                }
            } else if (list.isEmpty()) {
                EmptyTreeHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { e ->
                        val accent = pickAccentColor(e.id)
                        TreeHistoryCard(
                            title = e.title.ifBlank { "Knowledge Tree" },
                            timeText = fmt.format(Date(e.createdAt)),
                            subtitle = e.pdfDisplayName,
                            accent = accent,
                            onClick = { onOpen(e.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeHistoryCard(
    title: String,
    timeText: String,
    subtitle: String?,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧小图标块：卡通化/轻松感
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                TreeIcon(modifier = Modifier.size(22.dp), color = accent)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))

                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTreeHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(84.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                TreeIcon(modifier = Modifier.size(34.dp), color = Color(0xFF6366F1))
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "No trees yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "After you generate a knowledge tree, it will appear here for quick access.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 20.sp
        )
    }
}

private fun pickAccentColor(id: String): Color {
    val palette = listOf(
        Color(0xFF6366F1), // indigo
        Color(0xFF0EA5E9), // sky
        Color(0xFF10B981), // emerald
        Color(0xFFF59E0B), // amber
        Color(0xFFEF4444), // red
        Color(0xFF8B5CF6)  // violet
    )
    val idx = (id.hashCode().absoluteValue) % palette.size
    return palette[idx]
}

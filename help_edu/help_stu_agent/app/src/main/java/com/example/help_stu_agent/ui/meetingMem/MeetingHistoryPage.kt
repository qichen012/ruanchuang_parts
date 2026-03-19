package com.example.help_stu_agent.ui.meetingMem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingHistoryPage(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = MeetingViewModel.factory(androidx.compose.ui.platform.LocalContext.current))
) {
    val meetings by viewModel.userMeetings.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = Color(0xFFF8FBFF),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Meeting History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (meetings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No records found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(meetings) { meeting ->
                    MeetingHistoryItem(meeting = meeting, onClick = { onOpenDetail(meeting.id) })
                }
            }
        }
    }
}

@Composable
fun MeetingHistoryItem(meeting: MeetingMinutesEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFFE8F4FD), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Mic, null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meeting.summary.takeIf { it.isNotBlank() } ?: "No Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDateTime(meeting.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

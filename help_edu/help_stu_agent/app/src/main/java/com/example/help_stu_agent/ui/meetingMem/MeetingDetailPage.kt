package com.example.help_stu_agent.ui.meetingMem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.help_stu_agent.data.db.MeetingMinutesEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailPage(
    meetingId: String,
    onBack: () -> Unit,
    viewModel: MeetingViewModel = viewModel(factory = MeetingViewModel.factory(LocalContext.current))
) {
    var meeting by remember { mutableStateOf<MeetingMinutesEntity?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(meetingId) {
        meeting = viewModel.getMeetingById(meetingId)
        loading = false
    }

    Scaffold(
        containerColor = Color(0xFFF8FBFF),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Meeting Detail",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "HISTORY RECORD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                    }
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (meeting != null) {
                val m = meeting!!
                MeetingMinutesResult(
                    minutes = m.toMeetingMinutes(),
                    createdAt = m.createdAt,
                    showCard = false
                )
            } else {
                Text("Meeting not found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

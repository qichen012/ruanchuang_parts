package com.example.help_stu_agent.ui.meetingMem

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun LightRecordingScreen(
    isRecording: Boolean,
    isPaused: Boolean,
    seconds: Long,
    level: Float,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onUploadClick: () -> Unit,
    onGoHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MeetingViewModel = viewModel(factory = MeetingViewModel.factory(context))
    val latestMeeting by viewModel.latestMeeting.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Card (Latest Summary / Topic)
        LessonHeaderCard(
            meeting = latestMeeting,
            onClick = onGoHistory,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.weight(1f))

        // 2. Middle Content
        if (!isRecording) {
            LargeMicIdleIndicator(onClick = onStart)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatTime(seconds),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                AudioWaveformVisualizer(level = if (isPaused) 0f else level)
            }
        }

        Spacer(Modifier.weight(1.2f))

        // 3. Bottom Controls
        if (!isRecording) {
            StartRecordControlBar(onStart = onStart, onUploadClick = onUploadClick)
        } else {
            RecordingControlBar(
                isPaused = isPaused,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop
            )
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun LargeMicIdleIndicator(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9).copy(alpha = 0.5f),
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Mic,
                    null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Tap to start recording",
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun StartRecordControlBar(onStart: () -> Unit, onUploadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Start Recording Button (Center)
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "press_scale")

        Surface(
            shape = CircleShape,
            color = Color(0xFF0F172A),
            shadowElevation = 12.dp,
            onClick = { onStart() },
            modifier = Modifier
                .size(92.dp)
                .scale(scale)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isPressed = event.type != PointerEventType.Release && event.type != PointerEventType.Exit
                        }
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(36.dp))
                Box(
                    modifier = Modifier
                        .padding(18.dp)
                        .size(12.dp)
                        .background(Color(0xFFF43F5E), CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }

        // Upload Button (Right)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .clickable { onUploadClick() }
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.CloudUpload,
                "Upload",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
            Text("Upload", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}



@Composable
fun AudioWaveformVisualizer(level: Float) {
    val dots = 24
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.height(40.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(dots) { index ->
                val factor = if (index % 2 == 0) 0.6f else 1f
                val height by animateFloatAsState(
                    targetValue = (4.dp.value + level * 30.dp.value * factor).coerceAtLeast(4.dp.value),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = ""
                )
                Box(
                    Modifier
                        .size(width = 4.dp, height = height.dp)
                        .background(Color(0xFF2DD4BF), CircleShape)
                )
            }
        }
    }
}

@Composable
fun RecordingControlBar(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    isProcessing: Boolean = false
) {
    // Scaling animation for the center button pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by if (!isPaused && !isProcessing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop Button (Left)
        Surface(
            onClick = onStop,
            enabled = !isProcessing,
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Stop, "Stop", tint = Color(0xFFF43F5E), modifier = Modifier.size(28.dp))
            }
        }

        // Pause/Play Button (Center with Pulse)
        Surface(
            shape = CircleShape,
            color = Color(0xFF0F172A),
            shadowElevation = 8.dp,
            onClick = { if (isPaused) onResume() else onPause() },
            enabled = !isProcessing,
            modifier = Modifier
                .size(88.dp)
                .scale(pulseScale)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // Placeholder to keep center button centered
        Box(modifier = Modifier.size(64.dp))
    }
}

@Composable
fun LessonHeaderCard(
    meeting: MeetingMinutesEntity? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(6.dp).background(Color(0xFF2DD4BF)))
            
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (meeting != null) "LATEST SUMMARY" else "TOPIC",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCBD5E1))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = meeting?.summary?.takeIf { it.isNotBlank() } ?: "Advanced Calculus 101",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(24.dp))
                
                val displayDate = meeting?.createdAt?.let { formatDateTime(it) } ?: "Aug 13th"
                val displayUpdate = meeting?.updatedAt?.let { formatDateTime(it) } ?: "13:30 PM"
                
                Row(Modifier.fillMaxWidth()) {
                    InfoBlock(
                        icon = Icons.Default.CalendarMonth,
                        iconBg = Color(0xFFFEF9C3),
                        iconTint = Color(0xFFEAB308),
                        label = "CREATED",
                        value = displayDate.substringBefore(" •")
                    )
                    Spacer(Modifier.width(24.dp))
                    Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFFF1F5F9)))
                    Spacer(Modifier.width(24.dp))
                    InfoBlock(
                        icon = if (meeting != null) Icons.Outlined.History else Icons.Default.AccessTime,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF10B981),
                        label = if (meeting != null) "UPDATED" else "TIME",
                        value = displayUpdate.substringAfter("• ").ifBlank { displayUpdate }
                    )
                }
            }
        }
    }
}

@Composable
fun InfoBlock(icon: ImageVector, iconBg: Color, iconTint: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = iconBg.copy(alpha = 0.5f), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
        }
    }
}

private fun formatTime(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}

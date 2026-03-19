package com.example.help_stu_agent.ui.meetingMem

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import java.util.UUID

private enum class MeetScreen { Record, Processing, Text }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingMinutesPage(
    onBack: () -> Unit,
    onGoHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val wm = remember { WorkManager.getInstance(context) }

    val rec by MeetingRecorder.state.collectAsState()

    var screen by remember { mutableStateOf(MeetScreen.Record) }
    var rawResult by remember { mutableStateOf<String?>(null) }

    var uploadWorkId by remember { mutableStateOf<UUID?>(null) }

    val workerUuid = remember(rec.lastWorkId, uploadWorkId) {
        uploadWorkId ?: runCatching { rec.lastWorkId?.let { UUID.fromString(it) } }.getOrNull()
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            screen = MeetScreen.Processing
            val inputData = workDataOf(MeetingTranscribeWorker.KEY_AUDIO_PATH to uri.toString())
            val uploadRequest = OneTimeWorkRequestBuilder<MeetingTranscribeWorker>()
                .setInputData(inputData)
                .build()
            uploadWorkId = uploadRequest.id
            wm.enqueue(uploadRequest)
        }
    }

    val wi by workerUuid?.let { id -> wm.getWorkInfoByIdLiveData(id).observeAsState() }
        ?: remember { mutableStateOf<WorkInfo?>(null) }

    var stage by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }


    LaunchedEffect(wi) {
        val info = wi ?: return@LaunchedEffect
        stage = info.progress.getString(MeetingTranscribeWorker.KEY_STAGE).orEmpty()
        progress = info.progress.getFloat(MeetingTranscribeWorker.KEY_PROGRESS, 0f).coerceIn(0f, 1f)

        when (info.state) {
            WorkInfo.State.SUCCEEDED -> {
                val text = info.outputData.getString(MeetingTranscribeWorker.KEY_TEXT).orEmpty()
                val summary = info.outputData.getString(MeetingTranscribeWorker.KEY_SUMMARY).orEmpty()
                val points = info.outputData.getString(MeetingTranscribeWorker.KEY_POINTS).orEmpty()
                val todos = info.outputData.getString(MeetingTranscribeWorker.KEY_TODOS).orEmpty()
                
                rawResult = if (summary.isNotBlank() || points.isNotBlank() || todos.isNotBlank()) {
                    """{"text":"$text","summary":"$summary","points":$points,"todos":$todos}"""
                } else {
                    text
                }
                screen = MeetScreen.Text
            }
            WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> screen = MeetScreen.Processing
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> screen = MeetScreen.Record
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) MeetingRecorder.start(context)
    }

    Scaffold(
        containerColor = Color(0xFFF8FBFF),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Meeting Mem",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (screen == MeetScreen.Text) "AI GENERATED NOTES" else "AUDIO CAPTURE",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "meet_screen"
            ) { s ->
                when (s) {
                    MeetScreen.Record -> {
                        LightRecordingScreen(
                            isRecording = rec.isRecording,
                            isPaused = rec.isPaused,
                            seconds = rec.seconds,
                            level = rec.level01,
                            onStart = { permLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            onPause = { MeetingRecorder.pause(context) },
                            onResume = { MeetingRecorder.resume(context) },
                            onStop = {
                                MeetingRecorder.stop(context)
                                screen = MeetScreen.Processing
                            },
                            onUploadClick = { audioPickerLauncher.launch("audio/*") },
                            onGoHistory = onGoHistory
                        )
                    }

                    MeetScreen.Processing -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LessonHeaderCard(modifier = Modifier.padding(horizontal = 24.dp))
                            Spacer(Modifier.weight(1f))
                            CircularProgressIndicator(
                                color = Color(0xFF0F172A),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "正在由AI整理纪要...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.weight(1.2f))
                            RecordingControlBar(
                                isPaused = false,
                                onPause = {},
                                onResume = {},
                                onStop = {},
                                isProcessing = true
                            )
                        }
                    }

                    MeetScreen.Text -> {
                        val minutes = remember(rawResult) { parseMeetingMinutes(rawResult.orEmpty()) }
                        MeetingMinutesResult(
                            minutes = minutes,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Meet Memo", minutes.toPlainText()))
                            },
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, minutes.toPlainText())
                                }
                                context.startActivity(Intent.createChooser(intent, "Share"))
                            },
                            onRecordAgain = {
                                rawResult = null
                                screen = MeetScreen.Record
                            }
                        )
                    }
                }
            }
        }
    }
}

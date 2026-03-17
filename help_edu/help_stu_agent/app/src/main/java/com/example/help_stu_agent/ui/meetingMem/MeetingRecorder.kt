package com.example.help_stu_agent.ui.meetingMem

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class MeetingRecorderState(
    val isRecording: Boolean = false,
    val seconds: Long = 0,
    val lastFilePath: String? = null,
    val level01: Float = 0f,
    val error: String? = null,

    val lastWorkId: String? = null,
    val lastTraceId: String? = null
)

object MeetingRecorder {
    private val _state = MutableStateFlow(MeetingRecorderState())
    val state: StateFlow<MeetingRecorderState> = _state

    internal fun update(s: MeetingRecorderState) { _state.value = s }

    fun start(context: Context) {
        val i = Intent(context, MeetingRecorderService::class.java).apply {
            action = MeetingRecorderService.ACTION_START
        }
        context.startForegroundService(i)
    }

    fun stop(context: Context) {
        val i = Intent(context, MeetingRecorderService::class.java).apply {
            action = MeetingRecorderService.ACTION_STOP
        }
        context.startService(i)
    }

    fun deleteFileIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}

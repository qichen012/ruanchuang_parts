package com.example.help_stu_agent.ui.meeting

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

class MeetingRecorderService : Service() {

    companion object {
        const val ACTION_START = "meeting_rec_start"
        const val ACTION_STOP = "meeting_rec_stop"

        private const val CHANNEL_ID = "meeting_rec_channel"
        private const val NOTIF_ID = 5101
    }

    private var recorder: MediaRecorder? = null
    private var filePath: String? = null
    private var seconds: Long = 0L
    private var level01: Float = 0f

    // ✅ NEW: 记录最近一次转写任务（用于 UI 检查）
    private var lastWorkId: String? = null
    private var lastTraceId: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        pushState(isRecording = false, error = null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopTicker()
        runCatching { recorder?.release() }
        recorder = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (recorder != null) return

        val out = createOutputFile()
        filePath = out.absolutePath
        seconds = 0L
        level01 = 0f

        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
        recorder = r

        runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(128_000)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()

            startForeground(NOTIF_ID, buildNotification("Recording… 00:00"))
            startTicker()
            pushState(isRecording = true, error = null)
        }.onFailure { e ->
            runCatching { r.release() }
            recorder = null
            pushState(isRecording = false, error = e.message ?: "Start recording failed")
            stopSelf()
        }
    }

    /**
     * ✅ 停止录音后：立即把音频 enqueue 到 WorkManager（后台上传+转写）
     * 这样即便页面退出，也能“静默处理”并发送到后端。
     */
    private fun stopRecording() {
        val r = recorder ?: run { stopSelf(); return }

        stopTicker()

        runCatching { r.stop() }.onFailure {
            Log.w("MeetMemo", "recorder.stop failed: ${it.message}")
        }
        runCatching { r.release() }
        recorder = null
        level01 = 0f

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification("Saved: ${File(filePath ?: "").name}"))

        val path = filePath
        if (!path.isNullOrBlank()) {
            enqueueTranscribe(path)
        } else {
            Log.w("MeetMemo", "stopRecording: filePath is blank, skip enqueue")
        }

        pushState(isRecording = false, error = null)
        stopSelf()
    }

    private fun enqueueTranscribe(path: String) {
        val traceId = UUID.randomUUID().toString()
        val req = OneTimeWorkRequestBuilder<MeetingTranscribeWorker>()
            .setInputData(
                workDataOf(
                    MeetingTranscribeWorker.KEY_AUDIO_PATH to path,
                    MeetingTranscribeWorker.KEY_TRACE_ID to traceId
                )
            )
            // ✅ 固定 tag：便于检索所有会议转写任务
            .addTag(MeetingTranscribeWorker.TAG_MEETING_TRANSCRIBE)
            // ✅ 单次 trace tag：便于定位这一条
            .addTag("trace_$traceId")
            .build()

        lastWorkId = req.id.toString()
        lastTraceId = traceId

        Log.d("MeetMemo", "Enqueue transcribe workId=${req.id} traceId=$traceId path=$path")
        WorkManager.getInstance(this).enqueue(req)

        // 立即推一次 state，让 UI 立刻拿到 lastWorkId/lastTraceId
        pushState(isRecording = false, error = null)
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            var msAcc = 0L
            while (isActive) {
                delay(100) // 10Hz 音量采样
                msAcc += 100

                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                level01 = ampToLevel01(amp)

                if (msAcc >= 1000) {
                    msAcc -= 1000
                    seconds += 1
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIF_ID, buildNotification("Recording… ${fmt(seconds)}"))
                }

                pushState(isRecording = true, error = null)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun pushState(isRecording: Boolean, error: String?) {
        MeetingRecorder.update(
            MeetingRecorderState(
                isRecording = isRecording,
                seconds = seconds,
                lastFilePath = filePath,
                level01 = level01,
                error = error,
                lastWorkId = lastWorkId,
                lastTraceId = lastTraceId
            )
        )
    }

    private fun ampToLevel01(amp: Int): Float {
        val a = max(0, amp)
        if (a == 0) return 0f
        val x = ln(1.0 + a.toDouble()) / ln(1.0 + 32767.0)
        return min(1f, max(0f, x.toFloat()))
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Meeting Recording", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Meet Memo recording"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Meet Memo")
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createOutputFile(): File {
        val dir = File(cacheDir, "meeting_audio").apply { mkdirs() }
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "meeting_$ts.m4a")
    }

    private fun fmt(sec: Long): String = "%02d:%02d".format(sec / 60, sec % 60)
}

package com.example.help_stu_agent.ui.meetingMem

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
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
        const val ACTION_PAUSE = "meeting_rec_pause"
        const val ACTION_RESUME = "meeting_rec_resume"
        const val ACTION_STOP = "meeting_rec_stop"

        private const val CHANNEL_ID = "meeting_rec_channel"
        private const val NOTIF_ID = 5101
    }

    private var recorder: MediaRecorder? = null
    private var filePath: String? = null
    private var seconds: Long = 0L
    private var level01: Float = 0f
    private var isPaused: Boolean = false

    private var lastWorkId: String? = null
    private var lastTraceId: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        pushState(isRecording = false, isPaused = false, error = null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
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
        isPaused = false

        val r = MediaRecorder(this)
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
            pushState(isRecording = true, isPaused = false, error = null)
        }.onFailure { e ->
            runCatching { r.release() }
            recorder = null
            pushState(isRecording = false, isPaused = false, error = e.message ?: "Start recording failed")
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (recorder == null || isPaused) return
        runCatching {
            recorder?.pause()
            isPaused = true
            // We keep the ticker running to update the UI with 0 volume,
            // but we handle the timer increment inside startTicker.
            pushState(isRecording = true, isPaused = true, error = null)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification("Paused ${fmt(seconds)}"))
        }.onFailure { e ->
            Log.e("MeetMemo", "Pause failed", e)
        }
    }

    private fun resumeRecording() {
        if (recorder == null || !isPaused) return
        runCatching {
            recorder?.resume()
            isPaused = false
            pushState(isRecording = true, isPaused = false, error = null)
        }.onFailure { e ->
            Log.e("MeetMemo", "Resume failed", e)
        }
    }

    private fun stopRecording() {
        val r = recorder ?: run { stopSelf(); return }

        stopTicker()

        runCatching {
            r.stop()
        }.onFailure {
            Log.w("MeetMemo", "recorder.stop failed: ${it.message}")
        }
        runCatching { r.release() }
        recorder = null
        level01 = 0f
        isPaused = false

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification("Saved: ${File(filePath ?: "").name}"))

        val path = filePath
        if (!path.isNullOrBlank()) {
            enqueueTranscribe(path)
        }

        pushState(isRecording = false, isPaused = false, error = null)
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
            .addTag("trace_$traceId")
            .build()

        lastWorkId = req.id.toString()
        lastTraceId = traceId

        WorkManager.getInstance(this).enqueue(req)
        pushState(isRecording = false, isPaused = false, error = null)
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch {
            var msAcc = 0L
            while (isActive) {
                delay(100)
                
                if (!isPaused) {
                    msAcc += 100
                    val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                    level01 = ampToLevel01(amp)

                    if (msAcc >= 1000) {
                        msAcc -= 1000
                        seconds += 1
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIF_ID, buildNotification("Recording… ${fmt(seconds)}"))
                    }
                } else {
                    level01 = 0f
                }

                pushState(isRecording = true, isPaused = isPaused, error = null)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun pushState(isRecording: Boolean, isPaused: Boolean, error: String?) {
        MeetingRecorder.update(
            MeetingRecorderState(
                isRecording = isRecording,
                isPaused = isPaused,
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
            PendingIntent.FLAG_UPDATE_CURRENT or (PendingIntent.FLAG_IMMUTABLE)
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

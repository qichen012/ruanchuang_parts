package com.example.help_stu_agent.ui.meetingMem

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class MeetingTranscribeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_AUDIO_PATH = "audioPath"
        const val KEY_TEXT = "text"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"

        // ✅ 用于查询/排查
        const val TAG_MEETING_TRANSCRIBE = "tag_meeting_transcribe"
        const val KEY_TRACE_ID = "traceId"
    }

    private val baseUrl = "http://10.29.142.138:8001"
    private val endpoint = "/meeting_transcribe"

    override suspend fun doWork(): Result {
        val path = inputData.getString(KEY_AUDIO_PATH).orEmpty()
        val traceId = inputData.getString(KEY_TRACE_ID).orEmpty()

        Log.d("MeetMemo", "Worker start traceId=$traceId path=$path")

        if (path.isBlank()) return Result.failure(workDataOf("error" to "audioPath empty", KEY_TRACE_ID to traceId))

        val f = File(path)
        if (!f.exists() || f.length() <= 0) {
            return Result.failure(workDataOf("error" to "audio file missing/empty", KEY_TRACE_ID to traceId))
        }

        setProgress(workDataOf(KEY_STAGE to "Uploading", KEY_PROGRESS to 0.25f, KEY_TRACE_ID to traceId))

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build()

        val audioBody = f.asRequestBody("audio/mp4".toMediaType())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", f.name, audioBody) // ✅ 与 FastAPI: file=UploadFile 对齐
            .build()

        val url = baseUrl.trimEnd('/') + endpoint
        val req = Request.Builder().url(url).post(multipart).build()

        return try {
            Log.d("MeetMemo", "HTTP POST -> $url traceId=$traceId file=${f.name} size=${f.length()}")
            setProgress(workDataOf(KEY_STAGE to "Processing", KEY_PROGRESS to 0.7f, KEY_TRACE_ID to traceId))

            val resp = client.newCall(req).execute()
            val body = resp.body?.string().orEmpty()

            Log.d("MeetMemo", "HTTP RESP code=${resp.code} traceId=$traceId bodyPrefix=${body.take(200)}")

            if (!resp.isSuccessful) {
                return Result.failure(
                    workDataOf(
                        "error" to "HTTP ${resp.code}: $body",
                        KEY_TRACE_ID to traceId
                    )
                )
            }

            val text = extractText(body)
            setProgress(workDataOf(KEY_STAGE to "Done", KEY_PROGRESS to 1f, KEY_TRACE_ID to traceId))
            Result.success(workDataOf(KEY_TEXT to text, KEY_TRACE_ID to traceId))
        } catch (e: Exception) {
            Log.e("MeetMemo", "Worker exception traceId=$traceId ${e.message}", e)
            Result.failure(workDataOf("error" to (e.message ?: "upload failed"), KEY_TRACE_ID to traceId))
        }
    }

    private fun extractText(body: String): String {
        val t = body.trim()
        if (t.isBlank()) return ""
        return runCatching {
            val jo = JSONObject(t)
            when {
                jo.has("text") -> jo.optString("text")
                jo.has("minutes") -> jo.optString("minutes")
                jo.has("result") -> jo.optString("result")
                else -> jo.toString(2)
            }
        }.getOrElse { t }
    }
}

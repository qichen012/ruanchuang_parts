package com.example.help_stu_agent.ui.meetingMem

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import com.example.help_stu_agent.data.local.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLConnection
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

class MeetingTranscribeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_AUDIO_PATH = "audioPath"
        const val KEY_USER_ID = "user_id"
        const val KEY_DURATION = "duration"
        const val KEY_COURSE_NAME = "course_name"

        const val KEY_TEXT = "text"
        const val KEY_SUMMARY = "summary"
        const val KEY_POINTS = "points"
        const val KEY_TODOS = "todos"
        const val KEY_MINUTES = "minutes"
        const val KEY_TOPIC = "topic"
        const val KEY_CONCLUSIONS = "conclusions"

        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        const val KEY_TRACE_ID = "traceId"

        const val TAG_MEETING_TRANSCRIBE = "tag_meeting_transcribe"

    }

    private val baseUrl = "http://10.29.142.138:8001"
    private val endpoint = "/meeting_transcribe"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var audioPath = inputData.getString(KEY_AUDIO_PATH).orEmpty()
        val traceId = inputData.getString(KEY_TRACE_ID).orEmpty().ifBlank { id.toString() }

        if (audioPath.isBlank()) {
            return@withContext Result.failure(
                workDataOf(
                    KEY_ERROR to "audioPath empty",
                    KEY_TRACE_ID to traceId
                )
            )
        }

        if (audioPath.startsWith("content://")) {
            val localPath = copyUriToLocalCache(audioPath) ?: return@withContext Result.failure(
                workDataOf(
                    KEY_ERROR to "Failed to copy URI to local cache",
                    KEY_TRACE_ID to traceId
                )
            )
            audioPath = localPath
            Log.d(TAG_MEETING_TRANSCRIBE, "Converted URI to local path: $audioPath")
        }

        try {
            setProgress(
                workDataOf(
                    KEY_STAGE to "Preparing",
                    KEY_PROGRESS to 0.05f,
                    KEY_TRACE_ID to traceId
                )
            )

            val responseBody = requestMeetingMinutes(audioPath, traceId)

            Log.d(TAG_MEETING_TRANSCRIBE, "HTTP RESP FULL traceId=$traceId body=$responseBody")

            val status = extractField(responseBody, "status")
            val minutesMarkdown = extractField(responseBody, "minutes")

            if (status != "success" || minutesMarkdown.isBlank()) {
                return@withContext Result.failure(
                    workDataOf(
                        KEY_ERROR to "Invalid response: $responseBody",
                        KEY_TRACE_ID to traceId
                    )
                )
            }

            setProgress(
                workDataOf(
                    KEY_STAGE to "Parsing",
                    KEY_PROGRESS to 0.85f,
                    KEY_TRACE_ID to traceId
                )
            )

            val parsed = parseMeetingMinutesMarkdown(minutesMarkdown)
            val pointsJson = JSONArray(parsed.points).toString()
            val todosJson = JSONArray(parsed.todos).toString()
            val conclusionsJson = JSONArray(parsed.conclusions).toString()

            setProgress(
                workDataOf(
                    KEY_STAGE to "Saving",
                    KEY_PROGRESS to 0.95f,
                    KEY_TRACE_ID to traceId
                )
            )

            saveToDatabase(
                audioPath = audioPath,
                text = parsed.rawText,
                summary = parsed.summary,
                pointsJson = pointsJson,
                todosJson = todosJson,
                minutesMarkdown = minutesMarkdown,
                topic = parsed.topic,
                traceId = traceId
            )

            setProgress(
                workDataOf(
                    KEY_STAGE to "Done",
                    KEY_PROGRESS to 1f,
                    KEY_TRACE_ID to traceId
                )
            )

            return@withContext Result.success(
                workDataOf(
                    KEY_TEXT to parsed.rawText,
                    KEY_SUMMARY to parsed.summary,
                    KEY_POINTS to pointsJson,
                    KEY_TODOS to todosJson,
                    KEY_MINUTES to minutesMarkdown,
                    KEY_TOPIC to parsed.topic,
                    KEY_CONCLUSIONS to conclusionsJson,
                    KEY_TRACE_ID to traceId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG_MEETING_TRANSCRIBE, "Worker exception traceId=$traceId ${e.message}", e)
            return@withContext Result.failure(
                workDataOf(
                    KEY_ERROR to (e.message ?: "upload failed"),
                    KEY_TRACE_ID to traceId
                )
            )
        }
    }

    private suspend fun requestMeetingMinutes(
        audioPath: String,
        traceId: String
    ): String = withContext(Dispatchers.IO) {
        setProgress(
            workDataOf(
                KEY_STAGE to "Uploading",
                KEY_PROGRESS to 0.25f,
                KEY_TRACE_ID to traceId
            )
        )

        val uploadPayload = buildUploadPayload(audioPath)
            ?: throw IllegalStateException("Cannot read audio content")

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", uploadPayload.fileName, uploadPayload.body)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build()

        val url = baseUrl.trimEnd('/') + endpoint
        val request = Request.Builder()
            .url(url)
            .post(multipart)
            .build()

        Log.d(
            TAG_MEETING_TRANSCRIBE,
            "HTTP POST -> $url traceId=$traceId file=${uploadPayload.fileName} size=${uploadPayload.fileSize}"
        )

        setProgress(
            workDataOf(
                KEY_STAGE to "Processing",
                KEY_PROGRESS to 0.70f,
                KEY_TRACE_ID to traceId
            )
        )

        client.newCall(request).execute().use { response ->
            val responseCode = response.code
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP $responseCode: $responseBody")
            }

            responseBody
        }
    }

    private data class UploadPayload(
        val fileName: String,
        val fileSize: Long,
        val body: RequestBody
    )

    private fun buildUploadPayload(path: String): UploadPayload? {
        return if (path.startsWith("content://")) {
            val uri = path.toUri()
            val resolver = applicationContext.contentResolver

            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = queryDisplayName(uri) ?: "upload_audio"
            val mimeType = resolver.getType(uri)
                ?: guessMimeType(fileName)
                ?: "application/octet-stream"

            UploadPayload(
                fileName = fileName,
                fileSize = bytes.size.toLong(),
                body = bytes.toRequestBody(mimeType.toMediaType())
            )
        } else {
            val file = File(path)
            if (!file.exists()) return null

            val mimeType = guessMimeType(file.name) ?: "application/octet-stream"

            UploadPayload(
                fileName = file.name,
                fileSize = file.length(),
                body = file.asRequestBody(mimeType.toMediaType())
            )
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = applicationContext.contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
    }

    private fun guessMimeType(fileName: String): String? {
        return URLConnection.guessContentTypeFromName(fileName)
    }

    private fun extractField(body: String, fieldName: String): String {
        val t = body.trim()
        if (t.isBlank()) return ""
        return runCatching {
            val jo = JSONObject(t)
            jo.optString(fieldName, "")
        }.getOrElse { "" }
    }

    private fun copyUriToLocalCache(uriString: String): String? {
        return try {
            val uri = uriString.toUri()
            val contentResolver = applicationContext.contentResolver
            val fileName = queryDisplayName(uri) ?: "audio_upload"
            val extension = queryFileExtension(fileName) ?: "m4a"
            val cleanedFileName = "uploaded_${System.currentTimeMillis()}.$extension"

            val dir = File(applicationContext.cacheDir, "meeting_audio").apply { mkdirs() }
            val outFile = File(dir, cleanedFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG_MEETING_TRANSCRIBE, "Failed to copy URI to cache: ${e.message}", e)
            null
        }
    }

    private fun queryFileExtension(fileName: String): String? {
        val lastDot = fileName.lastIndexOf(".")
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            null
        }
    }

    private suspend fun saveToDatabase(
        audioPath: String,
        text: String,
        summary: String,
        pointsJson: String,
        todosJson: String,
        minutesMarkdown: String,
        topic: String,
        traceId: String
    ) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.meetingMinutesDao()

            val userManager = UserManager(applicationContext)
            val userId = runCatching {
                userManager.userIdFlow.firstOrNull() ?: inputData.getInt(KEY_USER_ID, 0)
            }.getOrDefault(inputData.getInt(KEY_USER_ID, 0))

            val audioFile = File(audioPath)
            val audioFileName = audioFile.name
            val audioFileSize = audioFile.length()
            val duration = inputData.getLong(KEY_DURATION, 0L)
            val courseName = inputData.getString(KEY_COURSE_NAME).orEmpty()

            val entity = MeetingMinutesEntity(
                userId = userId,
                rawText = text,
                summary = summary,
                pointsJson = pointsJson,
                todosJson = todosJson,
                minutesMarkdown = minutesMarkdown,
                audioFileName = audioFileName,
                audioFileSize = audioFileSize,
                audioLocalPath = audioPath,
                courseName = courseName,
                topic = topic,
                duration = duration
            )

            dao.insert(entity)

            Log.d(
                TAG_MEETING_TRANSCRIBE,
                "Saved to database: id=${entity.id}, userId=$userId, audioFile=$audioFileName, traceId=$traceId"
            )
        } catch (e: Exception) {
            Log.e(
                TAG_MEETING_TRANSCRIBE,
                "Failed to save to database: ${e.message}, traceId=$traceId",
                e
            )
        }
    }
}
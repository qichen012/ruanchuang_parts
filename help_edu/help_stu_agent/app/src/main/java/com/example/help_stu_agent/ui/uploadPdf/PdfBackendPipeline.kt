package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * 统一存放：上传PDF -> 后端处理 -> 拉取知识树JSON 的所有逻辑
 */
object PdfTreeCache {
    @Volatile var latestJson: String? = null
    @Volatile var latestJobId: String? = null
}
object PdfCardCache {
    @Volatile var latestCardJson: String? = null
}

enum class PdfStage {
    Uploading,
    Processing,
    Done,
    Error
}

data class PdfJobStatus(
    val jobId: String,
    val status: String,   // queued|extracting|llm|done|error
    val progress: Float,  // 0..1
    val error: String? = null
)

data class PdfUiUpdate(
    val stage: PdfStage,
    val progress01: Float,   // 0..1 给UI环形进度条用
    val statusText: String
)

object PdfBackendPipeline {

    /**
     * Emulator 访问本机后端：10.0.2.2
     * 真机访问电脑：改成 http://<电脑局域网IP>:8000
     */
    const val BASE_URL = "http://10.29.142.138:8001"


    private val http = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun runPipeline(
        context: Context,
        pdfUri: Uri,
        onUpdate: (PdfUiUpdate) -> Unit
    ): String = withContext(Dispatchers.IO) {

        onUpdate(PdfUiUpdate(PdfStage.Uploading, 0f, "上传中…"))
        val jobId = uploadCreateJob(context, pdfUri)
        PdfTreeCache.latestJobId = jobId

        // 轮询进度（把后端0..1 映射到 UI 0.15..0.95 ）
        onUpdate(PdfUiUpdate(PdfStage.Processing, 0.15f, "解析与入库中…"))

        val finalStatus = pollJob(jobId) { st ->
            val uiP = 0.15f + 0.80f * st.progress.coerceIn(0f, 1f)
            val text = when (st.status) {
                "queued" -> "排队中…"
                "extracting" -> "提取文本中…"
                "llm" -> "大模型解析中…"
                "done" -> "即将完成…"
                "error" -> "处理失败"
                else -> "处理中…"
            }
            onUpdate(PdfUiUpdate(PdfStage.Processing, uiP, text))
        }

        if (finalStatus.status == "error") {
            val msg = finalStatus.error ?: "unknown error"
            onUpdate(PdfUiUpdate(PdfStage.Error, 0f, "处理失败：$msg"))
            throw RuntimeException(msg)
        }

        onUpdate(PdfUiUpdate(PdfStage.Processing, 0.95f, "拉取结果中…"))
        val json = fetchResultJson(jobId)

        PdfTreeCache.latestJson = json
        onUpdate(PdfUiUpdate(PdfStage.Done, 1f, "处理完成，可进入知识树"))


        return@withContext json
    }

    suspend fun runCardPipeline(
        context: Context,
        pdfUri: Uri,
        onUpdate: (PdfUiUpdate) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onUpdate(PdfUiUpdate(PdfStage.Processing, 0.3f, "生成讲义卡片中…"))

        // 1. 读取 PDF 文件的字节流
        val bytes = context.contentResolver.openInputStream(pdfUri)
            ?.use { it.readBytes() }
            ?: run {
                onUpdate(PdfUiUpdate(PdfStage.Error, 0f, "无法读取本地PDF文件"))
                throw RuntimeException("无法读取PDF：$pdfUri")
            }

        // 2. 构造 MultipartBody，对应 FastAPI 中的 file: UploadFile = File(...)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "upload.pdf", // 这里是后端 file.filename 获取到的默认名字
                bytes.toRequestBody("application/pdf".toMediaType())
            )
            .build()

        // 3. 构建 Request，指向新的 generate_handout 接口
        val req = Request.Builder()
            .url("${BASE_URL}/generate_handout")
            .post(body)
            .build()

        // 4. 发送网络请求并处理结果
        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                onUpdate(PdfUiUpdate(PdfStage.Error, 0f, "讲义生成失败：HTTP ${resp.code}"))
                throw RuntimeException("讲义生成失败：HTTP ${resp.code}\n$respStr")
            }

            // 直接缓存并返回新的 JSON/Markdown 字符串
            PdfCardCache.latestCardJson = respStr
            onUpdate(PdfUiUpdate(PdfStage.Done, 1f, "讲义生成完成"))
            return@withContext respStr
        }
    }

    suspend fun generateDailyBriefing(
        userId: Int,
        targetDate: String
    ): String = withContext(Dispatchers.IO) {
        // 构造 JSON 请求体，与 FastAPI 中的 DailyBriefingRequest 对应
        val jsonReq = JSONObject().apply {
            put("user_id", userId)
            put("target_date", targetDate)
        }

        val body = jsonReq.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url("${BASE_URL}/generate_daily_briefing")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("生成每日简报失败：HTTP ${resp.code}\n$respStr")
            }
            // 返回后端生成的完整 JSON 字符串
            return@withContext respStr
        }
    }

    suspend fun updateDailyBriefingCard(
        userId: Int,
        targetDate: String,
        userReflect: String
    ): String = withContext(Dispatchers.IO) {
        val jsonReq = JSONObject().apply {
            put("user_id", userId)
            put("target_date", targetDate)
            put("user_reflect", userReflect)
        }

        val body = jsonReq.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url("${BASE_URL}/update_daily_briefing")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("更新简报失败：HTTP ${resp.code}\n$respStr")
            }

            // 直接返回新的 JSON 字符串
            return@withContext respStr
        }
    }

    private fun uploadCreateJob(context: Context, pdfUri: Uri): String {
        val bytes = context.contentResolver.openInputStream(pdfUri)
            ?.use { it.readBytes() }
            ?: throw RuntimeException("无法读取PDF：$pdfUri")

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // 后端接口是 file: UploadFile = File(...)
            .addFormDataPart(
                "file",
                "upload.pdf",
                bytes.toRequestBody("application/pdf".toMediaType())
            )
            .build()

        val req = Request.Builder()
            .url("${BASE_URL}/v1/pdf/jobs")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("上传失败：HTTP ${resp.code}\n$respStr")
            }
            val obj = JSONObject(respStr)
            return obj.getString("jobId")
        }
    }

    private suspend fun pollJob(
        jobId: String,
        onUpdate: (PdfJobStatus) -> Unit
    ): PdfJobStatus {
        while (true) {
            val req = Request.Builder()
                .url("${BASE_URL}/v1/pdf/jobs/$jobId")
                .get()
                .build()

            val st = http.newCall(req).execute().use { resp ->
                val respStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw RuntimeException("轮询失败：HTTP ${resp.code}\n$respStr")
                }
                val obj = JSONObject(respStr)
                PdfJobStatus(
                    jobId = obj.getString("jobId"),
                    status = obj.getString("status"),
                    progress = obj.getDouble("progress").toFloat(),
                    error = obj.optString("error", null)
                )
            }

            onUpdate(st)

            if (st.status == "done" || st.status == "error") return st
            delay(450)
        }
    }

    private fun fetchResultJson(jobId: String): String {
        val req = Request.Builder()
            .url("${BASE_URL}/v1/pdf/jobs/$jobId/result")
            .get()
            .build()

        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("取结果失败：HTTP ${resp.code}\n$respStr")
            }
            return respStr
        }
    }
}



package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.help_stu_agent.ui.treeStructure.KnowledgeJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    const val RAG_URL = "http://10.29.238.57:8002"


    private val http = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

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
        val jsonString = fetchResultJson(jobId)
        
        try {
            val root = jsonConfig.decodeFromString<KnowledgeJson>(jsonString)
            val pdfName = getFileName(context, pdfUri) ?: "document.pdf"
            syncTreeToRag(root, pdfName)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        PdfTreeCache.latestJson = jsonString
        onUpdate(PdfUiUpdate(PdfStage.Done, 1f, "处理完成，可进入知识树"))


        return@withContext jsonString
    }

    suspend fun runCardPipeline(
        context: Context,
        pdfUri: Uri,
        onUpdate: (PdfUiUpdate) -> Unit
    ): String = withContext(Dispatchers.IO) {
        onUpdate(PdfUiUpdate(PdfStage.Processing, 0.3f, "生成讲义文档中…"))

        val bytes = context.contentResolver.openInputStream(pdfUri)
            ?.use { it.readBytes() }
            ?: run {
                onUpdate(PdfUiUpdate(PdfStage.Error, 0f, "无法读取本地PDF文件"))
                throw RuntimeException("无法读取PDF：$pdfUri")
            }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "upload.pdf",
                bytes.toRequestBody("application/pdf".toMediaType())
            )
            .build()

        val req = Request.Builder()
            .url("${BASE_URL}/generate_handout")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val respStr = resp.body?.string().orEmpty()
                onUpdate(PdfUiUpdate(PdfStage.Error, 0f, "讲义生成失败：HTTP ${resp.code}"))
                throw RuntimeException("讲义生成失败：HTTP ${resp.code}\n$respStr")
            }

            // 因为后端返回 204 No Content，不需要读取 body
            onUpdate(PdfUiUpdate(PdfStage.Done, 1f, "讲义生成完成"))

            // 返回空字符串，表示执行成功但没有需要缓存的 json
            return@withContext ""
        }
    }

    suspend fun generateDailyBriefing(
        userId: Int
    ): String = withContext(Dispatchers.IO) {
        // 使用 FormBody 构建表单数据，仅传递 FastAPI 中 Form(...) 要求的 user_id
        val body = okhttp3.FormBody.Builder()
            .add("user_id", userId.toString())
            .build()

        val req = Request.Builder()
            .url("${BASE_URL}/upload_pdf_generate_daily_brief")
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

            return@withContext respStr
        }
    }


    suspend fun extractEliteIdeas(): Unit = withContext(Dispatchers.IO) {
        // 不需要传参数，直接用空表单触发默认逻辑
        val body = okhttp3.FormBody.Builder().build()
        val req = Request.Builder()
            .url("${BASE_URL}/extract_elite_ideas")
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                val respStr = resp.body?.string().orEmpty()
                throw RuntimeException("Elite Ideas 生成失败：HTTP ${resp.code}\n$respStr")
            }
        }
    }

    suspend fun getEliteIdeas(): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${BASE_URL}/get_elite_ideas")
            .get()
            .build()

        http.newCall(req).execute().use { resp ->
            val respStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw RuntimeException("拉取 Elite Ideas 失败：HTTP ${resp.code}\n$respStr")
            }
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

    // 1. 定义发送给服务 B 的数据模型
    @Serializable
    data class SyncToRagReq(
        val documents: List<String>,
        val metadatas: List<Map<String, String>>,
        val collection: String = "concepts"
    )

    // 2. 提取并推送的逻辑
    fun syncTreeToRag(root: KnowledgeJson, pdfName: String) {
        val allTexts = mutableListOf<String>()
        val allMetas = mutableListOf<Map<String, String>>()

        // 递归提取整棵树的 content
        fun traverse(node: KnowledgeJson) {
            val nodeText = "${node.title}\n${node.content}".trim()
            if (nodeText.isNotBlank()) {
                allTexts.add(nodeText)
                allMetas.add(mapOf(
                    "node_id" to node.id,
                    "title" to node.title,
                    "source" to pdfName
                ))
            }
            node.children.forEach { traverse(it) }
        }
        traverse(root)

        if (allTexts.isEmpty()) return

        // 异步推送到服务 B
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestBody = jsonConfig.encodeToString(SyncToRagReq(allTexts, allMetas))
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val request = Request.Builder()
                    .url("${RAG_URL}/documents/add") // 服务 B 的地址
                    .post(requestBody)
                    .build()

                http.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        println("成功将知识点同步到 RAG 后端: ${response.code}")
                    } else {
                        println("同步 RAG 失败: ${response.code} ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) cursor.getString(nameIndex) else null
            } ?: uri.path?.substringAfterLast('/')
        } catch (e: Exception) {
            uri.path?.substringAfterLast('/')
        }
    }
}

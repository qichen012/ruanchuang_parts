package com.example.help_stu_agent.ui.treeStructure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val timestamp: String? = null,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class RagChatRequest(
    val sessionId: String,
    val query: String,
    val nLearningResults: Int = 3,
    val nQaResults: Int = 5,
    val saveAnalysis: Boolean = true
)

@Serializable
data class RagChatResponse(
    val sessionId: String? = null,
    val query: String,
    val answer: String,
    val learningAnalysis: String? = null,
    val analysisId: String? = null,
    val learningContext: List<String> = emptyList(),
    val qaContext: List<String> = emptyList()
)

class RagBackendClient(
    private val baseUrl: String = "http://10.29.238.57:8002"
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * 面向节点聊天的 RAG 问答
     *
     * 约定：
     * - 一个 nodeId 对应一个独立会话
     * - session_id 由前端自动拼成 node_chat_<nodeId>
     */
    suspend fun chatWithRag(
        query: String,
        nodeId: String,
        contextHint: String?
    ): String = withContext(Dispatchers.IO) {
        try {
            val sessionId = "node_chat_$nodeId"

            val reqBody = RagChatRequest(
                sessionId = sessionId,
                query = query,
                nLearningResults = 3,
                nQaResults = 5,
                saveAnalysis = true
            )

            val bodyStr = json.encodeToString(reqBody)

            val request = Request.Builder()
                .url("$baseUrl/api/v1/qa/ask")
                .post(bodyStr.toRequestBody(mediaType))
                .build()

            http.newCall(request).execute().use { resp ->
                val respBody = resp.body?.string().orEmpty()

                if (!resp.isSuccessful) {
                    return@withContext buildString {
                        append("请求后端 RAG 失败：HTTP ${resp.code}")
                        if (respBody.isNotBlank()) {
                            append("\n")
                            append(respBody)
                        }
                    }
                }

                val parsed = json.decodeFromString<ApiResponse<RagChatResponse>>(respBody)

                if (parsed.code != 200) {
                    return@withContext "请求失败：${parsed.message}"
                }

                val data = parsed.data
                    ?: return@withContext "后端返回成功，但 data 为空。"

                return@withContext buildDisplayText(data)
            }
        } catch (e: SocketTimeoutException) {
            "连接超时：后端响应太慢，请稍后再试。"
        } catch (e: Exception) {
            "网络异常：${e.localizedMessage ?: e.toString()}"
        }
    }

    private fun buildDisplayText(resp: RagChatResponse): String {
        val answer = resp.answer.trim().ifBlank { "后端未返回有效回答。" }
        val learning = resp.learningAnalysis?.trim().orEmpty()

        return if (learning.isNotBlank()) {
            "$answer\n\n---\n💡 学习分析\n$learning"
        } else {
            answer
        }
    }
}
package com.example.help_stu_agent.ui.treeStructure

import com.example.help_stu_agent.BuildConfig.DEEPSEEK_API_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

@OptIn(InternalSerializationApi::class)
@Serializable
data class DSMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class DSChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<DSMessage>,
    val temperature: Double = 0.3
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class DSChatResponse(
    val choices: List<DSChoice> = emptyList()
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class DSChoice(
    val message: DSMessage? = null
)

class DeepSeekClient(
    private val apiKey: String = DEEPSEEK_API_KEY ,
    private val baseUrl: String = "https://api.deepseek.com"
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // 连接超时：60秒
        .readTimeout(60, TimeUnit.SECONDS)    // 读取超时：60秒
        .writeTimeout(60, TimeUnit.SECONDS)   // 写入超时：60秒
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(messages: List<DSMessage>): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "API Key 为空，请检查配置。"
        }

        // 2. 使用 try-catch 包装整个网络请求过程
        try {
            val reqBody = DSChatRequest(messages = messages)
            val bodyStr = json.encodeToString(DSChatRequest.serializer(), reqBody)

            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(bodyStr.toRequestBody(mediaType))
                .build()

            val resp = http.newCall(request).execute()
            val respBody = resp.body?.string().orEmpty()

            if (!resp.isSuccessful) {
                return@withContext "请求失败：HTTP ${resp.code}\n$respBody"
            }

            val parsed = runCatching {
                json.decodeFromString(DSChatResponse.serializer(), respBody)
            }.getOrNull()

            parsed?.choices?.firstOrNull()?.message?.content ?: "返回内容解析为空"

        } catch (e: SocketTimeoutException) {
            "连接超时：服务器响应太慢，请稍后再试。"
        } catch (e: Exception) {
            "网络异常：${e.localizedMessage}"
        }
    }
}

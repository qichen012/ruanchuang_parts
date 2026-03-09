package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.help_stu_agent.data.net.PdfRetrofitClient
import com.example.help_stu_agent.data.net.SourceDocumentCreateRequest
import com.example.help_stu_agent.data.net.SourceDocumentUpdateRequest
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import androidx.core.net.toUri


class PdfProcessWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val displayName = inputData.getString(KEY_NAME) ?: "PDF"
        val uri = uriStr.toUri()

        runCatching {
            applicationContext.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val treeRepo = KnowledgeTreeRepository(applicationContext)
        val cardRepo = KnowledgeCardRepository(applicationContext)

        var progress = 0f

        fun push(stage: String, p: Float, text: String) {
            progress = max(progress, p.coerceIn(0f, 1f))
            setProgressAsync(
                workDataOf(
                    KEY_STAGE to stage,
                    KEY_PROGRESS to progress,
                    KEY_STATUS to text,
                    KEY_NAME to displayName,
                    KEY_URI to uriStr
                )
            )
        }

        // 用于保存后端数据库返回的源文档 ID，以便后续更新状态
        var backendDocId: Int? = null

        return try {
            push("Init", 0.05f, "正在同步文档信息至云端...")
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val response = PdfRetrofitClient.api.createSourceDocument(
                    SourceDocumentCreateRequest(
                        user_id = 1, // TODO: 替换为当前登录的真实用户ID
                        file_name = displayName,
                        file_path = uriStr,
                        upload_date = currentDate,
                        processed_status = "Pending"
                    )
                )
                backendDocId = response.id
            } catch (e: Exception) {
                e.printStackTrace()
            }


            val treeJson = PdfBackendPipeline.runPipeline(
                context = applicationContext,
                pdfUri = uri,
                onUpdate = { up -> push(up.stage.name, up.progress01, "[树] ${up.statusText}") }
            )

            val cardJson = PdfBackendPipeline.runCardPipeline(
                context = applicationContext,
                pdfUri = uri,
                onUpdate = { up -> push(up.stage.name, up.progress01, "[卡] ${up.statusText}") }
            )

            var treeId: String? = null
            var cardId: String? = null

            if (treeJson.isNotBlank()) {
                treeId = treeRepo.saveNewTree(
                    pdfDisplayName = displayName,
                    pdfUri = uriStr,
                    title = null,
                    jsonString = treeJson
                )
            }
            if (cardJson.isNotBlank()) {
                cardId = cardRepo.saveNewCard(
                    pdfDisplayName = displayName,
                    pdfUri = uriStr,
                    rawJson = cardJson
                )
            }


            backendDocId?.let { id ->
                runCatching {
                    PdfRetrofitClient.api.updateSourceDocumentStatus(
                        id,
                        SourceDocumentUpdateRequest(processed_status = "Done")
                    )
                }
            }

            push("Done", 1f, "完成：已保存到本地与云端")

            Result.success(
                workDataOf(
                    KEY_TREE_ID to (treeId ?: ""),
                    KEY_CARD_ID to (cardId ?: ""),
                    OUT_NAME to displayName,
                    OUT_URI to uriStr
                )
            )
        } catch (e: Exception) {
            backendDocId?.let { id ->
                runCatching {
                    PdfRetrofitClient.api.updateSourceDocumentStatus(
                        id,
                        SourceDocumentUpdateRequest(processed_status = "Failed")
                    )
                }
            }

            push("Error", 0f, "失败：${e.message ?: "未知错误"}")
            Result.retry()
        }
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_NAME = "name"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_TREE_ID = "treeId"
        const val KEY_CARD_ID = "cardId"

        const val OUT_NAME = "out_name"
        const val OUT_URI = "out_uri"
    }
}
package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.help_stu_agent.data.repo.KnowledgeCardRepository
import com.example.help_stu_agent.data.repo.KnowledgeTreeRepository
import kotlin.math.max

class PdfProcessWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        val displayName = inputData.getString(KEY_NAME) ?: "PDF"
        val uri = Uri.parse(uriStr)

        // 确保持久权限（如果你在选择时已 takePersistableUriPermission，这里通常不需要）
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
                    KEY_NAME to displayName
                )
            )
        }

        return try {
            // 1) 知识树
            val treeJson = PdfBackendPipeline.runPipeline(
                context = applicationContext,
                pdfUri = uri,
                onUpdate = { up ->
                    push(up.stage.name, up.progress01, "[树] ${up.statusText}")
                }
            )

            // 2) 知识卡
            val cardJson = PdfBackendPipeline.runCardPipeline(
                context = applicationContext,
                pdfUri = uri,
                onUpdate = { up ->
                    push(up.stage.name, up.progress01, "[卡] ${up.statusText}")
                }
            )

            // 3) 入库（你原先在页面里做的事）:contentReference[oaicite:6]{index=6}
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

            push("Done", 1f, "完成：已保存到本地")

            Result.success(
                workDataOf(
                    KEY_TREE_ID to (treeId ?: ""),
                    KEY_CARD_ID to (cardId ?: ""),
                    KEY_NAME to displayName
                )
            )
        } catch (e: Exception) {
            push("Error", 0f, "失败：${e.message ?: "unknown error"}")
            Result.retry() // 需要更保守可改 failure()
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
    }
}

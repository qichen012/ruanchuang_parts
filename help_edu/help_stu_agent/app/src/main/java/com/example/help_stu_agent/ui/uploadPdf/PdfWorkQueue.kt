package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID

object PdfWorkQueue {

    const val TAG = "pdf_queue"

    /**
     * items: List of (uriStr, displayName)
     * 返回每个 WorkRequest 的 id，便于 UI 定位/取消
     */
    fun enqueueBatch(
        context: Context,
        items: List<Pair<String, String>>
    ): List<UUID> {
        if (items.isEmpty()) return emptyList()

        val wm = WorkManager.getInstance(context)

        val requests = items.map { (uriStr, name) ->
            OneTimeWorkRequestBuilder<PdfProcessWorker>()
                .setInputData(
                    workDataOf(
                        PdfProcessWorker.KEY_URI to uriStr,
                        PdfProcessWorker.KEY_NAME to name
                    )
                )
                .addTag(TAG)
                .build()
        }

        wm.beginUniqueWork(
            "pdf_pipeline_queue",
            ExistingWorkPolicy.APPEND,
            requests.first()
        ).also { cont ->
            requests.drop(1).forEach { cont.then(it) }
            cont.enqueue()
        }

        return requests.map { it.id }
    }
}

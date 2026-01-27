package com.example.help_stu_agent.ui.uploadPdf

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID

object PdfWorkQueue {
    const val UNIQUE_BATCH = "pdf_batch_queue"
    const val TAG = "pdf_process"

    /**
     * 追加到同一个串行队列（后台静默处理），避免并发把后端/本地处理跑爆。
     * @return 本次入队的 WorkRequest id 列表（UI 用来 lastOrNull/选中/取消）
     */
    fun enqueueBatch(context: Context, items: List<Pair<String, String>>): List<UUID> {
        if (items.isEmpty()) return emptyList()

        val wm = WorkManager.getInstance(context)

        val reqs: List<OneTimeWorkRequest> = items.map { (uriStr, name) ->
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

        // 串行追加：队列存在就 APPEND；不存在就创建
        var cont: WorkContinuation = wm.beginUniqueWork(
            UNIQUE_BATCH,
            ExistingWorkPolicy.APPEND,
            reqs.first()
        )
        reqs.drop(1).forEach { r ->
            cont = cont.then(r)
        }
        cont.enqueue()

        return reqs.map { it.id }
    }
}

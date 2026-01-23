package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import com.example.help_stu_agent.ui.home.HomePage

@Composable
fun MainHomePage(
    onGoUpload: () -> Unit,
    onGoKnowledgeTree: () -> Unit
) {
    val canResumeTree = !PdfTreeCache.latestJson.isNullOrBlank()

    HomePage(
        onMenuClick = { /* TODO: 你可以后续接 Drawer/Settings */ },
        onRightActionClick = { /* TODO: 你可以后续接 History/Notes */ },
        onGoUploadPdf = onGoUpload,
        onGoKnowledgeTree = onGoKnowledgeTree,
        canResumeTree = canResumeTree
    )
}

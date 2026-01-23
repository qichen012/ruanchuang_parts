package com.example.help_stu_agent.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * HomePage 只做页面组合，不写业务逻辑
 */
@Composable
fun HomePage(
    onMenuClick: () -> Unit,
    onRightActionClick: () -> Unit,
    onGoUploadPdf: () -> Unit,
    onGoKnowledgeTree: () -> Unit,
    canResumeTree: Boolean
) {
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = {
            HomeTopBar(
                onMenuClick = onMenuClick,
                onRightActionClick = onRightActionClick
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // 背景装饰层（不影响交互）
            HomeDecorations()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp)
            ) {
                GreetingSection()

                Spacer(modifier = Modifier.height(22.dp))

                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    TodayActionCard(
                        title = "Upload a PDF",
                        description = "Turn your document into a structured knowledge tree with formulas and cross-links.",
                        primaryButtonText = "Start",
                        onPrimaryClick = onGoUploadPdf
                    )

                    if (canResumeTree) {
                        Spacer(modifier = Modifier.height(14.dp))

                        TodayActionCard(
                            title = "Continue where you left off",
                            description = "Open your latest knowledge tree and keep refining the nodes.",
                            primaryButtonText = "View",
                            onPrimaryClick = onGoKnowledgeTree
                        )
                    }
                }
            }
        }
    }
}

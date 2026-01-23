package com.example.help_stu_agent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.help_stu_agent.ui.home.HomeDrawerAction
import com.example.help_stu_agent.ui.home.HomeDrawerSheet
import com.example.help_stu_agent.ui.home.HomePage

@Composable
fun MainHomePage(
    onGoUpload: () -> Unit,
    onGoKnowledgeTree: () -> Unit
) {
    val drawerOpen = remember { mutableStateOf(false) }

    // 你的主页
    HomePage(
        onMenuClick = { drawerOpen.value = true },
        onRightActionClick = { /* 你可以后续接 History/Notes */ },
        onGoUploadPdf = onGoUpload
    )


    HomeDrawerSheet(
        visible = drawerOpen.value,
        onDismiss = { drawerOpen.value = false },
        onAction = { action ->
            when (action) {
                HomeDrawerAction.DailyReport -> { /* TODO */ }
                HomeDrawerAction.KnowledgeStructure -> onGoKnowledgeTree()
                HomeDrawerAction.SparkyLink -> { /* TODO */ }
                HomeDrawerAction.EliteIdeas -> { /* TODO */ }
                HomeDrawerAction.MyAccount -> { /* TODO */ }
            }
        }
    )
}
